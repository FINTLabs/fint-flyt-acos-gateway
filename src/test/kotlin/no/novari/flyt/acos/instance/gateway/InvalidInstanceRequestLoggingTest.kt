package no.novari.flyt.acos.instance.gateway

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver

class InvalidInstanceRequestLoggingTest {
    private val logger = LoggerFactory.getLogger(InvalidInstanceRequestLogging::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        appender.start()
        logger.addAppender(appender)
        val resolvers = mutableListOf<HandlerExceptionResolver>(DefaultHandlerExceptionResolver())
        InvalidInstanceRequestLogging().extendHandlerExceptionResolvers(resolvers)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(AcosInstanceController(mock(), mock(), mock()))
                .setHandlerExceptionResolvers(resolvers)
                .build()
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(appender)
        appender.stop()
    }

    @Test
    fun `logs missing required field without submitted values`() {
        val message =
            postInvalidInstance(
                """{"metadata":{"formId":"private-form"},"elements":[],"formPdfBase64":"private-document"}""",
            )

        assertThat(message).contains("field=metadata.instanceId", "targetType=AcosInstanceMetadata")
        assertThat(message).doesNotContain("private-form", "private-document")
    }

    @Test
    fun `logs wrong JSON type without rejected value`() {
        val message = postInvalidInstance("""{"metadata":"private-value"}""")

        assertThat(message).contains("field=metadata", "targetType=AcosInstanceMetadata")
        assertThat(message).doesNotContain("private-value")
    }

    @Test
    fun `logs malformed JSON location without source content`() {
        val message = postInvalidInstance("""{"metadata": private-value}""")

        assertThat(message).contains("error=JsonParseException", "line=1", "column=")
        assertThat(message).doesNotContain("private-value")
    }

    @Test
    fun `logs validation field and constraint without rejected value`() {
        val message =
            postInvalidInstance(
                """
                {
                  "metadata":{"formId":"private-form","instanceId":"private-instance"},
                  "elements":[],
                  "formPdfBase64":"private-document!"
                }
                """.trimIndent(),
            )

        assertThat(message).contains("elements: NotEmpty", "formPdfBase64: ValidBase64")
        assertThat(message).doesNotContain("private-form", "private-instance", "private-document")
    }

    @Test
    fun `logs invalid MIME type for the failing attachment only`() {
        val message = postInvalidDocumentType("pdf")

        assertThat(message)
            .contains("error=ValueInstantiationException", "field=documents.[3].type", "mimeType=\"pdf\"")
        assertThat(message).doesNotContain("private-name", "private-document", "private-instance", "private-form")
    }

    @Test
    fun `escapes and limits invalid MIME type in the log`() {
        val mimeType = "bad\r\n\t\"" + "x".repeat(300)
        val message = postInvalidDocumentType(mimeType)

        assertThat(message).contains("mimeType=\"bad\\r\\n\\t\\\"")
        assertThat(message).doesNotContain("\r", "\n", "\t", "x".repeat(256))
        val loggedMimeType = message.substringAfter("mimeType=")
        assertThat(jacksonObjectMapper().readValue(loggedMimeType, String::class.java)).isEqualTo(mimeType.take(256))
    }

    private fun postInvalidDocumentType(mimeType: String): String {
        val encodedMimeType = jacksonObjectMapper().writeValueAsString(mimeType)
        val validDocument = """{"name":"private-name","type":"application/pdf","base64":"private-document"}"""
        return postInvalidInstance(
            """
            {
              "metadata":{"formId":"private-form","instanceId":"private-instance"},
              "elements":[],
              "formPdfBase64":"private-document",
              "documents":[$validDocument,$validDocument,$validDocument,
                {"name":"private-name","type":$encodedMimeType,"base64":"private-document"}]
            }
            """.trimIndent(),
        )
    }

    private fun postInvalidInstance(body: String): String {
        mockMvc
            .perform(post("/api/acos/instances").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest)

        assertThat(appender.list).hasSize(1)
        val event = appender.list.single()
        assertThat(event.throwableProxy).isNull()
        assertThat(event.formattedMessage).contains("method=POST", "path=/api/acos/instances")
        return event.formattedMessage
    }
}

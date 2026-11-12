package no.novari.flyt.acos.instance.gateway

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
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

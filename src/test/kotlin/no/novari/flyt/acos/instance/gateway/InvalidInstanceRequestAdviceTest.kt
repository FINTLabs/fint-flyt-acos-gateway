package no.novari.flyt.acos.instance.gateway

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.novari.flyt.acos.discovery.gateway.AcosIntegrationMetadataController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController

class InvalidInstanceRequestAdviceTest {
    private val logger = LoggerFactory.getLogger(InvalidInstanceRequestAdvice::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private lateinit var mockMvc: MockMvc
    private lateinit var response: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        appender.start()
        logger.addAppender(appender)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(
                    AcosInstanceController(mock(), mock(), mock()),
                    AcosIntegrationMetadataController(mock(), mock(), mock()),
                ).setControllerAdvice(InvalidInstanceRequestAdvice(jacksonObjectMapper()))
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
        assertThat(responseMessage()).isEqualTo("Required field metadata.instanceId is missing or null.")
        assertThat(response.contentAsString).doesNotContain("private-form", "private-document")
    }

    @Test
    fun `logs wrong JSON type without rejected value`() {
        val message = postInvalidInstance("""{"metadata":"private-value"}""")

        assertThat(message).contains("field=metadata", "targetType=AcosInstanceMetadata")
        assertThat(message).doesNotContain("private-value")
        assertThat(responseMessage()).isEqualTo("Incorrect data type in field metadata. Expected an object.")
        assertThat(response.contentAsString).doesNotContain("private-value")
    }

    @Test
    fun `logs malformed JSON location without source content`() {
        val message = postInvalidInstance("""{"metadata": private-value}""")

        assertThat(message).contains("error=JsonParseException", "line=1", "column=")
        assertThat(message).doesNotContain("private-value")
        assertThat(responseMessage()).contains("Invalid JSON at line 1, column", "Check the JSON syntax.")
        assertThat(response.contentAsString).doesNotContain("private-value")
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
        assertThat(responseMessage())
            .contains("elements: must not be empty", "formPdfBase64: Invalid Base64-encoded string")
        assertThat(response.contentAsString).doesNotContain("private-form", "private-instance", "private-document")
    }

    @Test
    fun `explains explicit null in a required field`() {
        postInvalidInstance("""{"metadata":null,"elements":[],"formPdfBase64":"YQ=="}""")

        assertThat(responseMessage()).isEqualTo("Required field metadata is missing or null.")
    }

    @Test
    fun `explains wrong type for a list`() {
        postInvalidInstance(
            """{"metadata":{"formId":"form","instanceId":"instance"},"elements":{},"formPdfBase64":"YQ=="}""",
        )

        assertThat(responseMessage()).isEqualTo("Incorrect data type in field elements. Expected a list.")
    }

    @Test
    fun `explains wrong type for a text field`() {
        postInvalidInstance("""{"metadata":{"formId":{}}}""")

        assertThat(responseMessage()).isEqualTo("Incorrect data type in field metadata.formId. Expected text.")
    }

    @Test
    fun `explains all nested validation errors`() {
        postInvalidInstance(
            """
            {
              "metadata":{"formId":" ","instanceId":"private-instance"},
              "elements":[{"id":""}],
              "formPdfBase64":"YQ==",
              "documents":[{"name":"","type":"application/pdf","base64":"private-document!"}]
            }
            """.trimIndent(),
        )

        assertThat(responseMessage()).contains(
            "metadata.formId: must not be blank",
            "elements[0].id: must not be blank",
            "documents[0].name: must not be blank",
            "documents[0].base64: Invalid Base64-encoded string",
        )
        assertThat(response.contentAsString).doesNotContain("private-instance", "private-document")
    }

    @Test
    fun `explains an empty request body`() {
        postInvalidInstance("")

        assertThat(responseMessage()).isEqualTo("The request body is missing or could not be read as JSON.")
    }

    @Test
    fun `logs invalid MIME type and explains the failing attachment in the response`() {
        val message = postInvalidDocumentType("m4a")

        assertThat(message)
            .contains("error=ValueInstantiationException", "field=documents.[3].type", "mimeType=\"m4a\"")
        assertThat(message).doesNotContain("private-name", "private-document", "private-instance", "private-form")
        assertThat(response.contentType).isEqualTo("application/json;charset=UTF-8")
        val body = jacksonObjectMapper().readTree(response.contentAsByteArray)
        assertThat(body["timestamp"].asText()).isNotBlank()
        assertThat(body["status"].asInt()).isEqualTo(400)
        assertThat(body["error"].asText()).isEqualTo("Bad Request")
        assertThat(body["path"].asText()).isEqualTo("/afk-no/api/acos/instances")
        assertThat(body["message"].asText()).isEqualTo(
            "Invalid MIME type \"m4a\" in field documents[3].type. " +
                "Expected format type/subtype, for example audio/mp4 or application/pdf.",
        )
        assertThat(response.contentAsString)
            .doesNotContain("private-name", "private-document", "private-instance", "private-form")
    }

    @Test
    fun `escapes and limits invalid MIME type in the log`() {
        val mimeType = "bad\r\n\t\"" + "x".repeat(300)
        val message = postInvalidDocumentType(mimeType)

        assertThat(message).contains("mimeType=\"bad\\r\\n\\t\\\"")
        assertThat(message).doesNotContain("\r", "\n", "\t", "x".repeat(256))
        val loggedMimeType = message.substringAfter("mimeType=")
        assertThat(jacksonObjectMapper().readValue(loggedMimeType, String::class.java)).isEqualTo(mimeType.take(256))
        val responseMessage = jacksonObjectMapper().readTree(response.contentAsByteArray)["message"].asText()
        assertThat(responseMessage).contains(loggedMimeType).doesNotContain("\r", "\n", "\t", "x".repeat(256))
    }

    @Test
    fun `expects text when the MIME type is an object`() {
        postInvalidInstance(
            """
            {
              "metadata":{"formId":"form","instanceId":"instance"},
              "elements":[],
              "formPdfBase64":"YQ==",
              "documents":[{"name":"attachment","type":{},"base64":"YQ=="}]
            }
            """.trimIndent(),
        )

        assertThat(responseMessage()).isEqualTo("Incorrect data type in field documents[0].type. Expected text.")
    }

    @Test
    fun `returns UTF-8 so non-ASCII field names are readable`() {
        postInvalidDocumentType("audio/æøå")

        assertThat(response.characterEncoding).isEqualTo("UTF-8")
        assertThat(response.contentAsString).contains("audio/æøå")
        assertThat(response.contentAsString).isEqualTo(response.contentAsByteArray.toString(Charsets.UTF_8))
    }

    @Test
    fun `leaves metadata error handling to existing resolvers`() {
        mockMvc
            .perform(post("/api/acos/metadata").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)

        assertThat(appender.list).isEmpty()
    }

    @Test
    fun `preserves controller-local exception handlers`() {
        MockMvcBuilders
            .standaloneSetup(LocallyHandledInstanceController())
            .setControllerAdvice(InvalidInstanceRequestAdvice(jacksonObjectMapper()))
            .build()
            .perform(post("/api/acos/instances").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("local handler"))

        assertThat(appender.list).isEmpty()
    }

    @Test
    fun `explains scalar and array target types`() {
        val mapper = jacksonObjectMapper()
        val advice = InvalidInstanceRequestAdvice(mapper)
        val targets =
            listOf(
                Int::class.javaObjectType to "a number",
                Int::class.javaPrimitiveType!! to "a number",
                Boolean::class.javaObjectType to "a boolean",
                Boolean::class.javaPrimitiveType!! to "a boolean",
                HttpStatus::class.java to "text",
                IntArray::class.java to "a list",
            )

        targets.forEach { (type, expected) ->
            val cause = assertThrows<MismatchedInputException> { mapper.readValue("{}", type) }
            val exception =
                HttpMessageNotReadableException("Unreadable JSON", cause, MockHttpInputMessage(byteArrayOf()))
            val result =
                advice.handleUnreadableRequest(
                    exception,
                    MockHttpServletRequest("POST", "/api/acos/instances"),
                )

            assertThat(result.body?.get("message"))
                .isEqualTo("Incorrect data type in field (JSON root). Expected $expected.")
        }
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

    private fun responseMessage(): String = jacksonObjectMapper().readTree(response.contentAsString)["message"].asText()

    private fun postInvalidInstance(body: String): String {
        response =
            mockMvc
                .perform(
                    post("/afk-no/api/acos/instances")
                        .contextPath("/afk-no")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body),
                ).andExpect(status().isBadRequest)
                .andReturn()
                .response

        assertThat(appender.list).hasSize(1)
        val event = appender.list.single()
        assertThat(event.throwableProxy).isNull()
        assertThat(event.formattedMessage).contains("method=POST", "path=/afk-no/api/acos/instances")
        return event.formattedMessage
    }

    @RestController
    class LocallyHandledInstanceController : AcosInstanceController(mock(), mock(), mock()) {
        @ExceptionHandler(HttpMessageNotReadableException::class)
        fun handleUnreadableRequest(): ResponseEntity<Map<String, String>> =
            ResponseEntity.badRequest().body(mapOf("message" to "local handler"))
    }
}

package no.novari.flyt.acos.instance.gateway

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidNullException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.OffsetDateTime

private val log = KotlinLogging.logger {}
private val documentTypePath = Regex("""documents\.\[\d+]\.type""")

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = [AcosInstanceController::class])
class InvalidInstanceRequestAdvice(
    private val objectMapper: ObjectMapper,
) {
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<Map<String, Any>> {
        val causes = generateSequence<Throwable>(exception) { it.cause }.toList()
        val jsonException = causes.filterIsInstance<JsonProcessingException>().firstOrNull()
        val fieldPath =
            (jsonException as? JsonMappingException)?.path?.joinToString(".") {
                it.fieldName ?: "[${it.index}]"
            }
        val errorType = jsonException?.javaClass?.simpleName ?: exception.javaClass.simpleName
        val targetType = (jsonException as? MismatchedInputException)?.targetType?.simpleName
        val mimeType =
            if (fieldPath?.matches(documentTypePath) == true) {
                causes
                    .filterIsInstance<InvalidMediaTypeException>()
                    .firstOrNull()
                    ?.mediaType
                    ?.let { objectMapper.writeValueAsString(it.take(256)) }
            } else {
                null
            }
        // Only the invalid MIME type is logged; other values may contain sensitive data.
        log.warn {
            "Invalid ACOS request: method=${request.method}, path=${request.requestURI}, " +
                "error=$errorType, field=$fieldPath, targetType=$targetType, " +
                "line=${jsonException?.location?.lineNr}, column=${jsonException?.location?.columnNr}" +
                mimeType?.let { ", mimeType=$it" }.orEmpty()
        }
        val field = fieldPath?.replace(".[", "[")?.takeIf { it.isNotBlank() } ?: "(JSON root)"
        val message =
            when {
                mimeType != null -> {
                    "Invalid MIME type $mimeType in field $field. " +
                        "Expected format type/subtype, for example audio/mp4 or application/pdf."
                }

                jsonException is InvalidNullException -> {
                    "Required field $field is missing or null."
                }

                jsonException is JsonParseException -> {
                    "Invalid JSON at line ${jsonException.location?.lineNr}, " +
                        "column ${jsonException.location?.columnNr}. Check the JSON syntax."
                }

                jsonException is MismatchedInputException -> {
                    val type = jsonException.targetType
                    val expected =
                        when {
                            type == String::class.java || type == MediaType::class.java || type?.isEnum == true -> {
                                "text"
                            }

                            type == Boolean::class.javaObjectType || type == Boolean::class.javaPrimitiveType -> {
                                "a boolean"
                            }

                            type != null && Number::class.java.isAssignableFrom(type) -> {
                                "a number"
                            }

                            type?.isPrimitive == true && type != Char::class.javaPrimitiveType -> {
                                "a number"
                            }

                            type == Char::class.javaObjectType || type == Char::class.javaPrimitiveType -> {
                                "text"
                            }

                            type != null && (type.isArray || Collection::class.java.isAssignableFrom(type)) -> {
                                "a list"
                            }

                            type != null -> {
                                "an object"
                            }

                            else -> {
                                "a valid JSON value"
                            }
                        }
                    "Incorrect data type in field $field. Expected $expected."
                }

                jsonException is JsonMappingException -> {
                    "Invalid value in field $field."
                }

                else -> {
                    "The request body is missing or could not be read as JSON."
                }
            }
        return badRequest(message, request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationFailure(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<Map<String, Any>> {
        val errors = exception.bindingResult.fieldErrors.map { "${it.field}: ${it.code}" }
        log.warn {
            "Invalid ACOS request: method=${request.method}, path=${request.requestURI}, errors=$errors"
        }
        val message =
            exception.bindingResult.allErrors.joinToString("; ") { error ->
                val field = (error as? FieldError)?.field ?: error.objectName
                "$field: ${error.defaultMessage ?: "invalid value"}"
            }
        return badRequest(message, request)
    }

    private fun badRequest(
        message: String,
        request: HttpServletRequest,
    ): ResponseEntity<Map<String, Any>> =
        ResponseEntity
            .badRequest()
            .contentType(MediaType("application", "json", Charsets.UTF_8))
            .body(
                mapOf(
                    "timestamp" to OffsetDateTime.now().toString(),
                    "status" to HttpStatus.BAD_REQUEST.value(),
                    "error" to HttpStatus.BAD_REQUEST.reasonPhrase,
                    "message" to message,
                    "path" to request.requestURI,
                ),
            )
}

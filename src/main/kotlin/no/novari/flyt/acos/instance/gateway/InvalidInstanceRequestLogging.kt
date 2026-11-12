package no.novari.flyt.acos.instance.gateway

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.io.JsonStringEncoder
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidNullException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.OffsetDateTime

private val log = KotlinLogging.logger {}

@Configuration
class InvalidInstanceRequestLogging(
    private val objectMapper: ObjectMapper,
) : WebMvcConfigurer {
    override fun extendHandlerExceptionResolvers(resolvers: MutableList<HandlerExceptionResolver>) {
        resolvers.add(
            0,
            HandlerExceptionResolver { request, response, handler, exception ->
                if ((handler as? HandlerMethod)?.beanType != AcosInstanceController::class.java) {
                    return@HandlerExceptionResolver null
                }
                val message =
                    when (exception) {
                        is HttpMessageNotReadableException -> {
                            val jsonException =
                                generateSequence<Throwable>(exception) { it.cause }
                                    .filterIsInstance<JsonProcessingException>()
                                    .firstOrNull()
                            val fieldPath =
                                (jsonException as? JsonMappingException)?.path?.joinToString(".") {
                                    it.fieldName ?: "[${it.index}]"
                                }
                            val errorType = jsonException?.javaClass?.simpleName ?: exception.javaClass.simpleName
                            val targetType = (jsonException as? MismatchedInputException)?.targetType?.simpleName
                            val invalidMimeType =
                                if (fieldPath?.matches(Regex("""documents\.\[\d+]\.type""")) == true) {
                                    generateSequence<Throwable>(exception) { it.cause }
                                        .filterIsInstance<InvalidMediaTypeException>()
                                        .firstOrNull()
                                        ?.mediaType
                                } else {
                                    null
                                }
                            val mimeType =
                                invalidMimeType
                                    ?.let { String(JsonStringEncoder.getInstance().quoteAsString(it.take(256))) }
                                    ?.let { ", mimeType=\"$it\"" }
                                    .orEmpty()
                            // Only the invalid MIME type is logged; other values may contain sensitive data.
                            log.warn {
                                "Invalid ACOS request: method=${request.method}, path=${request.requestURI}, " +
                                    "error=$errorType, field=$fieldPath, targetType=$targetType, " +
                                    "line=${jsonException?.location?.lineNr}, " +
                                    "column=${jsonException?.location?.columnNr}$mimeType"
                            }
                            val field = fieldPath?.replace(".[", "[")?.takeIf { it.isNotBlank() } ?: "(JSON-roten)"
                            when {
                                invalidMimeType != null -> {
                                    val value = objectMapper.writeValueAsString(invalidMimeType.take(256))
                                    "Ugyldig MIME-type $value i feltet $field. " +
                                        "Forventet format type/subtype, for eksempel audio/mp4 eller application/pdf."
                                }

                                jsonException is InvalidNullException -> {
                                    "Påkrevd felt $field mangler eller er null."
                                }

                                jsonException is JsonParseException -> {
                                    "Ugyldig JSON på linje ${jsonException.location?.lineNr}, " +
                                        "kolonne ${jsonException.location?.columnNr}. Kontroller JSON-syntaksen."
                                }

                                jsonException is MismatchedInputException -> {
                                    val type = jsonException.targetType
                                    val expected =
                                        when {
                                            type == String::class.java -> "tekst"
                                            type != null && Collection::class.java.isAssignableFrom(type) -> "en liste"
                                            type != null -> "et objekt"
                                            else -> "en gyldig JSON-verdi"
                                        }
                                    "Feil datatype i feltet $field. Forventet $expected."
                                }

                                jsonException is JsonMappingException -> {
                                    "Ugyldig verdi i feltet $field."
                                }

                                else -> {
                                    "Forespørselen mangler en JSON-body eller kunne ikke leses som JSON."
                                }
                            }
                        }

                        is MethodArgumentNotValidException -> {
                            val errors = exception.bindingResult.fieldErrors.map { "${it.field}: ${it.code}" }
                            log.warn {
                                "Invalid ACOS request: method=${request.method}, path=${request.requestURI}, errors=$errors"
                            }
                            exception.bindingResult.allErrors.joinToString("; ") { error ->
                                val field = (error as? FieldError)?.field ?: error.objectName
                                "$field: ${error.defaultMessage ?: "ugyldig verdi"}"
                            }
                        }

                        else -> {
                            return@HandlerExceptionResolver null
                        }
                    }
                response.status = HttpStatus.BAD_REQUEST.value()
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                objectMapper.writeValue(
                    response.outputStream,
                    mapOf(
                        "timestamp" to OffsetDateTime.now().toString(),
                        "status" to HttpStatus.BAD_REQUEST.value(),
                        "error" to HttpStatus.BAD_REQUEST.reasonPhrase,
                        "message" to message,
                        "path" to request.requestURI,
                    ),
                )
                ModelAndView()
            },
        )
    }
}

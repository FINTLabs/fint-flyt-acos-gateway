package no.novari.flyt.acos.instance.gateway

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

private val log = KotlinLogging.logger {}

@Configuration
class InvalidInstanceRequestLogging : WebMvcConfigurer {
    override fun extendHandlerExceptionResolvers(resolvers: MutableList<HandlerExceptionResolver>) {
        resolvers.add(
            0,
            HandlerExceptionResolver { request, _, handler, exception ->
                if ((handler as? HandlerMethod)?.beanType == AcosInstanceController::class.java) {
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
                            // Exception messages and stack traces can contain submitted field values.
                            log.warn {
                                "Invalid ACOS request: method=${request.method}, path=${request.requestURI}, " +
                                    "error=$errorType, field=$fieldPath, targetType=$targetType, " +
                                    "line=${jsonException?.location?.lineNr}, column=${jsonException?.location?.columnNr}"
                            }
                        }

                        is MethodArgumentNotValidException -> {
                            val errors = exception.bindingResult.fieldErrors.map { "${it.field}: ${it.code}" }
                            log.warn {
                                "Invalid ACOS request: method=${request.method}, path=${request.requestURI}, errors=$errors"
                            }
                        }
                    }
                }
                // Let the existing exception handlers produce the response.
                null
            },
        )
    }
}

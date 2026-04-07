package no.novari.instance.gateway.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UniqueElementIdsValidator::class])
annotation class UniqueElementIds(
    val message: String = "contains duplicate element ids: [{duplicateElementIds}]",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

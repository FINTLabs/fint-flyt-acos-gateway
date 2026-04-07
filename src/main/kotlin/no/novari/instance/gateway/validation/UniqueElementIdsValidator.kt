package no.novari.instance.gateway.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.novari.instance.gateway.model.acos.AcosInstanceElement
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext

class UniqueElementIdsValidator : ConstraintValidator<UniqueElementIds, List<AcosInstanceElement>> {
    override fun isValid(
        value: List<AcosInstanceElement>?,
        constraintValidatorContext: ConstraintValidatorContext,
    ): Boolean {
        val duplicateElementIds = findDuplicateElementIds(value.orEmpty())
        if (duplicateElementIds.isEmpty()) {
            return true
        }

        if (constraintValidatorContext is HibernateConstraintValidatorContext) {
            constraintValidatorContext
                .unwrap(HibernateConstraintValidatorContext::class.java)
                .addMessageParameter("duplicateElementIds", duplicateElementIds.joinToString())
                .withDynamicPayload(duplicateElementIds.toList())
        }

        return false
    }

    private fun findDuplicateElementIds(acosInstanceElements: List<AcosInstanceElement>): List<String> {
        val items = mutableSetOf<String>()
        return acosInstanceElements
            .map { it.id }
            .filter { !items.add(it) }
            .distinct()
    }
}

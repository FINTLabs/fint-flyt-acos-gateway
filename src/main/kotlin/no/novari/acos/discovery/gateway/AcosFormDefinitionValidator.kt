package no.novari.acos.discovery.gateway

import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import no.novari.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.acos.discovery.gateway.model.acos.AcosFormElement
import org.springframework.stereotype.Service

@Service
class AcosFormDefinitionValidator(
    validatorFactory: ValidatorFactory,
) {
    private val fieldValidator: Validator = validatorFactory.validator

    fun validate(acosFormDefinition: AcosFormDefinition): List<String>? {
        val errors =
            fieldValidator
                .validate(acosFormDefinition)
                .map { constraintViolation ->
                    "${constraintViolation.propertyPath} ${constraintViolation.message}"
                }.sorted()
                .toMutableList()

        errors += validateElementIds(acosFormDefinition)

        return errors.takeIf { it.isNotEmpty() }
    }

    private fun validateElementIds(acosFormDefinition: AcosFormDefinition): List<String> {
        val elements = getElements(acosFormDefinition)
        val errors = mutableListOf<String>()

        val missingElementIds = findMissingElementIds(elements)
        if (missingElementIds.isNotEmpty()) {
            errors += "Missing element ID(s) for: $missingElementIds"
        }

        val duplicateElementIds = findDuplicateElementIds(elements)
        if (duplicateElementIds.isNotEmpty()) {
            errors += "Duplicate element ID(s): $duplicateElementIds"
        }

        return errors
    }

    private fun getElements(acosFormDefinition: AcosFormDefinition): List<AcosFormElement> {
        return buildList {
            acosFormDefinition.steps.orEmpty().forEach { step ->
                addAll(flattenElements(step.elements.orEmpty()))
            }

            addAll(flattenElements(acosFormDefinition.savedValues?.elements.orEmpty()))
        }
    }

    private fun findDuplicateElementIds(acosFormElements: List<AcosFormElement>): List<String> {
        val items = mutableSetOf<String>()

        return acosFormElements
            .mapNotNull(AcosFormElement::id)
            .filterNot(items::add)
    }

    private fun findMissingElementIds(acosFormElements: List<AcosFormElement>): List<String> {
        return acosFormElements
            .filterNot(::isGroupElement)
            .filter { it.id.isNullOrBlank() }
            .mapNotNull(AcosFormElement::displayName)
    }

    private fun isGroupElement(element: AcosFormElement): Boolean {
        return element.type.equals("Group", ignoreCase = true)
    }

    private fun flattenElements(elements: List<AcosFormElement>): List<AcosFormElement> {
        return buildList {
            elements.forEach { element ->
                add(element)
                addAll(flattenElements(element.elements.orEmpty()))
            }
        }
    }
}

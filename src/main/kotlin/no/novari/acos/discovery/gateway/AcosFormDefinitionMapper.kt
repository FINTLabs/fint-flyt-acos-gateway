package no.novari.acos.discovery.gateway

import no.novari.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.acos.discovery.gateway.model.acos.AcosFormElement
import no.novari.acos.discovery.gateway.model.acos.AcosFormSavedValues
import no.novari.acos.discovery.gateway.model.acos.AcosFormStep
import no.novari.acos.discovery.gateway.model.fint.InstanceMetadataCategory
import no.novari.acos.discovery.gateway.model.fint.InstanceMetadataContent
import no.novari.acos.discovery.gateway.model.fint.InstanceObjectCollectionMetadata
import no.novari.acos.discovery.gateway.model.fint.InstanceValueMetadata
import no.novari.acos.discovery.gateway.model.fint.IntegrationMetadata
import org.springframework.stereotype.Service

@Service
class AcosFormDefinitionMapper {
    fun toIntegrationMetadata(
        sourceApplicationId: Long,
        acosFormDefinition: AcosFormDefinition,
    ): IntegrationMetadata {
        val metadata = requireNotNull(acosFormDefinition.metadata)

        return IntegrationMetadata(
            sourceApplicationId = sourceApplicationId,
            sourceApplicationIntegrationId = requireNotNull(metadata.formId),
            sourceApplicationIntegrationUri = metadata.formUri,
            integrationDisplayName = requireNotNull(metadata.formDisplayName),
            version = metadata.version,
            instanceMetadata =
                InstanceMetadataContent(
                    instanceValueMetadata = listOf(createSkjemaPdfMetadata()),
                    instanceObjectCollectionMetadata = listOf(createVedleggMetadata()),
                    categories = toMetadataCategories(acosFormDefinition),
                ),
        )
    }

    private fun createSkjemaPdfMetadata(): InstanceValueMetadata {
        return InstanceValueMetadata(
            displayName = "Skjema-PDF",
            type = InstanceValueMetadata.Type.FILE,
            key = "skjemaPdf",
        )
    }

    private fun createVedleggMetadata(): InstanceObjectCollectionMetadata {
        return InstanceObjectCollectionMetadata(
            displayName = "Vedlegg",
            objectMetadata =
                InstanceMetadataContent(
                    instanceValueMetadata =
                        listOf(
                            InstanceValueMetadata(
                                displayName = "Navn",
                                type = InstanceValueMetadata.Type.STRING,
                                key = "navn",
                            ),
                            InstanceValueMetadata(
                                displayName = "Type",
                                type = InstanceValueMetadata.Type.STRING,
                                key = "type",
                            ),
                            InstanceValueMetadata(
                                displayName = "Enkoding",
                                type = InstanceValueMetadata.Type.STRING,
                                key = "enkoding",
                            ),
                            InstanceValueMetadata(
                                displayName = "Fil",
                                type = InstanceValueMetadata.Type.FILE,
                                key = "fil",
                            ),
                        ),
                ),
            key = "vedlegg",
        )
    }

    private fun toMetadataCategory(acosFormStep: AcosFormStep): InstanceMetadataCategory {
        return InstanceMetadataCategory(
            displayName = requireNotNull(acosFormStep.displayName),
            content = toMetadataContent(acosFormStep.elements),
        )
    }

    private fun toMetadataCategory(savedValues: AcosFormSavedValues): InstanceMetadataCategory {
        return InstanceMetadataCategory(
            displayName = requireNotNull(savedValues.displayName),
            content = toMetadataContent(savedValues.elements),
        )
    }

    private fun toMetadataCategory(acosFormElement: AcosFormElement): InstanceMetadataCategory {
        return InstanceMetadataCategory(
            displayName = requireNotNull(acosFormElement.displayName),
            content = toMetadataContent(acosFormElement.elements),
        )
    }

    private fun toInstanceValueMetadata(acosFormElement: AcosFormElement): InstanceValueMetadata {
        return InstanceValueMetadata(
            displayName = requireNotNull(acosFormElement.displayName),
            type = InstanceValueMetadata.Type.STRING,
            key = "skjema.${acosFormElement.id}",
        )
    }

    private fun toMetadataContent(elements: List<AcosFormElement>?): InstanceMetadataContent {
        val safeElements = elements.orEmpty()

        return InstanceMetadataContent(
            instanceValueMetadata = safeElements.filter(::isValueElement).map(::toInstanceValueMetadata),
            categories = safeElements.filter(::isGroupElement).map(::toMetadataCategory),
        )
    }

    private fun toMetadataCategories(definition: AcosFormDefinition): List<InstanceMetadataCategory> {
        val categories = mutableListOf<InstanceMetadataCategory>()

        definition.savedValues
            ?.takeIf { !it.displayName.isNullOrBlank() }
            ?.let { categories += toMetadataCategory(it) }

        categories += definition.steps.orEmpty().map(::toMetadataCategory)

        return categories
    }

    private fun isGroupElement(element: AcosFormElement): Boolean {
        return element.type.equals(GROUP_TYPE, ignoreCase = true)
    }

    private fun isValueElement(element: AcosFormElement): Boolean {
        return !isGroupElement(element) && !element.id.isNullOrBlank()
    }

    private companion object {
        private const val GROUP_TYPE = "Group"
    }
}

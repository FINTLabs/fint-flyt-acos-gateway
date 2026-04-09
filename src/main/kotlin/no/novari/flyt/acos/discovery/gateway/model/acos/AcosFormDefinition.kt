package no.novari.flyt.acos.discovery.gateway.model.acos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class AcosFormDefinition(
    @field:NotNull
    @field:Valid
    val metadata: AcosFormMetadata? = null,
    @field:Valid
    val savedValues: AcosFormSavedValues? = null,
    @field:Valid
    val steps: List<@Valid AcosFormStep>? = null,
)

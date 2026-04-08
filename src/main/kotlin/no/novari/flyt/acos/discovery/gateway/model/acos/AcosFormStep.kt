package no.novari.flyt.acos.discovery.gateway.model.acos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class AcosFormStep(
    @field:NotBlank
    val displayName: String? = null,
    @field:Valid
    val elements: List<@Valid AcosFormElement>? = null,
)

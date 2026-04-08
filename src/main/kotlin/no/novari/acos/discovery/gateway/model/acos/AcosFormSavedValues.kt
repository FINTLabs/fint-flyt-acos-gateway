package no.novari.acos.discovery.gateway.model.acos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class AcosFormSavedValues(
    @field:NotBlank
    val displayName: String? = null,
    @field:Valid
    val elements: List<@Valid AcosFormElement>? = null,
)

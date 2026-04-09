package no.novari.flyt.acos.discovery.gateway.model.acos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class AcosFormElement(
    val id: String? = null,
    @field:NotBlank
    val displayName: String? = null,
    @field:NotBlank
    val type: String? = null,
    @field:Valid
    val elements: List<@Valid AcosFormElement>? = null,
)

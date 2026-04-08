package no.novari.flyt.acos.discovery.gateway.model.acos

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AcosFormMetadata(
    @field:NotBlank
    val formId: String? = null,
    @field:NotBlank
    val formDisplayName: String? = null,
    val formUri: String? = null,
    @field:NotNull
    val version: Long? = null,
)

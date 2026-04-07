package no.novari.instance.gateway.model.acos

import jakarta.validation.constraints.NotBlank

data class AcosInstanceMetadata(
    @field:NotBlank
    val formId: String,
    @field:NotBlank
    val instanceId: String,
    val instanceUri: String? = null,
)

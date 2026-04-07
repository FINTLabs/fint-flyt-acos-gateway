package no.novari.instance.gateway.model.acos

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.novari.flyt.gateway.webinstance.validation.constraints.ValidBase64
import no.novari.instance.gateway.validation.UniqueElementIds

data class AcosInstance(
    @field:NotNull
    @field:Valid
    val metadata: AcosInstanceMetadata,
    @field:NotEmpty
    @field:UniqueElementIds
    @field:Valid
    val elements: List<@NotNull AcosInstanceElement>,
    @field:NotBlank
    @field:ValidBase64
    val formPdfBase64: String,
    @field:Valid
    val documents: List<@NotNull AcosDocument> = emptyList(),
)

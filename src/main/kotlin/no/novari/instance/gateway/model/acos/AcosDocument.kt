package no.novari.instance.gateway.model.acos

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.novari.flyt.gateway.webinstance.validation.constraints.ValidBase64
import org.springframework.http.MediaType

data class AcosDocument(
    @field:NotBlank
    val name: String,
    @field:NotNull
    val type: MediaType,
    val encoding: String? = null,
    @field:NotNull
    @field:ValidBase64
    val base64: String,
)

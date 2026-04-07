package no.novari.instance.gateway.model.acos

import jakarta.validation.constraints.NotBlank

class AcosInstanceElement(
    @field:NotBlank
    val id: String,
    val value: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is AcosInstanceElement) {
            return false
        }

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "AcosInstanceElement(id='$id', value=$value)"
    }
}

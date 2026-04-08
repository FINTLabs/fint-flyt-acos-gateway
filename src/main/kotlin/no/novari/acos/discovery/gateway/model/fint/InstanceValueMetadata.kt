package no.novari.acos.discovery.gateway.model.fint

data class InstanceValueMetadata(
    val displayName: String,
    val type: Type,
    val key: String,
) {
    enum class Type {
        STRING,
        BOOLEAN,
        FILE,
    }
}

package no.novari.acos.discovery.gateway.model.fint

data class InstanceMetadataContent(
    val instanceValueMetadata: List<InstanceValueMetadata> = emptyList(),
    val instanceObjectCollectionMetadata: List<InstanceObjectCollectionMetadata> = emptyList(),
    val categories: List<InstanceMetadataCategory> = emptyList(),
)

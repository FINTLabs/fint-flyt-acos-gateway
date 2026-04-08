package no.novari.acos.discovery.gateway.model.fint

data class IntegrationMetadata(
    val sourceApplicationId: Long,
    val sourceApplicationIntegrationId: String,
    val sourceApplicationIntegrationUri: String?,
    val integrationDisplayName: String,
    val version: Long?,
    val instanceMetadata: InstanceMetadataContent,
)

package no.novari.flyt.acos.instance.gateway

import no.novari.fint.model.resource.arkiv.noark.SakResource
import no.novari.flyt.gateway.instance.HistoryClient
import no.novari.flyt.security.FlytSecurityProperties
import org.springframework.stereotype.Service

@Service
class ArchiveCaseService(
    private val historyClient: HistoryClient,
    private val archiveGatewayClient: ArchiveGatewayClient,
    private val securityProperties: FlytSecurityProperties,
) {
    fun getCase(archiveCaseId: String): SakResource? =
        archiveGatewayClient.getCaseWithFilteredJournalposts(archiveCaseId)

    fun getCaseForInstance(sourceApplicationInstanceId: String): SakResource? {
        val sourceApplicationId = securityProperties.external.sourceApplicationId!!
        val archiveCaseId =
            historyClient.getArchiveInstanceId(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationInstanceId = sourceApplicationInstanceId,
            ) ?: return null

        return getCase(archiveCaseId)
    }
}

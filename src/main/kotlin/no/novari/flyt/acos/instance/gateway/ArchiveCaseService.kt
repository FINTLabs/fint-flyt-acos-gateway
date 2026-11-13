package no.novari.flyt.acos.instance.gateway

import no.novari.fint.model.resource.arkiv.noark.SakResource
import no.novari.flyt.gateway.instance.HistoryClient
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class ArchiveCaseService(
    private val historyClient: HistoryClient,
    private val archiveGatewayClient: ArchiveGatewayClient,
    private val sourceApplicationAuthorizationService: SourceApplicationAuthorizationService,
) {
    fun getCase(archiveCaseId: String): SakResource? =
        archiveGatewayClient.getCaseWithFilteredJournalposts(archiveCaseId)

    fun getCase(
        authentication: Authentication,
        sourceApplicationInstanceId: String,
    ): SakResource? {
        val sourceApplicationId = sourceApplicationAuthorizationService.getSourceApplicationId(authentication)
        val archiveCaseId =
            historyClient.getArchiveInstanceId(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationInstanceId = sourceApplicationInstanceId,
            ) ?: return null

        return getCase(archiveCaseId)
    }
}

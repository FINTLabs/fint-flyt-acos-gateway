package no.novari.flyt.acos.instance.gateway

import no.novari.fint.model.resource.arkiv.noark.SakResource
import no.novari.flyt.gateway.instance.HistoryClient
import no.novari.flyt.security.SourceApplication
import org.springframework.stereotype.Service

@Service
class ArchiveCaseService(
    private val historyClient: HistoryClient,
    private val archiveGatewayClient: ArchiveGatewayClient,
) {
    fun getCase(archiveCaseId: String): SakResource? =
        archiveGatewayClient.getCaseWithFilteredJournalposts(archiveCaseId)

    fun getCaseForInstance(sourceApplicationInstanceId: String): SakResource? {
        val archiveCaseId =
            historyClient.getArchiveInstanceId(
                sourceApplicationId = SourceApplication.ACOS.id,
                sourceApplicationInstanceId = sourceApplicationInstanceId,
            ) ?: return null

        return getCase(archiveCaseId)
    }
}

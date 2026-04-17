package no.novari.flyt.acos.instance.gateway

import no.novari.fint.model.resource.arkiv.noark.SakResource
import no.novari.flyt.gateway.webinstance.kafka.ArchiveCaseIdRequestService
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service

@Service
class ArchiveCaseService(
    private val archiveCaseIdRequestService: ArchiveCaseIdRequestService,
    private val archiveCaseRequestService: ArchiveCaseRequestService,
    private val sourceApplicationAuthorizationService: SourceApplicationAuthorizationService,
) {
    fun getCase(archiveCaseId: String): SakResource? = archiveCaseRequestService.getByArchiveCaseId(archiveCaseId)

    fun getCase(
        authentication: Authentication,
        sourceApplicationInstanceId: String,
    ): SakResource? {
        val sourceApplicationId = sourceApplicationAuthorizationService.getSourceApplicationId(authentication)
        val archiveCaseId =
            archiveCaseIdRequestService.getArchiveCaseId(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationInstanceId = sourceApplicationInstanceId,
            ) ?: return null

        return getCase(archiveCaseId)
    }
}

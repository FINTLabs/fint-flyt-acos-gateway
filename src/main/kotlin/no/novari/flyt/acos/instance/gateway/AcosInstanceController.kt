package no.novari.flyt.acos.instance.gateway

import jakarta.validation.Valid
import no.novari.flyt.acos.instance.gateway.caseinfo.CaseInfoMappingService
import no.novari.flyt.acos.instance.gateway.model.acos.AcosInstance
import no.novari.flyt.acos.instance.gateway.model.caseinfo.CaseInfo
import no.novari.flyt.gateway.webinstance.InstanceProcessor
import no.novari.flyt.webresourceserver.UrlPaths.EXTERNAL_API
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("$EXTERNAL_API/acos/instances")
class AcosInstanceController(
    private val instanceProcessor: InstanceProcessor<AcosInstance>,
    private val archiveCaseService: ArchiveCaseService,
    private val caseInfoMappingService: CaseInfoMappingService,
) {
    @GetMapping("{sourceApplicationInstanceId}/case-info")
    fun getInstanceCaseInfo(
        authentication: Authentication,
        @PathVariable sourceApplicationInstanceId: String,
    ): ResponseEntity<CaseInfo> {
        val caseInfo =
            archiveCaseService
                .getCase(authentication, sourceApplicationInstanceId)
                ?.let { caseResource ->
                    caseInfoMappingService.toCaseInfo(sourceApplicationInstanceId, caseResource)
                }
                ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Case with sourceApplicationInstanceId=$sourceApplicationInstanceId could not be found",
                )

        return ResponseEntity.ok(caseInfo)
    }

    @PostMapping
    fun postInstance(
        @Valid @RequestBody acosInstance: AcosInstance,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        log.info("Received instance: {}", acosInstance)
        return instanceProcessor.processInstance(authentication, acosInstance)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(AcosInstanceController::class.java)
    }
}

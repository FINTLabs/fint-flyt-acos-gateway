package no.novari.flyt.acos.discovery.gateway

import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.flyt.webresourceserver.UrlPaths.EXTERNAL_API
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("$EXTERNAL_API/acos/metadata")
class AcosIntegrationMetadataController(
    private val acosFormDefinitionMapper: AcosFormDefinitionMapper,
    private val acosFormDefinitionValidator: AcosFormDefinitionValidator,
    private val integrationMetadataProducerService: IntegrationMetadataProducerService,
    private val sourceApplicationAuthorizationService: SourceApplicationAuthorizationService,
) {
    @PostMapping
    fun postIntegrationMetadata(
        @RequestBody acosFormDefinition: AcosFormDefinition,
        authentication: Authentication,
    ): ResponseEntity<Void> {
        log.info("Received acos form definition: {}", acosFormDefinition)

        acosFormDefinitionValidator.validate(acosFormDefinition)?.let { validationErrors ->
            throw ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation error(s): ${validationErrors.map { "'$it'" }}",
            )
        }

        val integrationMetadata =
            acosFormDefinitionMapper.toIntegrationMetadata(
                sourceApplicationAuthorizationService.getSourceApplicationId(authentication),
                acosFormDefinition,
            )

        integrationMetadataProducerService.publishNewIntegrationMetadata(integrationMetadata)

        return ResponseEntity.accepted().build()
    }

    private companion object {
        private val log = LoggerFactory.getLogger(AcosIntegrationMetadataController::class.java)
    }
}

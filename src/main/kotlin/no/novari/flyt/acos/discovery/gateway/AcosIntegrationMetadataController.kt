package no.novari.flyt.acos.discovery.gateway

import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.flyt.gateway.metadata.IntegrationMetadataProcessor
import no.novari.flyt.security.UrlPaths.EXTERNAL_API
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("$EXTERNAL_API/acos/metadata")
class AcosIntegrationMetadataController(
    private val acosFormDefinitionMapper: AcosFormDefinitionMapper,
    private val acosFormDefinitionValidator: AcosFormDefinitionValidator,
    private val integrationMetadataProcessor: IntegrationMetadataProcessor,
) {
    @PostMapping
    fun postIntegrationMetadata(
        @RequestBody acosFormDefinition: AcosFormDefinition,
    ): ResponseEntity<Void> {
        return integrationMetadataProcessor.processIntegrationMetadata(
            incomingMetadata = acosFormDefinition,
            integrationMetadataMapper = acosFormDefinitionMapper,
            integrationMetadataValidator = acosFormDefinitionValidator,
        )
    }
}

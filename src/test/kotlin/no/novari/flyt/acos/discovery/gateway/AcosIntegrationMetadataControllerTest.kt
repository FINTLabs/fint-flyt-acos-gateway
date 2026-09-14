package no.novari.flyt.acos.discovery.gateway

import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormMetadata
import no.novari.flyt.gateway.metadata.IntegrationMetadataProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication

@ExtendWith(MockitoExtension::class)
class AcosIntegrationMetadataControllerTest {
    @Mock
    private lateinit var acosFormDefinitionMapper: AcosFormDefinitionMapper

    @Mock
    private lateinit var acosFormDefinitionValidator: AcosFormDefinitionValidator

    @Mock
    private lateinit var integrationMetadataProcessor: IntegrationMetadataProcessor

    @Mock
    private lateinit var authentication: Authentication

    @Test
    fun postIntegrationMetadataShouldDelegateToIntegrationMetadataProcessor() {
        val acosFormDefinition =
            AcosFormDefinition(
                metadata =
                    AcosFormMetadata(
                        formId = "integration-id",
                        formDisplayName = "Integration",
                        version = 1L,
                    ),
            )

        whenever(
            integrationMetadataProcessor.processIntegrationMetadata(
                authentication = authentication,
                incomingMetadata = acosFormDefinition,
                integrationMetadataMapper = acosFormDefinitionMapper,
                integrationMetadataValidator = acosFormDefinitionValidator,
            ),
        ).thenReturn(ResponseEntity.accepted().build<Void>())

        val controller =
            AcosIntegrationMetadataController(
                acosFormDefinitionMapper = acosFormDefinitionMapper,
                acosFormDefinitionValidator = acosFormDefinitionValidator,
                integrationMetadataProcessor = integrationMetadataProcessor,
            )

        val responseEntity = controller.postIntegrationMetadata(acosFormDefinition, authentication)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        verify(integrationMetadataProcessor).processIntegrationMetadata(
            authentication = authentication,
            incomingMetadata = acosFormDefinition,
            integrationMetadataMapper = acosFormDefinitionMapper,
            integrationMetadataValidator = acosFormDefinitionValidator,
        )
    }
}

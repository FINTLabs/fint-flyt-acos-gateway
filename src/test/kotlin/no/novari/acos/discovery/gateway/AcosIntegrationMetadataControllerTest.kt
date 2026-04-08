package no.novari.acos.discovery.gateway

import no.novari.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.acos.discovery.gateway.model.acos.AcosFormElement
import no.novari.acos.discovery.gateway.model.acos.AcosFormStep
import no.novari.acos.discovery.gateway.model.fint.InstanceMetadataContent
import no.novari.acos.discovery.gateway.model.fint.IntegrationMetadata
import no.novari.flyt.webresourceserver.security.client.sourceapplication.SourceApplicationAuthorizationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.server.ResponseStatusException

@ExtendWith(MockitoExtension::class)
class AcosIntegrationMetadataControllerTest {
    @Mock
    private lateinit var acosFormDefinitionMapper: AcosFormDefinitionMapper

    @Mock
    private lateinit var acosFormDefinitionValidator: AcosFormDefinitionValidator

    @Mock
    private lateinit var integrationMetadataProducerService: IntegrationMetadataProducerService

    @Mock
    private lateinit var authentication: Authentication

    @Mock
    private lateinit var sourceApplicationAuthorizationService: SourceApplicationAuthorizationService

    @Test
    fun postIntegrationMetadataValidInputShouldReturnAccepted() {
        val sourceApplicationId = 1L
        val acosFormDefinition = mockValidFormDefinition()
        val mockedMetadata =
            IntegrationMetadata(
                sourceApplicationId = sourceApplicationId,
                sourceApplicationIntegrationId = "integration-id",
                sourceApplicationIntegrationUri = null,
                integrationDisplayName = "Integration",
                version = 1L,
                instanceMetadata = InstanceMetadataContent(),
            )

        whenever(
            sourceApplicationAuthorizationService.getSourceApplicationId(authentication),
        ).thenReturn(sourceApplicationId)
        whenever(acosFormDefinitionValidator.validate(acosFormDefinition)).thenReturn(null)
        whenever(
            acosFormDefinitionMapper.toIntegrationMetadata(
                eq(sourceApplicationId),
                eq(acosFormDefinition),
            ),
        ).thenReturn(mockedMetadata)

        val controller =
            AcosIntegrationMetadataController(
                acosFormDefinitionMapper = acosFormDefinitionMapper,
                acosFormDefinitionValidator = acosFormDefinitionValidator,
                integrationMetadataProducerService = integrationMetadataProducerService,
                sourceApplicationAuthorizationService = sourceApplicationAuthorizationService,
            )

        val responseEntity = controller.postIntegrationMetadata(acosFormDefinition, authentication)

        assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        verify(acosFormDefinitionValidator).validate(acosFormDefinition)
        verify(acosFormDefinitionMapper).toIntegrationMetadata(eq(sourceApplicationId), eq(acosFormDefinition))
        verify(integrationMetadataProducerService).publishNewIntegrationMetadata(mockedMetadata)
    }

    @Test
    fun postIntegrationMetadataValidationErrorsShouldReturnUnprocessableEntity() {
        val acosFormDefinition = mockFormDefinitionWithDuplicateIds()

        whenever(acosFormDefinitionValidator.validate(acosFormDefinition)).thenReturn(listOf("Error"))

        val controller =
            AcosIntegrationMetadataController(
                acosFormDefinitionMapper = acosFormDefinitionMapper,
                acosFormDefinitionValidator = acosFormDefinitionValidator,
                integrationMetadataProducerService = integrationMetadataProducerService,
                sourceApplicationAuthorizationService = sourceApplicationAuthorizationService,
            )

        val exception =
            assertThrows(ResponseStatusException::class.java) {
                controller.postIntegrationMetadata(acosFormDefinition, authentication)
            }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        verify(integrationMetadataProducerService, never()).publishNewIntegrationMetadata(any())
    }

    @Test
    fun shouldThrowResponseStatusExceptionWhenInvalid() {
        val mockFormDefinition = mockFormDefinitionWithDuplicateIds()
        whenever(acosFormDefinitionValidator.validate(mockFormDefinition)).thenReturn(listOf("Error1", "Error2"))

        val controller =
            AcosIntegrationMetadataController(
                acosFormDefinitionMapper = acosFormDefinitionMapper,
                acosFormDefinitionValidator = acosFormDefinitionValidator,
                integrationMetadataProducerService = integrationMetadataProducerService,
                sourceApplicationAuthorizationService = sourceApplicationAuthorizationService,
            )

        assertThrows(ResponseStatusException::class.java) {
            controller.postIntegrationMetadata(mockFormDefinition, authentication)
        }
    }

    private fun mockValidFormDefinition(): AcosFormDefinition {
        val element1 =
            AcosFormElement(
                id = "1",
                displayName = "TestElement1",
                type = "Type1",
            )
        val element2 =
            AcosFormElement(
                id = "2",
                displayName = "TestElement2",
                type = "Type2",
            )
        val group =
            AcosFormElement(
                displayName = "TestGroup",
                type = "Group",
                elements = listOf(element1, element2),
            )
        val step =
            AcosFormStep(
                displayName = "TestStep",
                elements = listOf(group),
            )

        return AcosFormDefinition(steps = listOf(step))
    }

    private fun mockFormDefinitionWithDuplicateIds(): AcosFormDefinition {
        val element1 =
            AcosFormElement(
                id = "1",
                displayName = "TestElement1",
                type = "Type1",
            )
        val element2 =
            AcosFormElement(
                id = "1",
                displayName = "TestElement2",
                type = "Type2",
            )
        val group =
            AcosFormElement(
                displayName = "TestGroup",
                type = "Group",
                elements = listOf(element1, element2),
            )
        val step =
            AcosFormStep(
                displayName = "TestStep",
                elements = listOf(group),
            )

        return AcosFormDefinition(steps = listOf(step))
    }
}

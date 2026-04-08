package no.novari.acos.discovery.gateway

import jakarta.validation.ConstraintViolation
import jakarta.validation.Path
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory
import no.novari.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.acos.discovery.gateway.model.acos.AcosFormElement
import no.novari.acos.discovery.gateway.model.acos.AcosFormStep
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AcosFormDefinitionValidatorTest {
    @Mock
    private lateinit var validatorFactory: ValidatorFactory

    @Mock
    private lateinit var validator: Validator

    private lateinit var acosFormDefinitionValidator: AcosFormDefinitionValidator

    @BeforeEach
    fun setUp() {
        whenever(validatorFactory.validator).thenReturn(validator)
        acosFormDefinitionValidator = AcosFormDefinitionValidator(validatorFactory)
    }

    @Test
    fun shouldReturnValidatorErrors() {
        val definition = mockValidFormDefinition()

        val violation = mock<ConstraintViolation<AcosFormDefinition>>()
        val mockPath = mock<Path>()
        whenever(mockPath.toString()).thenReturn("mockedPath")
        whenever(violation.propertyPath).thenReturn(mockPath)
        whenever(violation.message).thenReturn("Some validation error.")
        whenever(validator.validate(any<AcosFormDefinition>())).thenReturn(setOf(violation))

        val result = acosFormDefinitionValidator.validate(definition)

        assertNotNull(result)
        assertTrue(result!!.contains("mockedPath Some validation error."))
    }

    @Test
    fun shouldReturnEmptyWhenValidFormDefinition() {
        val definition = mockValidFormDefinition()
        whenever(validator.validate(any<AcosFormDefinition>())).thenReturn(emptySet())

        val result = acosFormDefinitionValidator.validate(definition)

        assertNull(result)
    }

    @Test
    fun shouldReturnErrorsWhenInvalidFormDefinition() {
        val definition = mockFormDefinitionWithDuplicateIds()
        whenever(validator.validate(any<AcosFormDefinition>())).thenReturn(emptySet())

        val result = acosFormDefinitionValidator.validate(definition)

        assertNotNull(result)
    }

    @Test
    fun shouldIdentifyDuplicateElementIds() {
        val definition = mockFormDefinitionWithDuplicateIds()
        whenever(validator.validate(any<AcosFormDefinition>())).thenReturn(emptySet())

        val result = acosFormDefinitionValidator.validate(definition)

        assertNotNull(result)
        assertTrue(result!!.contains("Duplicate element ID(s): [1]"))
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

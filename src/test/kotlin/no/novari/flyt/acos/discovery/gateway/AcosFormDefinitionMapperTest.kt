package no.novari.flyt.acos.discovery.gateway

import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormDefinition
import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormElement
import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormMetadata
import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormSavedValues
import no.novari.flyt.acos.discovery.gateway.model.acos.AcosFormStep
import no.novari.flyt.acos.discovery.gateway.model.fint.InstanceMetadataCategory
import no.novari.flyt.acos.discovery.gateway.model.fint.InstanceMetadataContent
import no.novari.flyt.acos.discovery.gateway.model.fint.InstanceObjectCollectionMetadata
import no.novari.flyt.acos.discovery.gateway.model.fint.InstanceValueMetadata
import no.novari.flyt.acos.discovery.gateway.model.fint.IntegrationMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AcosFormDefinitionMapperTest {
    private lateinit var acosFormDefinitionMapper: AcosFormDefinitionMapper
    private lateinit var acosFormDefinition: AcosFormDefinition
    private lateinit var expectedIntegrationMetadata: IntegrationMetadata

    @BeforeEach
    fun setup() {
        acosFormDefinitionMapper = AcosFormDefinitionMapper()

        acosFormDefinition =
            AcosFormDefinition(
                metadata =
                    AcosFormMetadata(
                        formId = "Test0488",
                        formDisplayName = "Test integration",
                        formUri = "https://edit.acos.com?formid=test0488",
                    ),
                savedValues =
                    AcosFormSavedValues(
                        displayName = "SavedValues",
                        elements =
                            listOf(
                                group(
                                    "From logic",
                                    listOf(
                                        input("saved.logic", "Logic field"),
                                    ),
                                ),
                            ),
                    ),
                steps =
                    listOf(
                        AcosFormStep(
                            displayName = "Person med valg",
                            elements =
                                listOf(
                                    group(
                                        "Person 1",
                                        listOf(
                                            input("person_1.fornavn", "Fornavn"),
                                            input("person_1.etternavn", "Etternavn"),
                                        ),
                                    ),
                                    group(
                                        "Person 2",
                                        listOf(
                                            input("person_2.fornavn", "Fornavn"),
                                            input("person_2.etternavn", "Etternavn"),
                                            AcosFormElement(
                                                id = "person_2.fødselsdato",
                                                displayName = "Fødselsdato",
                                                type = "NumberBox",
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )

        expectedIntegrationMetadata =
            IntegrationMetadata(
                sourceApplicationId = 1L,
                sourceApplicationIntegrationId = "Test0488",
                sourceApplicationIntegrationUri = "https://edit.acos.com?formid=test0488",
                integrationDisplayName = "Test integration",
                version = null,
                instanceMetadata =
                    InstanceMetadataContent(
                        instanceValueMetadata =
                            listOf(
                                InstanceValueMetadata(
                                    displayName = "Skjema-PDF",
                                    key = "skjemaPdf",
                                    type = InstanceValueMetadata.Type.FILE,
                                ),
                            ),
                        instanceObjectCollectionMetadata =
                            listOf(
                                InstanceObjectCollectionMetadata(
                                    displayName = "Vedlegg",
                                    key = "vedlegg",
                                    objectMetadata =
                                        InstanceMetadataContent(
                                            instanceValueMetadata =
                                                listOf(
                                                    InstanceValueMetadata(
                                                        displayName = "Navn",
                                                        key = "navn",
                                                        type = InstanceValueMetadata.Type.STRING,
                                                    ),
                                                    InstanceValueMetadata(
                                                        displayName = "Type",
                                                        key = "type",
                                                        type = InstanceValueMetadata.Type.STRING,
                                                    ),
                                                    InstanceValueMetadata(
                                                        displayName = "Enkoding",
                                                        key = "enkoding",
                                                        type = InstanceValueMetadata.Type.STRING,
                                                    ),
                                                    InstanceValueMetadata(
                                                        displayName = "Fil",
                                                        key = "fil",
                                                        type = InstanceValueMetadata.Type.FILE,
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                        categories =
                            listOf(
                                InstanceMetadataCategory(
                                    displayName = "SavedValues",
                                    content =
                                        InstanceMetadataContent(
                                            categories =
                                                listOf(
                                                    InstanceMetadataCategory(
                                                        displayName = "From logic",
                                                        content =
                                                            InstanceMetadataContent(
                                                                instanceValueMetadata =
                                                                    listOf(
                                                                        InstanceValueMetadata(
                                                                            key = "skjema.saved.logic",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            displayName = "Logic field",
                                                                        ),
                                                                    ),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                ),
                                InstanceMetadataCategory(
                                    displayName = "Person med valg",
                                    content =
                                        InstanceMetadataContent(
                                            categories =
                                                listOf(
                                                    InstanceMetadataCategory(
                                                        displayName = "Person 1",
                                                        content =
                                                            InstanceMetadataContent(
                                                                instanceValueMetadata =
                                                                    listOf(
                                                                        InstanceValueMetadata(
                                                                            key = "skjema.person_1.fornavn",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            displayName = "Fornavn",
                                                                        ),
                                                                        InstanceValueMetadata(
                                                                            key = "skjema.person_1.etternavn",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            displayName = "Etternavn",
                                                                        ),
                                                                    ),
                                                            ),
                                                    ),
                                                    InstanceMetadataCategory(
                                                        displayName = "Person 2",
                                                        content =
                                                            InstanceMetadataContent(
                                                                instanceValueMetadata =
                                                                    listOf(
                                                                        InstanceValueMetadata(
                                                                            key = "skjema.person_2.fornavn",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            displayName = "Fornavn",
                                                                        ),
                                                                        InstanceValueMetadata(
                                                                            key = "skjema.person_2.etternavn",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            displayName = "Etternavn",
                                                                        ),
                                                                        InstanceValueMetadata(
                                                                            key = "skjema.person_2.fødselsdato",
                                                                            type = InstanceValueMetadata.Type.STRING,
                                                                            displayName = "Fødselsdato",
                                                                        ),
                                                                    ),
                                                            ),
                                                    ),
                                                ),
                                        ),
                                ),
                            ),
                    ),
            )
    }

    @Test
    fun shouldMapToIntegrationMetadata() {
        val mappingResult = acosFormDefinitionMapper.toIntegrationMetadata(1L, acosFormDefinition)

        assertThat(mappingResult).isEqualTo(expectedIntegrationMetadata)
    }

    @Test
    fun shouldMapFullFormDefinitionWithSavedValues() {
        val definition =
            AcosFormDefinition(
                metadata =
                    AcosFormMetadata(
                        formId = "ACOS-11",
                        formDisplayName = "FINT-test",
                        formUri = "",
                        version = 5L,
                    ),
                savedValues =
                    AcosFormSavedValues(
                        displayName = "SavedValues",
                        elements =
                            listOf(
                                group("From logic", emptyList()),
                                group(
                                    "From logged in user",
                                    listOf(
                                        input("Login.UserID", "User Id"),
                                        input("Login.FirstName", "First name"),
                                        input("Login.LastName", "Last name"),
                                        input("Login.Address", "Address"),
                                        input("Login.PostalCode", "Postal code"),
                                        input("Login.PostalArea", "Postal area"),
                                        input("Login.Telephone", "Telephone"),
                                        input("Login.Email", "Email address"),
                                        group(
                                            "ID Porten",
                                            listOf(
                                                group(
                                                    "Folkeregister",
                                                    listOf(
                                                        input(
                                                            "Login.IDPorten.Folkeregister.Fødselsnummer",
                                                            "National ID-number",
                                                        ),
                                                        input("Login.IDPorten.Folkeregister.Gatenavn", "Street name"),
                                                        input(
                                                            "Login.IDPorten.Folkeregister.Gatenummer",
                                                            "Street number",
                                                        ),
                                                        input(
                                                            "Login.IDPorten.Folkeregister.Husbokstav",
                                                            "House letter",
                                                        ),
                                                        input("Login.IDPorten.Folkeregister.Postnummer", "Postal code"),
                                                        input("Login.IDPorten.Folkeregister.Poststed", "Postal area"),
                                                    ),
                                                ),
                                                group(
                                                    "KRR",
                                                    listOf(
                                                        input("Login.IDPorten.KRR.Telefon", "Telephone"),
                                                        input("Login.IDPorten.KRR.Epost", "Email"),
                                                        input("Login.IDPorten.KRR.Reservert", "Reserved"),
                                                    ),
                                                ),
                                            ),
                                        ),
                                        group(
                                            "Entra ID",
                                            listOf(
                                                input("Login.AzureAD.EmployeeID", "Employee ID"),
                                                input("Login.AzureAD.Department", "Department"),
                                                input("Login.AzureAD.Account_name", "Account name"),
                                                input("Login.AzureAD.Job_title", "Job title"),
                                                group(
                                                    "Nearest manager",
                                                    listOf(
                                                        input("Login.AzureAD.Manager.FirstName", "First name"),
                                                        input("Login.AzureAD.Manager.LastName", "Surname"),
                                                        input("Login.AzureAD.Manager.Email", "Email"),
                                                        input("Login.AzureAD.Manager.MobilePhone", "Mobile phone"),
                                                        input("Login.AzureAD.Manager.BusinessPhones", "Work phone"),
                                                        input("Login.AzureAD.Manager.UPN", "UPN (Username)"),
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                                group("From integrations", emptyList()),
                                group("From dataset", emptyList()),
                            ),
                    ),
                steps =
                    listOf(
                        AcosFormStep(
                            displayName = "Skjemasteg",
                            elements =
                                listOf(
                                    input("Skjemasteg.Felt_utenfor_gr", "Felt utenfor gruppe"),
                                    group(
                                        "Gruppe",
                                        listOf(
                                            input("Skjemasteg.Gruppe.Felt_i_gruppe", "Felt i gruppe"),
                                        ),
                                    ),
                                    group(
                                        "Gruppe med gruppe",
                                        listOf(
                                            group(
                                                "Undefined label 1",
                                                listOf(
                                                    dropdown(
                                                        "Skjemasteg.Gruppe_med_grup.Organisasjon.Organisasjonsna",
                                                        "Organisasjonsnavn",
                                                    ),
                                                    input(
                                                        "Skjemasteg.Gruppe_med_grup.Organisasjon.Organisasjonsnu",
                                                        "Organisasjonsnummer",
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )

        val result = acosFormDefinitionMapper.toIntegrationMetadata(1L, definition)

        assertThat(result.sourceApplicationIntegrationId).isEqualTo("ACOS-11")
        assertThat(result.integrationDisplayName).isEqualTo("FINT-test")
        assertThat(result.version).isEqualTo(5L)

        val content = result.instanceMetadata
        val savedValuesCategory = findCategory(content.categories, "SavedValues")
        val loggedInUserCategory = findCategory(savedValuesCategory.content.categories, "From logged in user")
        val idPortenCategory = findCategory(loggedInUserCategory.content.categories, "ID Porten")
        val folkeregisterCategory = findCategory(idPortenCategory.content.categories, "Folkeregister")
        assertThat(
            findValue(
                folkeregisterCategory.content.instanceValueMetadata,
                "skjema.Login.IDPorten.Folkeregister.Fødselsnummer",
            ),
        ).isNotNull()

        val entraIdCategory = findCategory(loggedInUserCategory.content.categories, "Entra ID")
        val managerCategory = findCategory(entraIdCategory.content.categories, "Nearest manager")
        assertThat(
            findValue(managerCategory.content.instanceValueMetadata, "skjema.Login.AzureAD.Manager.UPN"),
        ).isNotNull()

        val stepCategory = findCategory(content.categories, "Skjemasteg")
        assertThat(
            findValue(stepCategory.content.instanceValueMetadata, "skjema.Skjemasteg.Felt_utenfor_gr"),
        ).isNotNull()
        val groupWithGroup = findCategory(stepCategory.content.categories, "Gruppe med gruppe")
        val undefinedLabel = findCategory(groupWithGroup.content.categories, "Undefined label 1")
        assertThat(
            findValue(
                undefinedLabel.content.instanceValueMetadata,
                "skjema.Skjemasteg.Gruppe_med_grup.Organisasjon.Organisasjonsna",
            ),
        ).isNotNull()
    }

    private fun input(
        id: String,
        displayName: String,
    ): AcosFormElement {
        return AcosFormElement(
            id = id,
            displayName = displayName,
            type = "InputBox",
        )
    }

    private fun dropdown(
        id: String,
        displayName: String,
    ): AcosFormElement {
        return AcosFormElement(
            id = id,
            displayName = displayName,
            type = "DropdownList",
        )
    }

    private fun group(
        displayName: String,
        elements: List<AcosFormElement>,
    ): AcosFormElement {
        return AcosFormElement(
            displayName = displayName,
            type = "Group",
            elements = elements,
        )
    }

    private fun findCategory(
        categories: Collection<InstanceMetadataCategory>,
        displayName: String,
    ): InstanceMetadataCategory {
        return categories.firstOrNull { it.displayName == displayName }
            ?: throw IllegalStateException("Missing category: $displayName")
    }

    private fun findValue(
        values: Collection<InstanceValueMetadata>,
        key: String,
    ): InstanceValueMetadata? {
        return values.firstOrNull { it.key == key }
    }
}

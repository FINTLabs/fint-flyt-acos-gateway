package no.novari.instance.gateway

import no.novari.flyt.gateway.webinstance.model.File
import no.novari.flyt.gateway.webinstance.model.instance.InstanceObject
import no.novari.instance.gateway.model.acos.AcosDocument
import no.novari.instance.gateway.model.acos.AcosInstance
import no.novari.instance.gateway.model.acos.AcosInstanceElement
import no.novari.instance.gateway.model.acos.AcosInstanceMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InstanceMapperTest {
    private lateinit var acosInstanceMapper: AcosInstanceMapper

    @Mock
    private lateinit var persistFile: (File) -> UUID

    @BeforeEach
    fun setup() {
        acosInstanceMapper = AcosInstanceMapper()
    }

    @Test
    fun shouldMapToInstance() {
        val acosInstance =
            AcosInstance(
                metadata =
                    AcosInstanceMetadata(
                        formId = "TEST0488",
                        instanceId = "100384",
                    ),
                formPdfBase64 = "formPdfBase64Value",
                elements =
                    listOf(
                        AcosInstanceElement(id = "Fornavn", value = "Ola"),
                        AcosInstanceElement(id = "Etternavn", value = "Nordmann"),
                        AcosInstanceElement(id = "Fornavn2", value = "Kari"),
                        AcosInstanceElement(id = "Etternavn2", value = "Ødegård"),
                        AcosInstanceElement(id = "Ukedag", value = null),
                        AcosInstanceElement(id = "Farge_pa_bil", value = "Grønn"),
                    ),
                documents =
                    listOf(
                        AcosDocument(
                            name = "vedleggImageNavn",
                            type = MediaType.IMAGE_JPEG,
                            encoding = null,
                            base64 = "vedleggImageBase64Value",
                        ),
                        AcosDocument(
                            name = "vedleggVideoNavn",
                            type = MediaType.parseMediaType("video/mp4"),
                            encoding = null,
                            base64 = "vedleggVideoBase64Value",
                        ),
                    ),
            )

        val expectedSkjemaPdfFile =
            File(
                name = "skjemaPdf",
                sourceApplicationId = 1L,
                sourceApplicationInstanceId = "100384",
                type = MediaType.APPLICATION_PDF,
                encoding = "UTF-8",
                base64Contents = "formPdfBase64Value",
            )

        val expectedVedleggImageFile =
            File(
                name = "vedleggImageNavn",
                sourceApplicationId = 1L,
                sourceApplicationInstanceId = "100384",
                type = MediaType.IMAGE_JPEG,
                encoding = "",
                base64Contents = "vedleggImageBase64Value",
            )

        val expectedVedleggVideoFile =
            File(
                name = "vedleggVideoNavn",
                sourceApplicationId = 1L,
                sourceApplicationInstanceId = "100384",
                type = MediaType.parseMediaType("video/mp4"),
                encoding = "",
                base64Contents = "vedleggVideoBase64Value",
            )

        whenever(persistFile(expectedSkjemaPdfFile))
            .thenReturn(UUID.fromString("391e9177-2790-469a-9f42-c8042731bc55"))
        whenever(persistFile(expectedVedleggImageFile))
            .thenReturn(UUID.fromString("dab3ecc8-2901-46f0-9553-2fbc3e71ae9e"))
        whenever(persistFile(expectedVedleggVideoFile))
            .thenReturn(UUID.fromString("5a15e2dd-29a7-41ac-a635-f4ab41d10d18"))

        val instance =
            acosInstanceMapper.map(
                sourceApplicationId = 1L,
                incomingInstance = acosInstance,
                persistFile = persistFile,
            )

        val expectedInstanceObject =
            InstanceObject(
                valuePerKey =
                    mapOf(
                        "skjema.Fornavn" to "Ola",
                        "skjema.Etternavn" to "Nordmann",
                        "skjema.Fornavn2" to "Kari",
                        "skjema.Etternavn2" to "Ødegård",
                        "skjema.Ukedag" to "",
                        "skjema.Farge_pa_bil" to "Grønn",
                        "skjemaPdf" to "391e9177-2790-469a-9f42-c8042731bc55",
                    ),
                objectCollectionPerKey =
                    mutableMapOf(
                        "vedlegg" to
                            listOf(
                                InstanceObject(
                                    valuePerKey =
                                        mapOf(
                                            "navn" to "vedleggImageNavn",
                                            "type" to "image/jpeg",
                                            "enkoding" to "",
                                            "fil" to "dab3ecc8-2901-46f0-9553-2fbc3e71ae9e",
                                        ),
                                ),
                                InstanceObject(
                                    valuePerKey =
                                        mapOf(
                                            "navn" to "vedleggVideoNavn",
                                            "type" to "video/mp4",
                                            "enkoding" to "",
                                            "fil" to "5a15e2dd-29a7-41ac-a635-f4ab41d10d18",
                                        ),
                                ),
                            ),
                    ),
            )

        assertThat(instance).isEqualTo(expectedInstanceObject)
    }
}

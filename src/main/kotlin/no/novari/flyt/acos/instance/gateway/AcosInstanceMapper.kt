package no.novari.flyt.acos.instance.gateway

import no.novari.flyt.acos.instance.gateway.model.acos.AcosDocument
import no.novari.flyt.acos.instance.gateway.model.acos.AcosInstance
import no.novari.flyt.acos.instance.gateway.model.acos.AcosInstanceElement
import no.novari.flyt.gateway.webinstance.InstanceMapper
import no.novari.flyt.gateway.webinstance.model.File
import no.novari.flyt.gateway.webinstance.model.instance.InstanceObject
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AcosInstanceMapper : InstanceMapper<AcosInstance> {
    override fun map(
        sourceApplicationId: Long,
        incomingInstance: AcosInstance,
        persistFile: (File) -> UUID,
    ): InstanceObject {
        val formPdfFileId = mapPdfFileToFileId(persistFile, sourceApplicationId, incomingInstance)
        val documentInstanceObjects =
            mapDocumentsToInstanceObjects(
                persistFile = persistFile,
                sourceApplicationId = sourceApplicationId,
                sourceApplicationInstanceId = incomingInstance.metadata.instanceId,
                acosDocuments = incomingInstance.documents,
            )

        return InstanceObject(
            valuePerKey = toValuePerKey(incomingInstance.elements, formPdfFileId),
            objectCollectionPerKey = mutableMapOf("vedlegg" to documentInstanceObjects),
        )
    }

    private fun toValuePerKey(
        acosInstanceElements: Collection<AcosInstanceElement>,
        formPdfFileId: UUID,
    ): Map<String, String> {
        val valuePerKey =
            acosInstanceElements
                .associate { acosInstanceElement ->
                    "skjema.${acosInstanceElement.id}" to acosInstanceElement.value.orEmpty()
                }.toMutableMap()

        valuePerKey["skjemaPdf"] = formPdfFileId.toString()

        return valuePerKey
    }

    private fun mapPdfFileToFileId(
        persistFile: (File) -> UUID,
        sourceApplicationId: Long,
        acosInstance: AcosInstance,
    ): UUID {
        return persistFile(
            File(
                name = "skjemaPdf",
                sourceApplicationId = sourceApplicationId,
                sourceApplicationInstanceId = acosInstance.metadata.instanceId,
                type = MediaType.APPLICATION_PDF,
                encoding = "UTF-8",
                base64Contents = acosInstance.formPdfBase64,
            ),
        )
    }

    private fun mapDocumentsToInstanceObjects(
        persistFile: (File) -> UUID,
        sourceApplicationId: Long,
        sourceApplicationInstanceId: String,
        acosDocuments: Collection<AcosDocument>,
    ): List<InstanceObject> {
        return acosDocuments.map { acosDocument ->
            mapDocumentToInstanceObject(
                persistFile = persistFile,
                sourceApplicationId = sourceApplicationId,
                sourceApplicationInstanceId = sourceApplicationInstanceId,
                acosDocument = acosDocument,
            )
        }
    }

    private fun mapDocumentToInstanceObject(
        persistFile: (File) -> UUID,
        sourceApplicationId: Long,
        sourceApplicationInstanceId: String,
        acosDocument: AcosDocument,
    ): InstanceObject {
        val file = toFile(sourceApplicationId, sourceApplicationInstanceId, acosDocument)
        val fileId = persistFile(file)
        return toInstanceObject(acosDocument, fileId)
    }

    private fun toFile(
        sourceApplicationId: Long,
        sourceApplicationInstanceId: String,
        acosDocument: AcosDocument,
    ): File {
        return File(
            name = acosDocument.name,
            sourceApplicationId = sourceApplicationId,
            sourceApplicationInstanceId = sourceApplicationInstanceId,
            type = acosDocument.type,
            encoding = acosDocument.encoding.orEmpty(),
            base64Contents = acosDocument.base64,
        )
    }

    private fun toInstanceObject(
        acosDocument: AcosDocument,
        fileId: UUID,
    ): InstanceObject {
        return InstanceObject(
            valuePerKey =
                mapOf(
                    "navn" to acosDocument.name,
                    "type" to acosDocument.type.toString(),
                    "enkoding" to acosDocument.encoding.orEmpty(),
                    "fil" to fileId.toString(),
                ),
        )
    }
}

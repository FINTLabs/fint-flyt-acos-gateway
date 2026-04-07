package no.novari.instance.gateway.model.caseinfo

import com.fasterxml.jackson.annotation.JsonProperty

data class CaseInfo(
    @field:JsonProperty("instanceId")
    val sourceApplicationInstanceId: String,
    val archiveCaseId: String?,
    val caseManager: CaseManager?,
    val administrativeUnit: AdministrativeUnit?,
    val status: CaseStatus?,
)

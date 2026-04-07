package no.novari.instance.gateway.caseinfo

import no.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.fint.model.resource.arkiv.kodeverk.SaksstatusResource
import no.fint.model.resource.arkiv.noark.AdministrativEnhetResource
import no.fint.model.resource.arkiv.noark.ArkivressursResource
import no.fint.model.resource.arkiv.noark.SakResource
import no.fint.model.resource.felles.PersonResource
import no.novari.cache.FintCache
import no.novari.instance.gateway.codelist.links.ResourceLinkUtil.getFirstLink
import no.novari.instance.gateway.model.caseinfo.AdministrativeUnit
import no.novari.instance.gateway.model.caseinfo.CaseInfo
import no.novari.instance.gateway.model.caseinfo.CaseManager
import no.novari.instance.gateway.model.caseinfo.CaseStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CaseInfoMappingService(
    private val saksstatusResourceCache: FintCache<String, SaksstatusResource>,
    private val administrativEnhetResourceCache: FintCache<String, AdministrativEnhetResource>,
    private val arkivressursResourceCache: FintCache<String, ArkivressursResource>,
    private val personalressursResourceCache: FintCache<String, PersonalressursResource>,
    private val personResourceCache: FintCache<String, PersonResource>,
) {
    fun toCaseInfo(
        sourceApplicationInstanceId: String,
        caseResource: SakResource,
    ): CaseInfo {
        return CaseInfo(
            sourceApplicationInstanceId = sourceApplicationInstanceId,
            archiveCaseId = caseResource.mappeId.identifikatorverdi,
            caseManager = getCaseManager(caseResource),
            administrativeUnit = getAdministrativeUnit(caseResource),
            status = getCaseStatus(caseResource),
        )
    }

    private fun getCaseManager(caseResource: SakResource): CaseManager? {
        return runCatching {
            val archiveResource =
                arkivressursResourceCache.get(
                    getFirstLink(
                        linkProducer = caseResource::getSaksansvarlig,
                        resource = caseResource,
                        linkedResourceName = "Saksansvarlig",
                    ),
                )
            val personalResource =
                personalressursResourceCache.get(
                    getFirstLink(
                        linkProducer = archiveResource::getPersonalressurs,
                        resource = archiveResource,
                        linkedResourceName = "Personalressurs",
                    ),
                )
            val personResource =
                personResourceCache.get(
                    getFirstLink(
                        linkProducer = personalResource::getPerson,
                        resource = personalResource,
                        linkedResourceName = "Person",
                    ),
                )

            CaseManager(
                firstName = personResource.navn.fornavn,
                middleName = personResource.navn.mellomnavn,
                lastName = personResource.navn.etternavn,
                email = personResource.kontaktinformasjon.epostadresse,
                phone = personResource.kontaktinformasjon.mobiltelefonnummer,
            )
        }.getOrElse { exception ->
            log.warn("No case manager for case with mappeId='{}'", caseResource.mappeId.identifikatorverdi, exception)
            null
        }
    }

    private fun getAdministrativeUnit(caseResource: SakResource): AdministrativeUnit? {
        return runCatching {
            val administrativeUnitResource =
                administrativEnhetResourceCache.get(
                    getFirstLink(
                        linkProducer = caseResource::getAdministrativEnhet,
                        resource = caseResource,
                        linkedResourceName = "AdministrativEnhet",
                    ),
                )
            AdministrativeUnit(name = administrativeUnitResource.navn)
        }.getOrElse { exception ->
            log.warn(
                "No administrative unit for case with mappeId='{}'",
                caseResource.mappeId.identifikatorverdi,
                exception,
            )
            null
        }
    }

    private fun getCaseStatus(caseResource: SakResource): CaseStatus? {
        return runCatching {
            val caseStatusResource =
                saksstatusResourceCache.get(
                    getFirstLink(
                        linkProducer = caseResource::getSaksstatus,
                        resource = caseResource,
                        linkedResourceName = "Saksstatus",
                    ),
                )
            CaseStatus(
                name = caseStatusResource.navn,
                code = caseStatusResource.kode,
            )
        }.getOrElse { exception ->
            log.warn("No status for case with mappeId='{}'", caseResource.mappeId.identifikatorverdi, exception)
            null
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(CaseInfoMappingService::class.java)
    }
}

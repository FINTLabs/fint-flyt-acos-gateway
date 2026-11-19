package no.novari.flyt.acos.instance.gateway

import no.novari.fint.model.resource.arkiv.noark.SakResource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * Client for the internal client API in fint-flyt-archive-gateway.
 *
 * Returns `null` on 404.
 */
@Service
class ArchiveGatewayClient(
    @param:Qualifier("archiveGatewayRestClient")
    private val restClient: RestClient,
) {
    fun getCaseWithFilteredJournalposts(archiveInstanceId: String): SakResource? =
        try {
            restClient
                .get()
                .uri { builder ->
                    builder
                        .path("/arkiv/saker")
                        .queryParam("archiveInstanceId", "{archiveInstanceId}")
                        .build(archiveInstanceId)
                }.retrieve()
                .body<SakResource>()
        } catch (_: HttpClientErrorException.NotFound) {
            null
        }
}

package no.novari.flyt.acos.instance.gateway.codelist

import no.novari.fint.model.resource.FintLinks
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.arkiv.kodeverk.SaksstatusResource
import no.novari.fint.model.resource.arkiv.noark.AdministrativEnhetResource
import no.novari.fint.model.resource.arkiv.noark.ArkivressursResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.flyt.acos.instance.gateway.codelist.links.ResourceLinkUtil
import no.novari.flyt.cache.FlytCache
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload

@Configuration
class ResourceEntityConsumersConfiguration(
    private val administrativEnhetResourceCache: FlytCache<String, AdministrativEnhetResource>,
    private val arkivressursResourceCache: FlytCache<String, ArkivressursResource>,
    private val saksstatusResourceCache: FlytCache<String, SaksstatusResource>,
    private val personalressursResourceCache: FlytCache<String, PersonalressursResource>,
    private val personResourceCache: FlytCache<String, PersonResource>,
) : AbstractConsumerSeekAware() {
    @KafkaListener(topics = ["#{@administrativEnhetResourceTopic.name}"])
    fun onAdministrativEnhetResource(
        @Payload(required = false) resource: AdministrativEnhetResource?,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
    ) = administrativEnhetResourceCache.update(key, resource)

    @KafkaListener(topics = ["#{@arkivressursResourceTopic.name}"])
    fun onArkivressursResource(
        @Payload(required = false) resource: ArkivressursResource?,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
    ) = arkivressursResourceCache.update(key, resource)

    @KafkaListener(topics = ["#{@saksstatusResourceTopic.name}"])
    fun onSaksstatusResource(
        @Payload(required = false) resource: SaksstatusResource?,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
    ) = saksstatusResourceCache.update(key, resource)

    @KafkaListener(topics = ["#{@personalressursResourceTopic.name}"])
    fun onPersonalressursResource(
        @Payload(required = false) resource: PersonalressursResource?,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
    ) = personalressursResourceCache.update(key, resource)

    @KafkaListener(topics = ["#{@personResourceTopic.name}"])
    fun onPersonResource(
        @Payload(required = false) resource: PersonResource?,
        @Header(KafkaHeaders.RECEIVED_KEY, required = false) key: String?,
    ) = personResourceCache.update(key, resource)

    /**
     * A resource is cached under every self link. A tombstone removes all of them, found through the cached resource
     * under the record key (archive-gateway keys records with the first self link).
     */
    private fun <R : FintLinks> FlytCache<String, R>.update(
        key: String?,
        resource: R?,
    ) {
        if (resource != null) {
            put(ResourceLinkUtil.getSelfLinks(resource), resource)
        } else if (key != null) {
            getOrNull(key)?.let { remove(ResourceLinkUtil.getSelfLinks(it)) }
            remove(key)
        }
    }

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        super.onPartitionsAssigned(assignments, callback)
        callback.seekToBeginning(assignments.keys)
    }
}

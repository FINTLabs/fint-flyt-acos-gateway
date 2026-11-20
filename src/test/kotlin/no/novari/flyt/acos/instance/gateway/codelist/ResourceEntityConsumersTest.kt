package no.novari.flyt.acos.instance.gateway.codelist

import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.arkiv.kodeverk.SaksstatusResource
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.kafka.listener.ConsumerSeekAware
import tools.jackson.module.kotlin.jacksonObjectMapper

class ResourceEntityConsumersTest {
    private val cache = ResourceEntityCacheConfiguration().saksstatusResourceCache()
    private val listeners = ResourceEntityConsumersConfiguration(mock(), mock(), cache, mock(), mock())

    @Test
    fun `Jackson 3 resource is cached by every self link and tombstone removes its key`() {
        val original =
            SaksstatusResource().apply {
                addSelf(Link.with("https://fint.test/status/1"))
                addSelf(Link.with("https://fint.test/status/B"))
            }
        val mapper = jacksonObjectMapper()
        val resource = mapper.readValue(mapper.writeValueAsString(original), SaksstatusResource::class.java)
        listeners.onSaksstatusResource(resource, "https://fint.test/status/1")
        assertThat(cache.get("https://fint.test/status/1")).isSameAs(resource)
        assertThat(cache.get("https://fint.test/status/B")).isSameAs(resource)

        listeners.onSaksstatusResource(null, "https://fint.test/status/1")
        assertThat(cache.getOrNull("https://fint.test/status/1")).isNull()
    }

    @Test
    fun `assignment rebuilds caches from the beginning`() {
        val callback = mock<ConsumerSeekAware.ConsumerSeekCallback>()
        val assignments = mapOf(TopicPartition("fintlabs-no.flyt.entity.arkiv-kodeverk-saksstatus", 0) to 42L)
        listeners.onPartitionsAssigned(assignments, callback)
        verify(callback).seekToBeginning(assignments.keys)
    }
}

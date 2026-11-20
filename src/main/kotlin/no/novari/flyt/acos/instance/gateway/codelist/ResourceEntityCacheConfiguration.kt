package no.novari.flyt.acos.instance.gateway.codelist

import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.arkiv.kodeverk.SaksstatusResource
import no.novari.fint.model.resource.arkiv.noark.AdministrativEnhetResource
import no.novari.fint.model.resource.arkiv.noark.ArkivressursResource
import no.novari.fint.model.resource.felles.PersonResource
import no.novari.flyt.cache.FlytCache
import no.novari.kafka.starter.topic.KafkaTopics
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff
import java.util.Locale

@Configuration
class ResourceEntityCacheConfiguration {
    @Bean
    fun kafkaErrorHandler(): CommonErrorHandler = DefaultErrorHandler(FixedBackOff(0, 0))

    @Bean
    fun administrativEnhetResourceTopic(topics: KafkaTopics) = topics.entity("arkiv-noark-administrativenhet")

    @Bean
    fun arkivressursResourceTopic(topics: KafkaTopics) = topics.entity("arkiv-noark-arkivressurs")

    @Bean
    fun saksstatusResourceTopic(topics: KafkaTopics) = topics.entity("arkiv-kodeverk-saksstatus")

    @Bean
    fun personalressursResourceTopic(topics: KafkaTopics) = topics.entity("administrasjon-personal-personalressurs")

    @Bean
    fun personResourceTopic(topics: KafkaTopics) = topics.entity("administrasjon-personal-person")

    @Bean
    fun administrativEnhetResourceCache(): FlytCache<String, AdministrativEnhetResource> {
        return createCache(AdministrativEnhetResource::class.java)
    }

    @Bean
    fun arkivressursResourceCache(): FlytCache<String, ArkivressursResource> {
        return createCache(ArkivressursResource::class.java)
    }

    @Bean
    fun saksstatusResourceCache(): FlytCache<String, SaksstatusResource> {
        return createCache(SaksstatusResource::class.java)
    }

    @Bean
    fun personalressursResourceCache(): FlytCache<String, PersonalressursResource> {
        return createCache(PersonalressursResource::class.java)
    }

    @Bean
    fun personResourceCache(): FlytCache<String, PersonResource> {
        return createCache(PersonResource::class.java)
    }

    private fun <V : Any> createCache(resourceClass: Class<V>): FlytCache<String, V> {
        return FlytCache(resourceClass.name.lowercase(Locale.ROOT))
    }
}

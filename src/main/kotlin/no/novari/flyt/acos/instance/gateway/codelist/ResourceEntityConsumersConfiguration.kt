package no.novari.flyt.acos.instance.gateway.codelist

import no.fint.model.resource.FintLinks
import no.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.fint.model.resource.arkiv.kodeverk.SaksstatusResource
import no.fint.model.resource.arkiv.noark.AdministrativEnhetResource
import no.fint.model.resource.arkiv.noark.ArkivressursResource
import no.fint.model.resource.felles.PersonResource
import no.novari.cache.FintCache
import no.novari.flyt.acos.instance.gateway.codelist.links.ResourceLinkUtil
import no.novari.kafka.consuming.ErrorHandlerConfiguration
import no.novari.kafka.consuming.ErrorHandlerFactory
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.consuming.ParameterizedListenerContainerFactoryService
import no.novari.kafka.topic.name.EntityTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer

@Configuration
class ResourceEntityConsumersConfiguration(
    private val listenerFactoryService: ParameterizedListenerContainerFactoryService,
    private val errorHandlerFactory: ErrorHandlerFactory,
) {
    private fun <T : FintLinks> createCacheConsumer(
        resourceName: String,
        resourceClass: Class<T>,
        cache: FintCache<String, T>,
    ): ConcurrentMessageListenerContainer<String, T> {
        val listenerConfiguration =
            ListenerConfiguration
                .stepBuilder()
                .groupIdApplicationDefault()
                .maxPollRecordsKafkaDefault()
                .maxPollIntervalKafkaDefault()
                .seekToBeginningOnAssignment()
                .build()

        val errorHandler =
            errorHandlerFactory.createErrorHandler(
                ErrorHandlerConfiguration
                    .stepBuilder<T>()
                    .noRetries()
                    .skipFailedRecords()
                    .build(),
            )

        return listenerFactoryService
            .createRecordListenerContainerFactory(
                resourceClass,
                { record ->
                    val value = record.value()
                    cache.put(ResourceLinkUtil.getSelfLinks(value), value)
                },
                listenerConfiguration,
                errorHandler,
            ).createContainer(
                EntityTopicNameParameters
                    .builder()
                    .topicNamePrefixParameters(
                        TopicNamePrefixParameters
                            .stepBuilder()
                            .orgIdApplicationDefault()
                            .domainContextApplicationDefault()
                            .build(),
                    ).resourceName(resourceName)
                    .build(),
            )
    }

    @Bean
    fun administrativEnhetResourceEntityConsumer(
        administrativEnhetResourceCache: FintCache<String, AdministrativEnhetResource>,
    ): ConcurrentMessageListenerContainer<String, AdministrativEnhetResource> {
        return createCacheConsumer(
            resourceName = "arkiv-noark-administrativenhet",
            resourceClass = AdministrativEnhetResource::class.java,
            cache = administrativEnhetResourceCache,
        )
    }

    @Bean
    fun arkivressursResourceEntityConsumer(
        arkivressursResourceCache: FintCache<String, ArkivressursResource>,
    ): ConcurrentMessageListenerContainer<String, ArkivressursResource> {
        return createCacheConsumer(
            resourceName = "arkiv-noark-arkivressurs",
            resourceClass = ArkivressursResource::class.java,
            cache = arkivressursResourceCache,
        )
    }

    @Bean
    fun saksstatusResourceEntityConsumer(
        saksstatusResourceCache: FintCache<String, SaksstatusResource>,
    ): ConcurrentMessageListenerContainer<String, SaksstatusResource> {
        return createCacheConsumer(
            resourceName = "arkiv-kodeverk-saksstatus",
            resourceClass = SaksstatusResource::class.java,
            cache = saksstatusResourceCache,
        )
    }

    @Bean
    fun personalressursResourceEntityConsumer(
        personalressursResourceCache: FintCache<String, PersonalressursResource>,
    ): ConcurrentMessageListenerContainer<String, PersonalressursResource> {
        return createCacheConsumer(
            resourceName = "administrasjon-personal-personalressurs",
            resourceClass = PersonalressursResource::class.java,
            cache = personalressursResourceCache,
        )
    }

    @Bean
    fun personResourceEntityConsumer(
        personResourceCache: FintCache<String, PersonResource>,
    ): ConcurrentMessageListenerContainer<String, PersonResource> {
        return createCacheConsumer(
            resourceName = "administrasjon-personal-person",
            resourceClass = PersonResource::class.java,
            cache = personResourceCache,
        )
    }
}

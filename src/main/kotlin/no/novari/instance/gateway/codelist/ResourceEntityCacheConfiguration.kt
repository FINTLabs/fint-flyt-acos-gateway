package no.novari.instance.gateway.codelist

import no.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.fint.model.resource.arkiv.kodeverk.SaksstatusResource
import no.fint.model.resource.arkiv.noark.AdministrativEnhetResource
import no.fint.model.resource.arkiv.noark.ArkivressursResource
import no.fint.model.resource.felles.PersonResource
import no.novari.cache.FintCache
import no.novari.cache.FintCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Locale

@Configuration
class ResourceEntityCacheConfiguration(
    private val fintCacheManager: FintCacheManager,
) {
    @Bean
    fun administrativEnhetResourceCache(): FintCache<String, AdministrativEnhetResource> {
        return createCache(AdministrativEnhetResource::class.java)
    }

    @Bean
    fun arkivressursResourceCache(): FintCache<String, ArkivressursResource> {
        return createCache(ArkivressursResource::class.java)
    }

    @Bean
    fun saksstatusResourceCache(): FintCache<String, SaksstatusResource> {
        return createCache(SaksstatusResource::class.java)
    }

    @Bean
    fun personalressursResourceCache(): FintCache<String, PersonalressursResource> {
        return createCache(PersonalressursResource::class.java)
    }

    @Bean
    fun personResourceCache(): FintCache<String, PersonResource> {
        return createCache(PersonResource::class.java)
    }

    private fun <V> createCache(resourceClass: Class<V>): FintCache<String, V> {
        return fintCacheManager.createCache(
            resourceClass.name.lowercase(Locale.ROOT),
            String::class.java,
            resourceClass,
        )
    }
}

package no.novari.instance.gateway

import no.novari.flyt.gateway.webinstance.InstanceProcessor
import no.novari.flyt.gateway.webinstance.InstanceProcessorFactoryService
import no.novari.instance.gateway.model.acos.AcosInstance
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InstanceProcessorConfiguration {
    @Bean
    fun instanceProcessor(
        instanceProcessorFactoryService: InstanceProcessorFactoryService,
        acosInstanceMapper: AcosInstanceMapper,
    ): InstanceProcessor<AcosInstance> {
        return instanceProcessorFactoryService.createInstanceProcessor(
            sourceApplicationIntegrationIdFunction = { acosInstance -> acosInstance.metadata.formId },
            sourceApplicationInstanceIdFunction = { acosInstance -> acosInstance.metadata.instanceId },
            instanceMapper = acosInstanceMapper,
        )
    }
}

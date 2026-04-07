package no.novari.instance.gateway

import no.fint.model.resource.arkiv.noark.SakResource
import no.novari.kafka.consuming.ListenerConfiguration
import no.novari.kafka.requestreply.RequestProducerRecord
import no.novari.kafka.requestreply.RequestTemplate
import no.novari.kafka.requestreply.RequestTemplateFactory
import no.novari.kafka.requestreply.topic.ReplyTopicService
import no.novari.kafka.requestreply.topic.configuration.ReplyTopicConfiguration
import no.novari.kafka.requestreply.topic.name.ReplyTopicNameParameters
import no.novari.kafka.requestreply.topic.name.RequestTopicNameParameters
import no.novari.kafka.topic.name.TopicNamePrefixParameters
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class ArchiveCaseRequestService(
    @Value("\${novari.kafka.application-id}") applicationId: String,
    replyTopicService: ReplyTopicService,
    requestTemplateFactory: RequestTemplateFactory,
) {
    private val requestTopicNameParameters =
        RequestTopicNameParameters
            .builder()
            .topicNamePrefixParameters(
                TopicNamePrefixParameters
                    .stepBuilder()
                    .orgIdApplicationDefault()
                    .domainContextApplicationDefault()
                    .build(),
            ).resourceName(TOPIC_NAME)
            .parameterName("archive-instance-id")
            .build()

    private val requestTemplate: RequestTemplate<String, SakResource>

    init {
        val replyTopicNameParameters =
            ReplyTopicNameParameters
                .builder()
                .topicNamePrefixParameters(
                    TopicNamePrefixParameters
                        .stepBuilder()
                        .orgIdApplicationDefault()
                        .domainContextApplicationDefault()
                        .build(),
                ).applicationId(applicationId)
                .resourceName(TOPIC_NAME)
                .build()

        replyTopicService.createOrModifyTopic(
            replyTopicNameParameters,
            ReplyTopicConfiguration
                .builder()
                .retentionTime(RETENTION_TIME)
                .build(),
        )

        requestTemplate =
            requestTemplateFactory.createTemplate(
                replyTopicNameParameters,
                String::class.java,
                SakResource::class.java,
                REPLY_TIMEOUT,
                ListenerConfiguration
                    .stepBuilder()
                    .groupIdApplicationDefault()
                    .maxPollRecordsKafkaDefault()
                    .maxPollIntervalKafkaDefault()
                    .continueFromPreviousOffsetOnAssignment()
                    .build(),
            )
    }

    fun getByArchiveCaseId(archiveCaseId: String): SakResource? {
        return requestTemplate
            .requestAndReceive(
                RequestProducerRecord
                    .builder<String>()
                    .topicNameParameters(requestTopicNameParameters)
                    .value(archiveCaseId)
                    .build(),
            ).value()
    }

    private companion object {
        private const val TOPIC_NAME = "arkiv-noark-sak-with-filtered-journalposts"
        private val RETENTION_TIME: Duration = Duration.ofMinutes(10)
        private val REPLY_TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}

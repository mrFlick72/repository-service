package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import com.fasterxml.jackson.databind.ObjectMapper
import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.TimeStamp
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient


class DocumentUpdateEventSender(private val objectMapper: ObjectMapper,
                                private val sqsAsyncClient: SqsAsyncClient,
                                private val applicationStorageRepository: ApplicationStorageRepository) {
    fun publishEventFor(event: StorageUpdateEvent): Mono<Unit> =
            applicationStorageRepository.storageConfigurationFor(event.application)
                    .flatMap { config -> config.updateSignals }
                    .map { updateSignals ->
                        Mono.fromCompletionStage(
                                sqsAsyncClient.sendMessage {
                                    it.messageBody(objectMapper.writeValueAsString(event))
                                            .queueUrl(updateSignals.sqsQueue)
                                }
                        ).flatMap { Mono.just(Unit) }
                    }.orElse(Mono.just(Unit))
}

data class StorageUpdateEvent(val application: Application,
                              val path: Path,
                              val fileName: FileName,
                              val updateTimesTamp: TimeStamp)

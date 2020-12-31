package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import com.fasterxml.jackson.databind.ObjectMapper
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationName
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.TimeStamp
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.ZoneOffset


class DocumentUpdateEventSender(private val objectMapper: ObjectMapper,
                                private val sqsAsyncClient: SqsAsyncClient,
                                private val applicationRepository: ApplicationRepository) {
    fun publishEventFor(event: StorageUpdateEvent): Mono<Unit> =
            applicationRepository.findApplicationFor(event.applicationName)
                    .flatMap { config -> config.updateSignals }
                    .map { updateSignals ->
                        Mono.fromCompletionStage(
                                sqsAsyncClient.sendMessage {
                                    it.messageBody(objectMapper.writeValueAsString(event.toRepresentation()))
                                            .queueUrl(updateSignals.sqsQueue)
                                }
                        ).flatMap { Mono.just(Unit) }
                    }.orElse(Mono.just(Unit))
}

data class StorageUpdateEvent(val applicationName: ApplicationName,
                              val path: Path,
                              val fileName: FileName,
                              val updateTimesTamp: TimeStamp) {
    fun toRepresentation() = mapOf(
            "applicationName" to this.applicationName.value,
            "path" to this.path.value,
            "fileName" to "${this.fileName.name}.${this.fileName.extension}",
            "updateTimesTamp" to this.updateTimesTamp.localDateTime.toEpochSecond(ZoneOffset.UTC)
    )

}

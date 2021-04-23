package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import com.jayway.jsonpath.JsonPath
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document.Companion.emptyDocumentFrom
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.SaveDocumentRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3MetadataRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.Clock
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.DefaultApplicationArguments
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.fromCompletionStage
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.Message
import java.time.Duration
import java.util.*

class StorageUpdateEventsListener(
    private val clock: Clock,
    private val documentUpdateEventSender: DocumentUpdateEventSender,
    private val applicationRepository: ApplicationRepository,
    private val s3MetadataRepository: S3MetadataRepository,
    private val saveDocumentRepository: SaveDocumentRepository,
    private val sqsAsyncClient: SqsAsyncClient,
    private val factory: ReceiveMessageRequestFactory,
    private val duration: Duration
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(StorageUpdateEventsListener::class.java)

    fun listen() =
        Flux.interval(duration)
            .flatMap { fetchMessages() }
            .flatMap(this::objectMetadataFrom)
            .flatMap(this::updateIndexOnEsFrom)
            .flatMap { document ->
                documentUpdateEventSender.publishEventFor(
                    StorageUpdateEvent(
                        document.application.applicationName,
                        document.path,
                        document.fileContent.fileName,
                        clock.now()
                    )
                )
            }.doOnComplete { logger.info("subscription completed") }
            .doOnCancel { logger.info("subscription cancelled") }
            .doOnSubscribe { logger.info("subscription started") }
            .doOnError { e -> logger.error("subscription error: ", e) };

    override fun run(args: ApplicationArguments) {
        listen().doOnError(Exception::class.java) { e: Any ->
            try {
                this.run(DefaultApplicationArguments())
            } catch (exception: Exception) {
                logger.error("subscription error: ", e)
                logger.error("going to resubscribe again ")
            }
        }.subscribe()
    }


    private fun fetchMessages(): Flux<Map<String, String>> =
        Flux.from(fromCompletionStage(sqsAsyncClient.receiveMessage(factory.makeAReceiveMessageRequest())))
            .flatMap { response -> Flux.fromIterable(response.messages()) }
            .flatMap { message -> purgeProcessedMessagesFor(message) }

    private fun purgeProcessedMessagesFor(message: Message) =
        fromCompletionStage(sqsAsyncClient.deleteMessage(factory.makeADeleteMessageRequest(message.receiptHandle())))
            .thenMany(objectDetailsFrom(message))

    private fun objectDetailsFrom(message: Message): Flux<Map<String, String>> =
        Flux.defer { Flux.just(JsonPath.parse(message.body())) }
            .flatMap {
                Flux.zip(
                    Flux.fromIterable(it.read("\$..bucket.name", List::class.java)),
                    Flux.fromIterable(it.read("\$..object.key", List::class.java))
                )
            }.flatMap { Flux.just(mapOf("bucket" to it.t1.toString(), "key" to it.t2.toString())) }
            .onErrorResume {
                logger.error(it.message, it)
                Mono.empty()
            }


    private fun objectMetadataFrom(metadata: Map<String, String>): Mono<DocumentMetadata> =
        s3MetadataRepository.objectMetadataFor(
            bucketNameFrom(metadata),
            objectKeyFrom(metadata),
        ).onErrorResume {
            logger.error(it.message, it)
            Mono.empty()
        }


    private fun objectKeyFrom(metadata: Map<String, String>) = valueFrom(metadata, "key")
    private fun bucketNameFrom(metadata: Map<String, String>) = valueFrom(metadata, "bucket")

    private fun valueFrom(metadata: Map<String, String>, key: String) =
        Optional.ofNullable(metadata).map { it.getOrDefault(key, "") }.orElse("")

    private fun updateIndexOnEsFrom(documentMetadata: DocumentMetadata) =
        applicationRepository.findApplicationFor(Storage(bucketNameFrom(documentMetadata.content)))
            .map {
                saveDocumentRepository.save(emptyDocumentFrom(it, documentMetadata))
                    .then(Mono.defer { Mono.just(emptyDocumentFrom(it, documentMetadata)) })
            }
            .orElse(Mono.error(RuntimeException("application not found for application metadata ${documentMetadata.content}")))
            .onErrorResume { Mono.empty() }

}
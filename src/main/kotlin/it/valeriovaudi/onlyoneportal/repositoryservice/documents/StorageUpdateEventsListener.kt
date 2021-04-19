package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import com.jayway.jsonpath.JsonPath
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3MetadataRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono.fromCompletionStage
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.time.Duration

class StorageUpdateEventsListener(
    private val s3MetadataRepository: S3MetadataRepository,
    private val sqsAsyncClient: SqsAsyncClient,
    private val factory: ReceiveMessageRequestFactory,
    private val duration: Duration
) : ApplicationRunner {

    fun listen() =
        Flux.interval(duration)
            .flatMap { handleMessage() }
            .log()
            .flatMap { metadata ->
                val objectMetadataFor = s3MetadataRepository.objectMetadataFor(
                    metadata["bucket"]!!,
                    metadata["key"]!!
                )
                objectMetadataFor
            }


    private fun handleMessage(): Flux<Map<String, String>> =
        Flux.from(fromCompletionStage(sqsAsyncClient.receiveMessage(factory.makeAReceiveMessageRequest())))
            .flatMap { response -> Flux.fromIterable(response.messages()) }
            .flatMap { message -> purgeProcessedMessagesFor(message) }
            .log()

    private fun purgeProcessedMessagesFor(message: Message) =
        fromCompletionStage(sqsAsyncClient.deleteMessage(factory.makeADeleteMessageRequest(message.receiptHandle())))
            .thenMany(objectDetailsFrom(message))

    private fun objectDetailsFrom(message: Message): Flux<Map<String, String>> = Flux.defer {
        val parse = JsonPath.parse(message.body())
        Flux.zip(
            Flux.fromIterable(parse.read("\$..bucket.name", List::class.java)),
            Flux.fromIterable(parse.read("\$..object.key", List::class.java))
        ).flatMap { Flux.just(mapOf<String, String>("bucket" to it.t1.toString(), "key" to it.t2.toString())) }
    }


    override fun run(args: ApplicationArguments?) {
        listen().subscribe(System.out::println)
    }

}

class ReceiveMessageRequestFactory(
    private val queueUrl: String,
    private val maxNumberOfMessages: Int,
    private val visibilityTimeout: Int,
    private val waitTimeSeconds: Int
) {

    fun makeAReceiveMessageRequest(): ReceiveMessageRequest {
        return ReceiveMessageRequest.builder()
            .maxNumberOfMessages(maxNumberOfMessages)
            .visibilityTimeout(visibilityTimeout)
            .waitTimeSeconds(waitTimeSeconds)
            .queueUrl(queueUrl)
            .build()
    }

    fun makeADeleteMessageRequest(receiptHandle: String): DeleteMessageRequest {
        return DeleteMessageRequest.builder()
            .receiptHandle(receiptHandle)
            .queueUrl(queueUrl)
            .build()
    }
}
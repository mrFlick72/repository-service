package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono.fromCompletionStage
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import java.time.Duration

class StorageUpdateEventsListener(private val sqsAsyncClient: SqsAsyncClient,
                                  private val factory: ReceiveMessageRequestFactory,
                                  private val duration: Duration) : ApplicationRunner {

    fun listen() =
            Flux.interval(duration)
                    .flatMap { handleMessage() }


    private fun handleMessage() =
            Flux.from(fromCompletionStage(sqsAsyncClient.receiveMessage(factory.makeAReceiveMessageRequest())))
                    .flatMap { response -> Flux.fromIterable(response.messages()) }
                    .flatMap { message ->
                        fromCompletionStage(sqsAsyncClient.deleteMessage(factory.makeADeleteMessageRequest(message.receiptHandle())))
                                .thenMany(Flux.just(message.body()))
                    }.log()

    override fun run(args: ApplicationArguments?) {
        listen().subscribe(System.out::println)
    }

}

class ReceiveMessageRequestFactory(private val queueUrl: String,
                                   private val maxNumberOfMessages: Int,
                                   private val visibilityTimeout: Int,
                                   private val waitTimeSeconds: Int) {

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
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

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
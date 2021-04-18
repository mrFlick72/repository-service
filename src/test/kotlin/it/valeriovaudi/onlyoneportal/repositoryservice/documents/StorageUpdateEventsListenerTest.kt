package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.Duration

import ch.qos.logback.classic.Level;
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.aFakeDocument
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.aFakeDocumentWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.applicationWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.randomizer
import reactor.test.StepVerifier

internal class StorageUpdateEventsListenerTest {

    private val bucket = System.getenv("AWS_TESTING_S3_APPLICATION_STORAGE")
    private val queueUrl = System.getenv("AWS_TESTING_SQS_STORAGE_REINDEX_QUEUE")

    private val sqsClient: SqsAsyncClient =
            SqsAsyncClient.builder()
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build()


    private val s3Client: S3AsyncClient =
            S3AsyncClient.builder()
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build()

    private val s3Repository = S3Repository(s3Client)

    @BeforeEach
    fun changeLogLevel() {
        val rootLogger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = Level.INFO
    }

    @Test
    internal fun `when a message is received`() {

        StepVerifier.create(s3Repository.saveDocumentFor(aFakeDocumentWith(randomizer, applicationWith(Storage(bucket)))))
                .expectNext(Unit)
                .verifyComplete()

        val storageUpdateEventsListener =
                StorageUpdateEventsListener(
                        sqsClient,
                        ReceiveMessageRequestFactory(queueUrl,
                                10, 10, 10),
                        Duration.ofSeconds(1)
                )
        println(
                storageUpdateEventsListener.listen()
                        .blockFirst(Duration.ofSeconds(60))
        )
    }
}
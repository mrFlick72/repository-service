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

internal class StorageUpdateEventsListenerTest {


    fun sqsClient(): SqsAsyncClient =
            SqsAsyncClient.builder()
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build()


    fun s3Client(): S3AsyncClient =
            S3AsyncClient.builder()
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .build()

    @BeforeEach
    fun changeLogLevel() {
        val rootLogger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.setLevel(Level.INFO)
    }

    @Test
    internal fun `when a message is received`() {
        val s3Repository = S3Repository(s3Client())
//        s3Repository.saveDocumentFor(Document())
        val storageUpdateEventsListener =
                StorageUpdateEventsListener(
                        sqsClient(),
                        ReceiveMessageRequestFactory(System.getenv("AWS_TESTING_SQS_STORAGE_REINDEX_QUEUE"),
                                10, 10, 10),
                        Duration.ofSeconds(1)
                )

        storageUpdateEventsListener.listen()
                .subscribe(System.out::println)

        Thread.sleep(100000L)
        /*
        StepVerifier.create(storageUpdateEventsListener.listen())
                .expectNext()
                .expectComplete()
                .verify()
*/
    }
}
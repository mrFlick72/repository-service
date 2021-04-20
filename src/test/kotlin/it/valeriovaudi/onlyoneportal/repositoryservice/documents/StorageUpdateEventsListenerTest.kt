package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import ch.qos.logback.classic.Level
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.`updates a document on s3 with metadata from`
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.aFakeDocumentWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.applicationWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.bucket
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.queueUrl
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.randomizer
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.DocumentEsIdGenerator
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.SaveDocumentRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3MetadataRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.Duration

internal class StorageUpdateEventsListenerTest {

    private lateinit var s3MetadataRepository : S3MetadataRepository

    private val sqsClient: SqsAsyncClient =
        SqsAsyncClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()

    private fun template(): ReactiveElasticsearchTemplate =
        ReactiveElasticsearchTemplate(
            ReactiveRestClients.create(
                ClientConfiguration.builder().connectedTo("localhost:39200").build()
            )
        )

    private val reactiveElasticsearchTemplate = template()
    private val idGenerator = DocumentEsIdGenerator()

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
        val document = aFakeDocumentWith(
            randomizer,
            applicationWith(Storage(bucket))
        )

        `updates a document on s3 with metadata from`(document, s3Repository)

        val storageUpdateEventsListener =
            StorageUpdateEventsListener(
                s3MetadataRepository,
                SaveDocumentRepository(reactiveElasticsearchTemplate, idGenerator),
                sqsClient,
                ReceiveMessageRequestFactory(
                    queueUrl,
                    10, 10, 10
                ),
                Duration.ofSeconds(1)
            )

/*        StepVerifier.create(storageUpdateEventsListener.listen())
            .expectNext(mapOf("bucket" to bucket, "key" to "a_path/a_file.jpg"))
            .expectComplete()
            .verify()*/

        val message = storageUpdateEventsListener.listen()
            .blockFirst(Duration.ofSeconds(60))
        println(
            message
        )
    }

}
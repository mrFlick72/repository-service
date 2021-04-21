package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import ch.qos.logback.classic.Level
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationStorageFeature
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.application.YamlApplicationRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.application.YamlApplicationStorageMapping
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.`updates a document on s3 with metadata from`
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.aFakeDocumentWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.applicationWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.bucket
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.objectKey
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.queueUrl
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.SaveDocumentRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3MetadataRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.Duration

@ExtendWith(MockKExtension::class)
internal class StorageUpdateEventsListenerTest {

    @MockK
    private lateinit var s3MetadataRepository: S3MetadataRepository

    @MockK
    private lateinit var saveDocumentRepository: SaveDocumentRepository

    val applicationRepository =
        YamlApplicationRepository(YamlApplicationStorageMapping(mapOf("an_app" to ApplicationStorageFeature(bucket))))

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
        val randomizerValue = "randomizerValue"
        val document = aFakeDocumentWith(randomizerValue, applicationWith(Storage(bucket)))

        `updates a document on s3 with metadata from`(document, s3Repository)

        every { s3MetadataRepository.objectMetadataFor(bucket, objectKey) }
            .returns(Mono.just(DocumentFixture.documentMetadata(randomizerValue)))

        every { saveDocumentRepository.save(document) }
            .returns(Mono.just(Unit))

        val storageUpdateEventsListener =
            StorageUpdateEventsListener(
                applicationRepository,
                s3MetadataRepository,
                saveDocumentRepository,
                sqsClient,
                ReceiveMessageRequestFactory(
                    queueUrl,
                    10, 100, 10
                ),
                Duration.ofSeconds(1)
            )

        storageUpdateEventsListener.listen()
            .blockFirst(Duration.ofMinutes(1))

        verify { s3MetadataRepository.objectMetadataFor(bucket, objectKey) }
        verify { saveDocumentRepository.save(document) }
    }

}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import ch.qos.logback.classic.Level
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import it.valeriovaudi.onlyoneportal.repositoryservice.application.*
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.`updates a document on s3 with metadata from`
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.aFakeDocumentWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.applicationWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.bucket
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.objectKey
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.queueUrl
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.storageUpdateEventWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.SaveDocumentRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3MetadataRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.Clock
import it.valeriovaudi.onlyoneportal.repositoryservice.time.TimeStamp
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

    @MockK
    private lateinit var documentUpdateEventSender: DocumentUpdateEventSender

    @MockK
    private lateinit var clock: Clock

    private val applicationRepository =
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
        val updateTimesTamp = TimeStamp.now()
        val updateEvent = storageUpdateEventWith(updateTimesTamp)

        `updates a document on s3 with metadata from`(document, s3Repository)

        every { s3MetadataRepository.objectMetadataFor(bucket, objectKey) }
            .returns(Mono.just(DocumentFixture.documentMetadata(randomizerValue)))

        every { saveDocumentRepository.save(document) }
            .returns(Mono.just(Unit))

        every { documentUpdateEventSender.publishEventFor(updateEvent) }
            .returns(Mono.just(Unit))

        every { clock.now() }
            .returns(updateTimesTamp)

        val storageUpdateEventsListener =
            StorageUpdateEventsListener(
                clock,
                documentUpdateEventSender,
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

        verify(exactly = 1)  { s3MetadataRepository.objectMetadataFor(bucket, objectKey) }
        verify(exactly = 1)  { saveDocumentRepository.save(document) }
        verify(exactly = 1) { documentUpdateEventSender.publishEventFor(updateEvent) }

        confirmVerified(s3MetadataRepository)
        confirmVerified(saveDocumentRepository)
        confirmVerified(documentUpdateEventSender)
    }

}
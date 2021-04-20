package it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3

import ch.qos.logback.classic.Level
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.`updates a document on s3 with metadata from`
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.aFakeDocumentWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.applicationWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.bucket
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.objectKey
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.test.StepVerifier
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import java.util.*

internal class S3MetadataRepositoryTest {
    private val s3Client: S3AsyncClient =
        S3AsyncClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()

    private val s3Repository = S3Repository(s3Client)

    @BeforeEach
    fun changeLogLevel() {
        val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.level = Level.INFO
    }

    @Test
    fun `then fetch the document metadata for`() {
        val randomizerValue = UUID.randomUUID().toString()
        val document = aFakeDocumentWith(randomizerValue, applicationWith(Storage(bucket)))

        `updates a document on s3 with metadata from`(document, s3Repository)

        val metadataRepository = S3MetadataRepository(s3Client)
        StepVerifier.create(metadataRepository.objectMetadataFor(bucket, objectKey))
            .expectNext(
                DocumentMetadata(
                    mapOf(
                        "bucket" to bucket,
                        "randomizer" to "$randomizerValue",
                        "path" to "a_path",
                        "filename" to "a_file",
                        "extension" to "jpg",
                        "fullqualifiedfilepath" to "$bucket/a_path/a_file.jpg",
                        "prop1" to "A_VALUE",
                        "prop2" to "ANOTHER_VALUE"
                    )
                )
            )
            .verifyComplete()
    }
}
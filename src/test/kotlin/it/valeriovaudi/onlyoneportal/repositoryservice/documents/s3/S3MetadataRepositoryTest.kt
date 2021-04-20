package it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient

internal class S3MetadataRepositoryTest {
    private val bucket = System.getenv("AWS_TESTING_S3_APPLICATION_STORAGE")
    private val objectKey = System.getenv("AWS_TESTING_S3_APPLICATION_STORAGE")

    private val s3Client: S3AsyncClient =
        S3AsyncClient.builder()
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()


    @Test
    fun `then fetch the document metadata for`() {
        val metadataRepository = S3MetadataRepository(s3Client)

        StepVerifier.create(metadataRepository.objectMetadataFor(bucket, objectKey))
            .expectNext(DocumentMetadata(mapOf("bucket" to bucket)))
            .verifyComplete()
    }
}
package it.valeriovaudi.onlyoneportal.repositoryservice

import com.fasterxml.jackson.databind.ObjectMapper
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.application.YamlApplicationRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.application.YamlApplicationStorageMapping
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.*
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.*
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3MetadataRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.Clock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.Duration

@SpringBootApplication
@EnableConfigurationProperties(YamlApplicationStorageMapping::class)
class RepositoryServiceApplication {

    @Bean
    fun applicationStorageRepository(storage: YamlApplicationStorageMapping) =
        YamlApplicationRepository(storage)

    @Bean
    fun documentUpdateEventSender(
        objectMapper: ObjectMapper,
        sqsAsyncClient: SqsAsyncClient,
        applicationRepository: ApplicationRepository
    ) =
        DocumentUpdateEventSender(objectMapper, sqsAsyncClient, applicationRepository)


    @Bean
    fun documentRepository(
        reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
        s3Client: S3AsyncClient,
        documentUpdateEventSender: DocumentUpdateEventSender
    ) = AWSCompositeDocumentRepository(
        Clock(),
        S3Repository(s3Client),
        ESRepository(
            DeleteDocumentRepository(reactiveElasticsearchTemplate, DocumentEsIdGenerator()),
            FindAllDocumentRepository(reactiveElasticsearchTemplate),
            saveDocumentRepository(reactiveElasticsearchTemplate)
        ),
        documentUpdateEventSender
    )

    @Bean
    fun saveDocumentRepository(reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate) =
        SaveDocumentRepository(
            reactiveElasticsearchTemplate,
            DocumentEsIdGenerator()
        )

    @Bean
    fun storageUpdateEventsListener(
        documentUpdateEventSender: DocumentUpdateEventSender,
        applicationRepository: ApplicationRepository,
        sqsAsyncClient: SqsAsyncClient,
        s3Client: S3AsyncClient,
        saveDocumentRepository: SaveDocumentRepository,
        @Value("\${storage.update-events.queue}") queue: String,
        @Value("\${storage.update-events.max-number-of-message}") maxNumberOfMessage: Int,
        @Value("\${storage.update-events.visibility-time-out}") visibilityTimeOut: Int,
        @Value("\${storage.update-events.wait-time-seconds}") waitTimeSeconds: Int,
        ) =
        StorageUpdateEventsListener(
            Clock(),
            documentUpdateEventSender,
            applicationRepository,
            S3MetadataRepository(s3Client),
            saveDocumentRepository,
            sqsAsyncClient,
            ReceiveMessageRequestFactory(
                queue,
                maxNumberOfMessage, visibilityTimeOut, waitTimeSeconds
            ),
            Duration.ofSeconds(30)
        )

    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider =
        EnvironmentVariableCredentialsProvider.create()


    @Bean
    fun s3Client(awsCredentialsProvider: AwsCredentialsProvider): S3AsyncClient =
        S3AsyncClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .build()

    @Bean
    fun sqsAsyncClient(awsCredentialsProvider: AwsCredentialsProvider): SqsAsyncClient =
        SqsAsyncClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .build()
}

fun main(args: Array<String>) {
    runApplication<RepositoryServiceApplication>(*args)
}

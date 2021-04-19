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
    fun documentRepository(
        reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
        s3Client: S3AsyncClient,
        sqsAsyncClient: SqsAsyncClient,
        objectMapper: ObjectMapper,
        applicationRepository: ApplicationRepository
    ) =
        AWSCompositeDocumentRepository(
            Clock(),
            S3Repository(s3Client),
            ESRepository(
                DeleteDocumentRepository(reactiveElasticsearchTemplate, DocumentEsIdGenerator()),
                FindAllDocumentRepository(reactiveElasticsearchTemplate),
                SaveDocumentRepository(
                    reactiveElasticsearchTemplate,
                    DocumentEsIdGenerator()
                )
            ),
            DocumentUpdateEventSender(objectMapper, sqsAsyncClient, applicationRepository)
        )

    @Bean
    fun storageUpdateEventsListener(sqsAsyncClient: SqsAsyncClient, s3Client: S3AsyncClient) =
        StorageUpdateEventsListener(
            S3MetadataRepository(s3Client),
            sqsAsyncClient,
            ReceiveMessageRequestFactory(
                System.getenv("AWS_TESTING_SQS_STORAGE_REINDEX_QUEUE"),
                10, 10, 10
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

package it.valeriovaudi.onlyoneportal.repositoryservice

import com.fasterxml.jackson.databind.ObjectMapper
import it.valeriovaudi.onlyoneportal.repositoryservice.application.*
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
import org.springframework.nativex.hint.TypeHint
import org.springframework.nativex.hint.TypeHints
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import java.time.Duration

@TypeHints(
        TypeHint(types = [ApplicationRepository::class], typeNames = ["it.valeriovaudi.onlyoneportal.repositoryservice.application.YamlApplicationRepository"]),
        TypeHint(types = [DocumentRepository::class], typeNames = ["it.valeriovaudi.onlyoneportal.repositoryservice.documents.AWSCompositeDocumentRepository"]),

        TypeHint(types = [ApplicationName::class]),
        TypeHint(types = [Application::class]),
        TypeHint(types = [Storage::class]),

        TypeHint(types = [Document::class]),
        TypeHint(types = [FileContent::class]),
        TypeHint(types = [FileName::class]),
        TypeHint(types = [FileContentType::class]),
        TypeHint(types = [Path::class]),
        TypeHint(types = [DocumentMetadata::class]),
        TypeHint(types = [DocumentMetadataPage::class]),
)
@SpringBootApplication(proxyBeanMethods = false)
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
            documentUpdateEventSender: DocumentUpdateEventSender,
            saveDocumentRepository: SaveDocumentRepository
    ) = AWSCompositeDocumentRepository(
            Clock(),
            S3Repository(s3Client),
            ESRepository(
                    DeleteDocumentRepository(reactiveElasticsearchTemplate, DocumentEsIdGenerator()),
                    FindAllDocumentRepository(reactiveElasticsearchTemplate),
                    saveDocumentRepository
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

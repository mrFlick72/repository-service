package it.valeriovaudi.onlyoneportal.repositoryservice

import com.fasterxml.jackson.databind.ObjectMapper
import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.YamlApplicationStorageMapping
import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.YamlApplicationStorageRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.AWSCompositeDocumentRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentUpdateEventSender
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.*
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.Clock
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@SpringBootApplication
@EnableConfigurationProperties(YamlApplicationStorageMapping::class)
class RepositoryServiceApplication {

    @Bean
    fun applicationStorageRepository(storage: YamlApplicationStorageMapping) =
            YamlApplicationStorageRepository(storage)

    @Bean
    fun documentRepository(reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                           s3Client: S3AsyncClient,
                           sqsAsyncClient: SqsAsyncClient,
                           objectMapper: ObjectMapper,
                           applicationStorageRepository: ApplicationStorageRepository) =
            AWSCompositeDocumentRepository(
                    Clock(),
                    S3Repository(s3Client),
                    ESRepository(
                            DeleteDocumentRepository(reactiveElasticsearchTemplate, DocumentMetadataEsIdGenerator()),
                            FindDocumentRepository(reactiveElasticsearchTemplate),
                            SaveDocumentRepository(reactiveElasticsearchTemplate, applicationStorageRepository, DocumentMetadataEsIdGenerator())
                    ),
                    DocumentUpdateEventSender(objectMapper, sqsAsyncClient, applicationStorageRepository),
                    applicationStorageRepository
            )

    @Bean
    fun awsCredentialsProvider(@Value("\${aws.access-key}") accessKey: String,
                               @Value("\${aws.secret-key}") awsSecretKey: String): AwsCredentialsProvider =
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, awsSecretKey))


    @Bean
    fun s3Client(@Value("\${aws.region}") awsRegion: String,
                 awsCredentialsProvider: AwsCredentialsProvider) = S3AsyncClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(awsRegion))
            .build()

    @Bean
    fun sqsAsyncClient(@Value("\${aws.region}") awsRegion: String,
                       awsCredentialsProvider: AwsCredentialsProvider) = SqsAsyncClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(awsRegion))
            .build()
}

fun main(args: Array<String>) {
    runApplication<RepositoryServiceApplication>(*args)
}

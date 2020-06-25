package it.valeriovaudi.repositoryservice

import com.fasterxml.jackson.databind.ObjectMapper
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.time.Instant
import java.time.LocalDateTime

interface DocumentRepository {

    fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent>

    fun saveDocumentFor(application: Application, path: Path, content: FileContent): Mono<Unit>

}

class AWSCompositeDocumentRepository(private val clock: Clock,
                                     private val s3Repository: S3Repository,
                                     private val sqsEventSender: UpdateEventSender,
                                     private val applicationStorageRepository: ApplicationStorageRepository) : DocumentRepository {

    override fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent> =
            applicationStorageRepository.storageConfigurationFor(application)
                    .map { it.storage }
                    .map { storage -> s3Repository.getFromS3(storage, path, fileName) }
                    .orElse(Mono.empty())
                    .map { FileContent(fileName, FileContentType(it.response().contentType()), it.asByteArray()) }

    override fun saveDocumentFor(application: Application, path: Path, content: FileContent) =
            applicationStorageRepository.storageConfigurationFor(application)
                    .map { Mono.just(it.storage) }
                    .orElse(Mono.empty())
                    .log()
                    .flatMap { s3Repository.putOnS3(it, path, content).log() }
                    .flatMap { sqsEventSender.publishEventFor(StorageUpdateEvent(application, path, content.fileName, clock.now())).log() }
                    .flatMap { Mono.just(Unit) }
}

class UpdateEventSender(private val objectMapper: ObjectMapper,
                        private val sqsAsyncClient: SqsAsyncClient,
                        private val applicationStorageRepository: ApplicationStorageRepository) {
    fun publishEventFor(event: StorageUpdateEvent): Mono<Unit> =
            applicationStorageRepository.storageConfigurationFor(event.application)
                    .flatMap { config -> config.updateSignals }
                    .map { updateSignals ->
                        Mono.fromCompletionStage(
                                sqsAsyncClient.sendMessage {
                                    it.messageBody(objectMapper.writeValueAsString(event))
                                            .queueUrl(updateSignals.sqsQueue)
                                }
                        ).flatMap { Mono.just(Unit) }
                    }.orElse(Mono.just(Unit))
}

class S3Repository(private val s3Client: S3AsyncClient) {
    fun putOnS3(storage: Storage, path: Path, content: FileContent): Mono<Unit> {
        return Mono.fromCompletionStage {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(storage.bucket)
                    .key(s3KeyFor(path, content.fileName))
                    .build(),
                    AsyncRequestBody.fromBytes(content.content))
        }.flatMap { Mono.just(Unit) }
    }

    fun getFromS3(storage: Storage, path: Path, fileName: FileName): Mono<ResponseBytes<GetObjectResponse>> {
        return Mono.fromCompletionStage {
            s3Client.getObject(GetObjectRequest.builder()
                    .bucket(storage.bucket)
                    .key(s3KeyFor(path, fileName))
                    .build(),
                    AsyncResponseTransformer.toBytes())
        }
    }

    private fun s3KeyFor(path: Path, fileName: FileName) =
            "${listOf(path.value, fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileName.extension}"
}

data class StorageUpdateEvent(val application: Application,
                              val path: Path,
                              val fileName: FileName,
                              val updateTimesTamp: TimeStamp)

data class TimeStamp(val localDateTime: LocalDateTime) {
    companion object {
        fun now(): TimeStamp = TimeStamp(LocalDateTime.now())
        fun nowInMilliSecondsAsString(): String = Instant.now().toEpochMilli().toString()
    }
}

class Clock {
    fun now() = TimeStamp.now()
}

data class Application(val value: String)
data class Path(val value: String)
data class FileName(val name: String, val extension: String) {
    companion object {
        fun fileNameFrom(completeFileName: String): FileName {
            val fileExt = completeFileName.split(".").last()
            println(completeFileName)
            println(fileExt)
            val fileName = completeFileName.removeSuffix(".$fileExt")
            return FileName(fileName, fileExt)
        }
    }
}

data class FileContentType(val value: String)
data class FileContent(val fileName: FileName, val contentType: FileContentType, val content: ByteArray)
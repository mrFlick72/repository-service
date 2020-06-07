package it.valeriovaudi.repositoryservice

import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest

interface DocumentRepository {

    fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent>

}

class S3DocumentRepository(private val s3Client: S3AsyncClient,
                           private val applicationStorageRepository: ApplicationStorageRepository) : DocumentRepository {

    override fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent> {
        return applicationStorageRepository.getStorageConfigurationFor(application)
                .map { it.bucket as S3Bucket }
                .map { bucket ->
                    Mono.fromCompletionStage {
                        s3Client.getObject(GetObjectRequest.builder()
                                .bucket(bucket.name)
                                .key(s3KeyFor(path, fileName))
                                .build(),
                                AsyncResponseTransformer.toBytes())
                    }
                }.orElse(Mono.empty())
                .map { FileContent(fileName, FileContentType(it.response().contentType()), it.asByteArray()) }
    }

    private fun s3KeyFor(path: Path, fileName: FileName) =
            "${listOf(path.value, fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileName.extension}"

}

data class Application(val value: String)
data class Path(val value: String)
data class FileName(val name: String, val extension: String)
data class FileContentType(val value: String)
data class FileContent(val fileName: FileName, val contentType: FileContentType, val content: ByteArray)
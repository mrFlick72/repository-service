package it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.*
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document.Companion.fullQualifiedFilePathFor
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class S3Repository(private val s3Client: S3AsyncClient) : DocumentRepository {

    override fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent> =
            Mono.fromCompletionStage {
                s3Client.getObject(GetObjectRequest.builder()
                        .bucket(application.storage.bucket)
                        .key(fullQualifiedFilePathFor(path, fileName))
                        .build(),
                        AsyncResponseTransformer.toBytes())
            }.flatMap { Mono.just(FileContent(fileName, FileContentType(it.response().contentType()), it.asByteArray())) }

    override fun findDocumentsFor(application: Application, documentMetadata: DocumentMetadata, page: Int, size: Int): Mono<DocumentMetadataPage> =
            TODO("Not yet implemented")


    override fun saveDocumentFor(document: Document): Mono<Unit> = Mono.fromCompletionStage {
        s3Client.putObject(PutObjectRequest.builder()
                .contentType(document.fileContent.contentType.value)
                .bucket(document.application.storage.bucket)
                .metadata(document.metadataWithSystemMetadataFor(document.application.storage))
                .key(document.fullQualifiedFilePath())
                .build(),
                AsyncRequestBody.fromBytes(document.fileContent.content))
    }.flatMap { Mono.just(Unit) }

    override fun deleteDocumentFor(application: Application, path: Path, fileName: FileName): Mono<Unit> =
            Mono.fromCompletionStage {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(application.storage.bucket)
                        .key(fullQualifiedFilePathFor(path, fileName))
                        .build())
            }.flatMap { Mono.just(Unit) }
}
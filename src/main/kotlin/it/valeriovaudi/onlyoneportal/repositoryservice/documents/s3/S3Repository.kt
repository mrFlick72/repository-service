package it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document.Companion.fullQualifiedFilePathFor
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.FileName
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Path
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest


class S3Repository(private val s3Client: S3AsyncClient) {

    //    fun save(document: Document): Mono<Unit> = Mono.fromCompletionStage
    fun save(storage: Storage, document: Document): Mono<Unit> = Mono.fromCompletionStage {
        s3Client.putObject(PutObjectRequest.builder()
                .contentType(document.fileContent.contentType.value)
                .bucket(storage.bucket)
                .metadata(document.metadataWithSystemMetadataFor(storage))
                .key(document.fullQualifiedFilePath())
                .build(),
                AsyncRequestBody.fromBytes(document.fileContent.content))
    }.flatMap { Mono.just(Unit) }

    //    fun find(application: Application, path: Path, fileName: FileName): Mono<ResponseBytes<GetObjectResponse>>
    fun findOne(storage: Storage, path: Path, fileName: FileName): Mono<ResponseBytes<GetObjectResponse>> =
            Mono.fromCompletionStage {
                s3Client.getObject(GetObjectRequest.builder()
                        .bucket(storage.bucket)
                        .key(fullQualifiedFilePathFor(path, fileName))
                        .build(),
                        AsyncResponseTransformer.toBytes())
            }

    //    fun delete(application: Application, path: Path, fileName: FileName): Mono<Unit> =
    fun delete(storage: Storage, path: Path, fileName: FileName): Mono<Unit> =
            Mono.fromCompletionStage {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(storage.bucket)
                        .key(fullQualifiedFilePathFor(path, fileName))
                        .build())
            }.flatMap { Mono.just(Unit) }

}
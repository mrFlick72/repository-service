package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document.Companion.fullQualifiedFilePathFor
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest


class S3Repository(private val s3Client: S3AsyncClient) {

    fun save(storage: Storage, document: Document): Mono<Unit> {
        return Mono.fromCompletionStage {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(storage.bucket)
                    .metadata(document.metadataWithSystemMetadataFor(storage))
                    .key(document.fullQualifiedFilePath())
                    .build(),
                    AsyncRequestBody.fromBytes(document.fileContent.content))
        }.flatMap { Mono.just(Unit) }
    }

    fun find(storage: Storage, path: Path, fileName: FileName): Mono<ResponseBytes<GetObjectResponse>> {
        return Mono.fromCompletionStage {
            s3Client.getObject(GetObjectRequest.builder()
                    .bucket(storage.bucket)
                    .key(fullQualifiedFilePathFor(path, fileName))
                    .build(),
                    AsyncResponseTransformer.toBytes())
        }
    }


}
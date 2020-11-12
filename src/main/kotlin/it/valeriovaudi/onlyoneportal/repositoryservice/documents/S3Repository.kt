package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentHelper.s3KeyFor
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest


class S3Repository(private val s3Client: S3AsyncClient) {

    fun putOnS3(storage: Storage, path: Path, content: FileContent, metadata: DocumentMetadata = DocumentMetadata.empty()): Mono<Unit> {
        return Mono.fromCompletionStage {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(storage.bucket)
                    .metadata(DocumentHelper.metadata(storage, path, content.fileName, metadata))
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


}
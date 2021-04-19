package it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import reactor.core.publisher.Mono
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.HeadObjectRequest

class S3MetadataRepository(private val s3Client: S3AsyncClient) {

    fun objectMetadataFor(bucket: String, objectKey: String): Mono<DocumentMetadata> {
        val request = HeadObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build()
        return Mono.fromCompletionStage { s3Client.headObject(request) }
            .flatMap { Mono.just(DocumentMetadata(it.metadata())) }
    }
}
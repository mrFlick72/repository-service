package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.ESRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.Clock
import reactor.core.publisher.Mono

interface DocumentRepository {

    fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent>

    fun findDocumentsFor(application: Application, documentMetadata: DocumentMetadata): Mono<DocumentMetadataPage>

    fun saveDocumentFor(document: Document): Mono<Unit>

    fun deleteDocumentFor(application: Application, path: Path, fileName: FileName): Mono<Unit>

}

class AWSCompositeDocumentRepository(private val clock: Clock,
                                     private val s3Repository: S3Repository,
                                     private val esRepository: ESRepository,
                                     private val sqsEventSenderDocument: DocumentUpdateEventSender,
                                     private val applicationStorageRepository: ApplicationStorageRepository) : DocumentRepository {

    override fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent> =
            applicationStorageRepository.storageConfigurationFor(application)
                    .map { it.storage }
                    .map { storage -> s3Repository.find(storage, path, fileName) }
                    .orElse(Mono.empty())
                    .map { FileContent(fileName, FileContentType(it.response().contentType()), it.asByteArray()) }

    override fun findDocumentsFor(application: Application, documentMetadata: DocumentMetadata): Mono<DocumentMetadataPage> =
            esRepository.find(application, documentMetadata)

    override fun saveDocumentFor(document: Document) =
            applicationStorageRepository.storageConfigurationFor(document.application)
                    .map { Mono.just(it.storage) }
                    .orElse(Mono.empty())
                    .flatMap { s3Repository.save(it, document) }
                    .flatMap { esRepository.save(document) }
                    .flatMap { sqsEventSenderDocument.publishEventFor(StorageUpdateEvent(document.application, document.path, document.fileContent.fileName, clock.now())) }
                    .flatMap { Mono.just(Unit) }

    override fun deleteDocumentFor(application: Application, path: Path, fileName: FileName): Mono<Unit> =
            applicationStorageRepository.storageConfigurationFor(application)
                    .map { Mono.just(it.storage) }
                    .orElse(Mono.empty())
                    .flatMap { storage ->
                        Mono.zip(s3Repository.delete(storage, path, fileName),
                                esRepository.delete(application, DocumentMetadata(Document.fileBasedMetadataFor(storage, path, fileName))))
                    }
                    .flatMap { Mono.just(Unit) }

}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationRepository
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
                                     private val applicationRepository: ApplicationRepository) : DocumentRepository {

    override fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent> =
            s3Repository.findOne(application, path, fileName)
                    .flatMap { Mono.just(FileContent(fileName, FileContentType(it.response().contentType()), it.asByteArray())) }

    override fun findDocumentsFor(application: Application, documentMetadata: DocumentMetadata): Mono<DocumentMetadataPage> =
            esRepository.find(application, documentMetadata)

    override fun saveDocumentFor(document: Document) =
            s3Repository.save(document)
                    .flatMap { esRepository.save(document) }
                    .flatMap { sqsEventSenderDocument.publishEventFor(StorageUpdateEvent(document.application.applicationName, document.path, document.fileContent.fileName, clock.now())) }
                    .flatMap { Mono.just(Unit) }

    override fun deleteDocumentFor(application: Application, path: Path, fileName: FileName): Mono<Unit> =
            Mono.zip(s3Repository.delete(application, path, fileName),
                    esRepository.delete(application, DocumentMetadata(Document.fileBasedMetadataFor(application.storage, path, fileName))))

                    .flatMap { Mono.just(Unit) }

}
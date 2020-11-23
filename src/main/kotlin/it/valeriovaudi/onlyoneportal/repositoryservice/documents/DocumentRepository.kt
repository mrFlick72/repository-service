package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.ESRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import it.valeriovaudi.onlyoneportal.repositoryservice.time.Clock
import reactor.core.publisher.Mono

interface DocumentRepository {

    fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent>

    fun findDocumentsFor(application: Application, documentMetadata: DocumentMetadata, page: Int = 0, size: Int = 10): Mono<DocumentMetadataPage>

    fun saveDocumentFor(document: Document): Mono<Unit>

    fun deleteDocumentFor(application: Application, path: Path, fileName: FileName): Mono<Unit>

}

class AWSCompositeDocumentRepository(private val clock: Clock,
                                     private val s3Repository: S3Repository,
                                     private val esRepository: ESRepository,
                                     private val sqsEventSenderDocument: DocumentUpdateEventSender) : DocumentRepository {

    override fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent> =
            s3Repository.findOneDocumentFor(application, path, fileName)


    override fun findDocumentsFor(application: Application, documentMetadata: DocumentMetadata, page: Int, size: Int): Mono<DocumentMetadataPage> =
            esRepository.findDocumentsFor(application, documentMetadata, page, size)

    override fun saveDocumentFor(document: Document) =
            s3Repository.saveDocumentFor(document)
                    .flatMap { esRepository.saveDocumentFor(document) }
                    .flatMap { sqsEventSenderDocument.publishEventFor(StorageUpdateEvent(document.application.applicationName, document.path, document.fileContent.fileName, clock.now())) }
                    .flatMap { Mono.just(Unit) }

    override fun deleteDocumentFor(application: Application, path: Path, fileName: FileName): Mono<Unit> =
            Mono.zip(s3Repository.deleteDocumentFor(application, path, fileName),
                    esRepository.deleteDocumentFor(application, path, fileName))
                    .flatMap { Mono.just(Unit) }

}
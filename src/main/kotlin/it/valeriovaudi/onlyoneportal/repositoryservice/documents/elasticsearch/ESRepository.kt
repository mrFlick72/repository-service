package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadataPage
import reactor.core.publisher.Mono

class ESRepository(private val deleteDocumentRepository: DeleteDocumentRepository,
                   private val findAllDocumentRepository: FindAllDocumentRepository,
                   private val saveDocumentRepository: SaveDocumentRepository) {

    fun save(document: Document) = saveDocumentRepository.save(document)

    fun find(application: Application, documentMetadata: DocumentMetadata, page: Int = 0, size: Int = 10): Mono<DocumentMetadataPage> =
            findAllDocumentRepository.findAll(application, documentMetadata, page, size)

    fun delete(application: Application, documentMetadata: DocumentMetadata): Mono<Unit> =
            deleteDocumentRepository.delete(application, documentMetadata)

}

fun indexNameFor(application: Application) = "${application.applicationName.value}_indexes"
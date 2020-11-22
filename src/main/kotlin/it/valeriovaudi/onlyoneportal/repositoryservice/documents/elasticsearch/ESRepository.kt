package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadataPage
import reactor.core.publisher.Mono

class ESRepository(private val deleteDocumentRepository: DeleteDocumentRepository,
                   private val findDocumentRepository: FindDocumentRepository,
                   private val saveDocumentRepository: SaveDocumentRepository) {

    //*********************** WRITE FUNCTION ***************************************************************************

    fun save(document: Document) = saveDocumentRepository.save(document)

    //*********************** READ FUNCTION ****************************************************************************

    fun find(application: Application, documentMetadata: DocumentMetadata, page: Int = 0, size: Int = 10): Mono<DocumentMetadataPage> =
            findDocumentRepository.find(application, documentMetadata, page, size)

    ////////////////////// DELETE /////////////////

    fun delete(application: Application, documentMetadata: DocumentMetadata): Mono<Unit> =
            deleteDocumentRepository.delete(application, documentMetadata)

}

fun indexNameFor(application: Application) = "${application.value}_indexes"
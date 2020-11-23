package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.*
import reactor.core.publisher.Mono

class ESRepository(private val deleteDocumentRepository: DeleteDocumentRepository,
                   private val findAllDocumentRepository: FindAllDocumentRepository,
                   private val saveDocumentRepository: SaveDocumentRepository) : DocumentRepository {

    override fun findOneDocumentFor(application: Application, path: Path, fileName: FileName): Mono<FileContent> =
            TODO("Not yet implemented")


    override fun findDocumentsFor(application: Application, documentMetadata: DocumentMetadata, page: Int, size: Int): Mono<DocumentMetadataPage> =
            findAllDocumentRepository.findAll(application, documentMetadata, page, size)

    override fun saveDocumentFor(document: Document): Mono<Unit> = saveDocumentRepository.save(document)

    override fun deleteDocumentFor(application: Application, path: Path, fileName: FileName): Mono<Unit> =
            deleteDocumentRepository.delete(application, path, fileName)

}

fun indexNameFor(application: Application) = "${application.applicationName.value}_indexes"
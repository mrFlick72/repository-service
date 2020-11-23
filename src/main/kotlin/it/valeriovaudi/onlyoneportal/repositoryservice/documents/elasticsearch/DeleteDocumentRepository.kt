package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class DeleteDocumentRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                               private val idGenerator: ESIdGenerator<Map<String, String>>) {

    fun delete(application: Application, documentMetadata: DocumentMetadata): Mono<Unit> =
            deleteIndexRequestFor(application, documentMetadata)
                    .flatMap { Mono.just(Unit) }


    //    fun delete(application: Application, path: Path, fileName: FileName): Mono<Unit> =
    private fun deleteIndexRequestFor(application: Application, documentMetadata: DocumentMetadata) =
            reactiveElasticsearchTemplate.execute { client ->
                client.delete { deleteRequest ->
                    deleteRequest.index(indexNameFor(application))
                            .id(idGenerator.generateId(documentMetadata.content))
                }
            }.toMono()

}

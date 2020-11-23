package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.FileName
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Path
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.support.WriteRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class SaveDocumentRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                             private val idGenerator: ESIdGenerator<Triple<Storage, Path, FileName>>) {
    fun save(document: Document) =
            saveOnEsFor(document).flatMap { Mono.just(Unit) }

    private fun saveOnEsFor(document: Document): Mono<IndexResponse> =
            reactiveElasticsearchTemplate.execute { client ->
                client.index(indexRequestFor(document))
            }.toMono()


    private fun indexRequestFor(document: Document): (IndexRequest) -> Unit = { indexRequest ->
        val metadata = document.metadataWithSystemMetadataFor(document.application.storage)
        indexRequest.index(indexNameFor(document.application))
                .source(metadata)
                .id(idGenerator.generateId(Triple(document.application.storage, document.path, document.fileContent.fileName)))
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
    }

}

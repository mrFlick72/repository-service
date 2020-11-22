package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.support.WriteRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class SaveDocumentRepository (private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                              private val applicationStorageRepository: ApplicationStorageRepository,
                              private val idGenerator: ESIdGenerator<Map<String, String>>) {
    fun save(document: Document) =
            saveOnEsFor(document)
                    .map(::extractIndexIdFor)


    private fun extractIndexIdFor(documentMetadata: IndexResponse): Map<String, String> =
            mapOf("index" to documentMetadata.index, "documentId" to documentMetadata.id)

    private fun saveOnEsFor(document: Document): Mono<IndexResponse> =
            reactiveElasticsearchTemplate.execute { client ->
                client.index(indexRequestFor(document))
            }.toMono()


    private fun indexRequestFor(document: Document): (IndexRequest) -> Unit = { indexRequest ->
        applicationStorageRepository.storageConfigurationFor(document.application)
                .map {
                    val metadata = document.metadataWithSystemMetadataFor(it.storage)
                    indexRequest.index(indexNameFor(document.application))
                            .source(metadata)
                            .id(idGenerator.generateId(metadata))
                            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                }
    }

}

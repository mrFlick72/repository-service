package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.support.WriteRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.util.IdGenerator
import reactor.kotlin.core.publisher.toMono

class ESRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                   private val idGenerator: IdGenerator) {
    fun save(documentMetadata: DocumentMetadata) =
            reactiveElasticsearchTemplate.execute { client -> client.index(createDocumentOnIndexFor(documentMetadata)) }
                    .toMono()
                    .map(this::resultBodyFor)

    private fun createDocumentOnIndexFor(documentMetadata: DocumentMetadata): (IndexRequest) -> Unit = { indexRequest ->
        indexRequest
                .index("application")
                .source(documentMetadata.content)
                .id(idGenerator.generateId().toString())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .create(true)
    }

    private fun resultBodyFor(documentMetadata: IndexResponse): Map<String, String> =
            mapOf("index" to documentMetadata.index, "documentId" to documentMetadata.id)

}
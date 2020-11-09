package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.support.WriteRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.util.IdGenerator
import reactor.kotlin.core.publisher.toMono

class ESRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                   private val idGenerator: IdGenerator) {
    fun save(application: Application, fileName: FileName, documentMetadata: DocumentMetadata) =
            reactiveElasticsearchTemplate.execute { client ->
                client.index(
                        createDocumentOnIndexFor(application, fileName, documentMetadata)
                )
            }
                    .toMono()
                    .map(this::resultBodyFor)

    private fun createDocumentOnIndexFor(application: Application,
                                         fileName: FileName,
                                         documentMetadata: DocumentMetadata): (IndexRequest) -> Unit = { indexRequest ->
        indexRequest.index("application_indexes_${application.value}")
                .source(metadata(fileName, documentMetadata))
                .id(idGenerator.generateId().toString())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .create(true)
    }

    private fun metadata(fileName: FileName, documentMetadata: DocumentMetadata) =
            documentMetadata.content.plus(mapOf("fileName" to fileName.name, "extension" to fileName.extension))

    private fun resultBodyFor(documentMetadata: IndexResponse): Map<String, String> =
            mapOf("index" to documentMetadata.index, "documentId" to documentMetadata.id)

}
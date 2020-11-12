package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.extentions.toSha256
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder.searchSource
import org.reactivestreams.Publisher
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class ESRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                   private val applicationStorageRepository: ApplicationStorageRepository,
                   private val idGenerator: ESIdGenerator<Map<String, String>>) {

    //*********************** WRITE FUNCTION ***************************************************************************

    fun save(document: Document) =
            saveOnEsFor(document)
                    .toMono()
                    .map(::extractIndexIdFor)


    private fun extractIndexIdFor(documentMetadata: IndexResponse): Map<String, String> =
            mapOf("index" to documentMetadata.index, "documentId" to documentMetadata.id)

    private fun saveOnEsFor(document: Document): Publisher<IndexResponse> {
        return reactiveElasticsearchTemplate.execute { client ->
            client.index(indexRequestFor(document))
        }
    }

    private fun indexRequestFor(document: Document): (IndexRequest) -> Unit = { indexRequest ->
        applicationStorageRepository.storageConfigurationFor(document.application)
                .map {
                    val metadata = document.metadataWithSystemMetadataFor(it.storage)
                    indexRequest.index(indexNameFor(document.application))
                            .source(metadata)
                            .id(idGenerator.generateId(metadata))
                            .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                }
    }

    //*********************** READ FUNCTION ****************************************************************************


    fun find(application: Application, documentMetadata: DocumentMetadata, page: Int = 0, size: Int = 10): Mono<DocumentMetadataPage> {
        return Flux.just(QueryBuilders.boolQuery())
                .map { boolQueryBuilder(documentMetadata, it) }
                .flatMap { findFromEsFor(application, it, page, size) }
                .map(this::adaptDocument)
                .collectList()
                .map { DocumentMetadataPage(it, page, size) }
    }

    private fun boolQueryBuilder(documentMetadata: DocumentMetadata, builder: BoolQueryBuilder): BoolQueryBuilder {
        documentMetadata.content.map { entry -> builder.should(QueryBuilders.matchQuery(entry.key, entry.value)) }; return builder
    }

    private fun findFromEsFor(application: Application, it: BoolQueryBuilder?, page: Int, size: Int): Publisher<SearchHit> {
        return reactiveElasticsearchTemplate.execute { client ->
            client.search(searchRequestFor(application, it, page, size))
        }
    }

    private fun searchRequestFor(application: Application, it: BoolQueryBuilder?, page: Int, size: Int): (SearchRequest) -> Unit {
        return { searchRequest ->
            searchRequest.indices(indexNameFor(application))
                    .source(searchSource().query(it).from(page).size(size))
        }
    }

    private fun indexNameFor(application: Application) = "${application.value}_indexes"

    private fun adaptDocument(it: SearchHit) =
            DocumentMetadata(it.sourceAsMap.mapValues { entry -> entry.value.toString() })

}

interface ESIdGenerator<T> {

    fun generateId(criteria: T): String
}

class DocumentMetadataEsIdGenerator() : ESIdGenerator<Map<String, String>> {
    override fun generateId(criteria: Map<String, String>): String {
        val toSha256 = criteria["fullQualifiedFilePath"]!!.toSha256()
        println("DocumentMetadataEsIdGenerator: $toSha256")
        return toSha256
    }

}
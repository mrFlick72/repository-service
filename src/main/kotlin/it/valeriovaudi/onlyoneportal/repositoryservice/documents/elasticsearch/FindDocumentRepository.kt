package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadataPage
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono

class FindDocumentRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate) {
    private val logger: Logger = LoggerFactory.getLogger(FindDocumentRepository::class.java)


    fun find(application: Application, documentMetadata: DocumentMetadata, page: Int = 0, size: Int = 10): Mono<DocumentMetadataPage> {
        return Flux.just(QueryBuilders.boolQuery())
                .map { boolQueryBuilder(documentMetadata, it) }
                .flatMap {
                    findFromEsFor(application, it, page, size).toFlux().map(this::adaptDocument).collectList()
                            .zipWith(countFromEsFor(application, it).toMono())
                }.toMono()
                .map { DocumentMetadataPage(it.t1, page, size, it.t2) }
    }

    private fun boolQueryBuilder(documentMetadata: DocumentMetadata, builder: BoolQueryBuilder): BoolQueryBuilder {
        documentMetadata.content.map { entry -> builder.must(QueryBuilders.matchQuery(entry.key, entry.value)) };
        return builder
    }

    private fun findFromEsFor(application: Application, queryBuilder: BoolQueryBuilder, page: Int, size: Int): Publisher<SearchHit> {
        return reactiveElasticsearchTemplate.execute { client ->
            logger.debug("query:\n$queryBuilder")
            client.search(searchRequestFor(application, queryBuilder, page, size))
        }
    }

    private fun searchRequestFor(application: Application, it: BoolQueryBuilder, page: Int, size: Int): (SearchRequest) -> Unit {
        return { searchRequest ->
            searchRequest.indices(indexNameFor(application))
                    .source(SearchSourceBuilder.searchSource().query(it).from(page).size(size))
        }
    }

    private fun countFromEsFor(application: Application, queryBuilder: BoolQueryBuilder): Publisher<Int> {
        return reactiveElasticsearchTemplate.execute { client ->
            logger.debug("query:\n$queryBuilder")
            client.count(searchRequestFor(application, queryBuilder))
                    .map { it.toInt() }
        }
    }

    private fun searchRequestFor(application: Application, it: BoolQueryBuilder): (SearchRequest) -> Unit {
        return { searchRequest ->
            searchRequest.indices(indexNameFor(application))
                    .source(SearchSourceBuilder.searchSource().query(it))
        }
    }

    private fun adaptDocument(it: SearchHit) =
            DocumentMetadata(it.sourceAsMap.mapValues { entry -> entry.value.toString() })

}
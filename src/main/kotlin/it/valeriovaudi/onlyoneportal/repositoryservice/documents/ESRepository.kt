package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
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
import org.springframework.util.IdGenerator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono


class ESRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                   private val applicationStorageRepository: ApplicationStorageRepository,
                   private val idGenerator: IdGenerator) {

    fun save(application: Application, path: Path, fileName: FileName, documentMetadata: DocumentMetadata) =
            saveOnEsFor(application, path, fileName, documentMetadata)
                    .toMono()
                    .map(this::resultBodyFor)

    private fun saveOnEsFor(application: Application, path: Path, fileName: FileName, documentMetadata: DocumentMetadata): Publisher<IndexResponse> {
        return reactiveElasticsearchTemplate.execute { client ->
            client.index(indexRequestFor(application, path, fileName, documentMetadata))
        }
    }

    private fun indexRequestFor(application: Application,
                                path: Path,
                                fileName: FileName,
                                documentMetadata: DocumentMetadata): (IndexRequest) -> Unit = { indexRequest ->
        indexRequest.index(indexNameFor(application))
                .source(metadata(application, path, fileName, documentMetadata))
                .id(idGenerator.generateId().toString())
                .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                .create(true)
    }


    fun find(application: Application, documentMetadata: DocumentMetadata, page: Int = 0, size: Int = 10): Mono<DocumentMetadataPage> {
        return Flux.just(QueryBuilders.boolQuery())
                .map { documentMetadata.content.map { entry -> it.should(QueryBuilders.matchQuery(entry.key, entry.value)) }; it }
                .flatMap { findFromEsFor(application, it, page, size) }
                .map { DocumentMetadata(it.sourceAsMap.mapValues { entry -> entry.value.toString() }) }
                .collectList()
                .map { DocumentMetadataPage(it, page, size) }
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

    private fun metadata(application: Application, path: Path, fileName: FileName, documentMetadata: DocumentMetadata) =
            documentMetadata.content.plus(fileBasedMetadataFor(application, path, fileName))

    private fun fileBasedMetadataFor(application: Application, path: Path, fileName: FileName): Map<String, String> {
        return applicationStorageRepository.storageConfigurationFor(application).map {
            mapOf(
                    "fullQualifiedFilePath" to S3Repository.s3KeyFor(path, fileName),
                    "bucket" to it.storage.bucket,
                    "path" to path.value,
                    "fileName" to fileName.name,
                    "extension" to fileName.extension
            )
        }.orElse(emptyMap())
    }

    private fun resultBodyFor(documentMetadata: IndexResponse): Map<String, String> =
            mapOf("index" to documentMetadata.index, "documentId" to documentMetadata.id)

}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.common.document.DocumentField
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHits
import org.elasticsearch.search.aggregations.AggregationBuilders.terms
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder.*
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.util.IdGenerator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono


class ESRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                   private val applicationStorageRepository: ApplicationStorageRepository,
                   private val idGenerator: IdGenerator) {
    fun save(application: Application, path: Path, fileName: FileName, documentMetadata: DocumentMetadata) =
            reactiveElasticsearchTemplate.execute { client ->
                client.index(
                        createDocumentOnIndexFor(application, path, fileName, documentMetadata)
                )
            }
                    .toMono()
                    .map(this::resultBodyFor)

    private fun createDocumentOnIndexFor(application: Application,
                                         path: Path,
                                         fileName: FileName,
                                         documentMetadata: DocumentMetadata): (IndexRequest) -> Unit = { indexRequest ->
        indexRequest.index(indexNameFor(application))
                .source(metadata(application, path, fileName, documentMetadata))
                .id(idGenerator.generateId().toString())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .create(true)
    }

    private fun indexNameFor(application: Application) =
            "${application.value}_indexes"

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

    fun findFor(application: Application, documentMetadata: DocumentMetadata): Flux<DocumentMetadata> {
        return Flux.just(QueryBuilders.boolQuery())
                .map { documentMetadata.content.map { entry -> it.should(QueryBuilders.matchQuery(entry.key, entry.value)) }; it }
                .flatMap {
                    reactiveElasticsearchTemplate.execute { client ->
                        client.search { searchRequest ->
                            searchRequest.indices(indexNameFor(application))
                                    .source(searchSource().query(it).from(0).size(5))
                        }
                    }
                }.map { DocumentMetadata(it.sourceAsMap.mapValues { entry -> entry.toString() }) }
    }

}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.support.WriteRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.util.IdGenerator
import reactor.kotlin.core.publisher.toMono

class ESRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
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
        indexRequest.index("application_indexes_${application.value}")
                .source(metadata(path, fileName, documentMetadata))
                .id(idGenerator.generateId().toString())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .create(true)
    }

    private fun metadata(path: Path, fileName: FileName, documentMetadata: DocumentMetadata) =
            documentMetadata.content.plus(fileBasedMetadataFor(path, fileName))

    private fun fileBasedMetadataFor(path: Path, fileName: FileName): Map<String, String> {
        return mapOf(
                "fullQualifiedFilePath" to S3Repository.s3KeyFor(path, fileName),
                "path" to path.value,
                "fileName" to fileName.name,
                "extension" to fileName.extension
        )
    }

    private fun resultBodyFor(documentMetadata: IndexResponse): Map<String, String> =
            mapOf("index" to documentMetadata.index, "documentId" to documentMetadata.id)

}
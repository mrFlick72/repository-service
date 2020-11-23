package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.FileName
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Path
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class DeleteDocumentRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate,
                               private val idGenerator: ESIdGenerator<Triple<Storage, Path, FileName>>) {

    fun delete(application: Application, path: Path, fileName: FileName): Mono<Unit> =
            deleteIndexRequestFor(application, path, fileName)
                    .flatMap { Mono.just(Unit) }

    private fun deleteIndexRequestFor(application: Application, path: Path, fileName: FileName) =
            reactiveElasticsearchTemplate.execute { client ->
                client.delete { deleteRequest ->
                    deleteRequest.index(indexNameFor(application))
                            .id(idGenerator.generateId(Triple(application.storage, path, fileName)))
                }
            }.toMono()

}

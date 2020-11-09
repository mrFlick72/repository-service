package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import org.elasticsearch.action.support.WriteRequest
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*


class ESRepository(private val reactiveElasticsearchTemplate: ReactiveElasticsearchTemplate) {
    fun save(documentMetadata: DocumentMetadata): Mono<Unit> =
            reactiveElasticsearchTemplate.execute { client ->
                client.index { indexRequest ->
                    indexRequest
                            .index("application")
                            .source(documentMetadata.content)
                            .id(UUID.randomUUID().toString())
                            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                            .create(true)

                }
            }.toMono()
                    .map { documentMetadata ->
                        println("documentMetadata.content");
                        println(documentMetadata);
                        Unit
                    }
}
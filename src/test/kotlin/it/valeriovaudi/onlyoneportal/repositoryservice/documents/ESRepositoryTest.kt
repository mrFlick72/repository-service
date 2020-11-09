package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.Test
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient


internal class ESRepositoryTest {

    fun client(): ReactiveElasticsearchClient? {
        val clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:39200")
        /*        .withWebClientConfigurer { webClient: WebClient ->
                    val exchangeStrategies = ExchangeStrategies.builder()
                            .codecs { configurer: ClientCodecConfigurer ->
                                configurer.defaultCodecs()
                                        .maxInMemorySize(-1)
                            }
                            .build()
                    webClient.mutate().exchangeStrategies(exchangeStrategies).build()
                }*/
                .build()
        return ReactiveRestClients.create(clientConfiguration)
    }
    @Test
    internal fun `save a document on es`() {
        val defaultReactiveElasticsearchClient = client()!!
        val reactiveElasticsearchTemplate = ReactiveElasticsearchTemplate(defaultReactiveElasticsearchClient)
        val esRepository = ESRepository(reactiveElasticsearchTemplate)

        esRepository.save(DocumentMetadata(mapOf("test" to "test"))).block()
/*        val stream = StepVerifier.create(
                esRepository.save(DocumentMetadata(mapOf("test" to "test")))
        )
        stream.expectNext(Unit)
        stream.expectComplete()*/
    }
}
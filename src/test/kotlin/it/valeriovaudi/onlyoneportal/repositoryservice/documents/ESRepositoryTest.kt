package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.elasticsearch.client.ClientConfiguration.builder
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients.create
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.util.IdGenerator
import reactor.test.StepVerifier
import java.util.*
import java.util.function.Consumer


internal class ESRepositoryTest {

    private fun template(): ReactiveElasticsearchTemplate =
            ReactiveElasticsearchTemplate(create(builder().connectedTo("localhost:39200").build()))

    @Test
    internal fun `save a document on es`() {
        val id = UUID.randomUUID()
        val reactiveElasticsearchTemplate = template()
        val esRepository = ESRepository(reactiveElasticsearchTemplate, IdGenerator { id })

        val saveStream = esRepository.save(
                Application("an_app"),
                Path("/apath"),
                FileName("a_file", "jpg"),
                DocumentMetadata(mapOf("test" to "test"))
        )

        val verifier = StepVerifier.create(saveStream)
        verifier.assertNext(Consumer {
            Assertions.assertEquals(mapOf(
                    "index" to "application_indexes_an_app",
                    "documentId" to id.toString()
            ), it)
        })
        verifier.verifyComplete()
    }

}
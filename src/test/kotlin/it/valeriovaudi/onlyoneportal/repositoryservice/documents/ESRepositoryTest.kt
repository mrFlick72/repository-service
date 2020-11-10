package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.TestFixture.testableApplicationStorageRepository
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.data.elasticsearch.client.ClientConfiguration.builder
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients.create
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import org.springframework.util.IdGenerator
import reactor.test.StepVerifier
import java.util.*


internal class ESRepositoryTest {

    private fun template(): ReactiveElasticsearchTemplate =
            ReactiveElasticsearchTemplate(create(builder().connectedTo("localhost:39200").build()))

    private val id = UUID.randomUUID()

    private val reactiveElasticsearchTemplate = template()
    private val esRepository = ESRepository(reactiveElasticsearchTemplate, testableApplicationStorageRepository, IdGenerator { id })

    @Test
    @Order(1)
    internal fun `save a document on es`() {
        val saveStream = esRepository.save(
                Application("an_app"),
                Path("/a_path"),
                FileName("a_file", "jpg"),
                DocumentMetadata(mapOf("prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE"))
        )

        val verifier = StepVerifier.create(saveStream)
        verifier.assertNext {
            Assertions.assertEquals(mapOf(
                    "index" to "an_app_indexes",
                    "documentId" to id.toString()
            ), it)
        }
        verifier.verifyComplete()
    }

    @Test
    @Order(2)
    internal fun `get a document on ES`() {
        val stream = esRepository.findFor(Application("an_app"),
                DocumentMetadata(mapOf("prop1" to "A_VALUE")))
        val verifier = StepVerifier.create(stream)

        verifier.assertNext {
            Assertions.assertEquals(DocumentMetadata(
                    mapOf(
                            "prop1" to "A_VALUE",
                            "prop2" to "ANOTHER_VALUE",
                            "bucket" to "A_BUCKET",
                            "path" to "/a_path",
                            "fullQualifiedFilePath" to "/a_path/a_file.jpg",
                            "fileName" to "a_file",
                            "extension" to "jpg"
                    )
            ), it)
        }
        verifier.verifyComplete()
    }
}
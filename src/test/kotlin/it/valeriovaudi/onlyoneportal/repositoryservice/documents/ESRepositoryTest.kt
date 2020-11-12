package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.TestFixture.iGenerator
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.TestFixture.testableApplicationStorageRepository
import org.junit.jupiter.api.*
import org.springframework.data.elasticsearch.client.ClientConfiguration.builder
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients.create
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.test.StepVerifier
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ESRepositoryTest {

    private fun template(): ReactiveElasticsearchTemplate =
            ReactiveElasticsearchTemplate(create(builder().connectedTo("localhost:39200").build()))

    private val id = UUID.randomUUID()

    private val reactiveElasticsearchTemplate = template()
    private val esRepository = ESRepository(reactiveElasticsearchTemplate, testableApplicationStorageRepository, iGenerator(id))

    @Test
    @Order(1)
    internal fun `save a document on es`() {
        val saveStream = esRepository.save(
                Document(
                        Application("an_app"),
                        FileContent(FileName("a_file", "jpg"), FileContentType(""), ByteArray(0)),
                        Path("/a_path"),
                        DocumentMetadata(mapOf("prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE")
                        )
                )
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
        val stream = esRepository.find(Application("an_app"),
                DocumentMetadata(mapOf("prop1" to "A_VALUE")))
        val verifier = StepVerifier.create(stream)

        verifier.assertNext {
            Assertions.assertEquals(DocumentMetadataPage(listOf(
                    DocumentMetadata(
                            mapOf(
                                    "prop1" to "A_VALUE",
                                    "prop2" to "ANOTHER_VALUE",
                                    "bucket" to "A_BUCKET",
                                    "path" to "/a_path",
                                    "fullQualifiedFilePath" to "/a_path/a_file.jpg",
                                    "fileName" to "a_file",
                                    "extension" to "jpg"
                            )
                    )
            ),
                    0, 10

            ), it)
        }
        verifier.verifyComplete()
    }
}
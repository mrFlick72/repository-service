package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.TestFixture.testableApplicationStorageRepository
import org.junit.jupiter.api.*
import org.springframework.data.elasticsearch.client.ClientConfiguration.builder
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients.create
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.test.StepVerifier
import java.time.LocalDate

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ESRepositoryTest {

    private fun template(): ReactiveElasticsearchTemplate =
            ReactiveElasticsearchTemplate(create(builder().connectedTo("localhost:39200").build()))

    private val randomizer = LocalDate.now().toEpochDay().toString()

    private val reactiveElasticsearchTemplate = template()
    val idGenerator = DocumentMetadataEsIdGenerator()
    private val esRepository = ESRepository(reactiveElasticsearchTemplate, testableApplicationStorageRepository, idGenerator)

    @Test
    @Order(1)
    internal fun `save a document on es`() {
        val document = Document(
                Application("an_app"),
                FileContent(FileName("a_file", "jpg"), FileContentType(""), ByteArray(0)),
                Path("a_path"),
                DocumentMetadata(mapOf("randomizer" to randomizer, "prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE")
                )
        )
        val saveStream = esRepository.save(document)

        val verifier = StepVerifier.create(saveStream)
        verifier.assertNext {
            Assertions.assertEquals(mapOf(
                    "index" to "an_app_indexes",
                    "documentId" to idGenerator.generateId(document.metadataWithSystemMetadataFor((Storage("A_BUCKET"))))
            ), it)
        }
        verifier.verifyComplete()
    }

    @Test
    @Order(2)
    internal fun `get a document on ES`() {
        val stream = esRepository.find(Application("an_app"),
                DocumentMetadata(mapOf("prop1" to "A_VALUE", "randomizer" to randomizer)))
        val verifier = StepVerifier.create(stream)

        verifier.assertNext {
            println(it)
            Assertions.assertEquals(DocumentMetadataPage(listOf(
                    DocumentMetadata(
                            mapOf(
                                    "randomizer" to randomizer,
                                    "prop1" to "A_VALUE",
                                    "prop2" to "ANOTHER_VALUE",
                                    "bucket" to "A_BUCKET",
                                    "path" to "a_path",
                                    "fullQualifiedFilePath" to "A_BUCKET/a_path/a_file.jpg",
                                    "fileName" to "a_file",
                                    "extension" to "jpg"
                            )
                    )
            ),
                    0, 10, 1
            ), it)
        }
        verifier.verifyComplete()
    }
}
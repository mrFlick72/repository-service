package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationName
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch.*
import org.junit.jupiter.api.*
import org.springframework.data.elasticsearch.client.ClientConfiguration.builder
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients.create
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchTemplate
import reactor.test.StepVerifier
import java.time.LocalDate
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class ESRepositoryTest {

    private fun template(): ReactiveElasticsearchTemplate =
            ReactiveElasticsearchTemplate(create(builder().connectedTo("localhost:39200").build()))

    private val randomizer = LocalDate.now().toEpochDay().toString()

    private val reactiveElasticsearchTemplate = template()
    private val idGenerator = DocumentEsIdGenerator()
    private val esRepository = ESRepository(
            DeleteDocumentRepository(reactiveElasticsearchTemplate, idGenerator),
            FindAllDocumentRepository(reactiveElasticsearchTemplate),
            SaveDocumentRepository(reactiveElasticsearchTemplate, idGenerator)
    )

    @Test
    @Order(1)
    internal fun `save a document on es`() {
        val storage = Storage("A_BUCKET")
        val application = Application(ApplicationName("an_app"), storage, Optional.empty())
        val path = Path("a_path")
        val fileName = FileName("a_file", "jpg")
        val document = Document(
                application, FileContent(fileName, FileContentType(""), ByteArray(0)),
                path, DocumentMetadata(mapOf("randomizer" to randomizer, "prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE")
        )
        )
        val saveStream = esRepository.saveDocumentFor(document)
        val writerVerifier = StepVerifier.create(saveStream)
        writerVerifier.expectNext(Unit)
        writerVerifier.verifyComplete()

        val stream =
                esRepository.findDocumentsFor(
                        application,
                        DocumentMetadata(
                                mapOf(
                                        "randomizer" to randomizer,
                                        "fullQualifiedFilePath" to "A_BUCKET/a_path/a_file.jpg"
                                )
                        )
                )
        val readVerifier = StepVerifier.create(stream)

        readVerifier.assertNext {
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
        readVerifier.verifyComplete()
    }
}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationName
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.aFakeDocumentWith
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentFixture.randomizer
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadata
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadataPage
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
        val document = aFakeDocumentWith(randomizer);
        val saveStream = esRepository.saveDocumentFor(document)
        val writerVerifier = StepVerifier.create(saveStream)
        writerVerifier.expectNext(Unit)
        writerVerifier.verifyComplete()

        val stream =
            esRepository.findDocumentsFor(
                Application(ApplicationName("an_app"), Storage("A_BUCKET"), Optional.empty()),
                DocumentMetadata(
                    mapOf(
                        "randomizer" to randomizer,
                        "fullqualifiedfilepath" to "A_BUCKET/a_path/a_file.jpg"
                    )
                )
            )
        val readVerifier = StepVerifier.create(stream)

        readVerifier.assertNext {
            Assertions.assertEquals(
                DocumentMetadataPage(
                    listOf(
                        DocumentMetadata(
                            mapOf(
                                "randomizer" to randomizer,
                                "prop1" to "A_VALUE",
                                "prop2" to "ANOTHER_VALUE",
                                "bucket" to "A_BUCKET",
                                "path" to "a_path",
                                "fullqualifiedfilepath" to "A_BUCKET/a_path/a_file.jpg",
                                "filename" to "a_file",
                                "extension" to "jpg"
                            )
                        )
                    ),
                    0, 10, 1
                ), it
            )
        }
        readVerifier.verifyComplete()
    }
}
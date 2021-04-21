package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationName
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.s3.S3Repository
import reactor.test.StepVerifier
import java.time.LocalDate
import java.util.*

object DocumentFixture {
    val bucket: String = System.getenv("AWS_TESTING_S3_APPLICATION_STORAGE")
    const val objectKey: String = "a_path/a_file.jpg"
    val queueUrl: String = System.getenv("AWS_TESTING_SQS_STORAGE_REINDEX_QUEUE")

    val randomizer = LocalDate.now().toEpochDay().toString()

    fun applicationWith(storage: Storage, applicationName: ApplicationName = ApplicationName("an_app")) =
        Application(applicationName = applicationName, storage, Optional.empty())

    private val storage = Storage("A_BUCKET")
    val application = Application(ApplicationName("an_app"), storage, Optional.empty())

    private val path = Path("a_path")
    private val fileName = FileName("a_file", "jpg")
    fun aFakeDocument(randomizer: String) = Document(
        application, FileContent(fileName, FileContentType(""), ByteArray(0)),
        path, DocumentMetadata(
            mapOf("randomizer" to randomizer, "prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE")
        )
    )

    fun documentMetadata(randomizerValue: String) = DocumentMetadata(
        mapOf(
            "bucket" to bucket,
            "randomizer" to "$randomizerValue",
            "path" to "a_path",
            "filename" to "a_file",
            "extension" to "jpg",
            "fullqualifiedfilepath" to "$bucket/a_path/a_file.jpg",
            "prop1" to "A_VALUE",
            "prop2" to "ANOTHER_VALUE"
        )
    )

    fun aFakeDocumentWith(randomizer: String, application: Application) = Document(
        application, FileContent(fileName, FileContentType(""), ByteArray(0)),
        path, DocumentMetadata(
            mapOf("randomizer" to randomizer, "prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE")
        )
    )

    fun `updates a document on s3 with metadata from`(document: Document, s3Repository: S3Repository) {
        StepVerifier.create(
            s3Repository.saveDocumentFor(
                document
            )
        )
            .expectNext(Unit)
            .verifyComplete()
    }
}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Application
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationName
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import java.time.LocalDate
import java.util.*

object DocumentFixture {
    val randomizer = LocalDate.now().toEpochDay().toString()

    fun applicationWith(storage: Storage) = Application(ApplicationName("an_app"), storage, Optional.empty())

    val storage = Storage("A_BUCKET")
    val application = Application(ApplicationName("an_app"), storage, Optional.empty())

    val path = Path("a_path")
    val fileName = FileName("a_file", "jpg")
    fun aFakeDocument(randomizer: String) = Document(
            application, FileContent(fileName, FileContentType(""), ByteArray(0)),
            path, DocumentMetadata(mapOf("randomizer" to randomizer, "prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE")
    )
    )

    fun aFakeDocumentWith(randomizer: String, application: Application) = Document(
            application, FileContent(fileName, FileContentType(""), ByteArray(0)),
            path, DocumentMetadata(mapOf("randomizer" to randomizer, "prop1" to "A_VALUE", "prop2" to "ANOTHER_VALUE")
    )
    )
}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.Storage

data class Document(
        val application: Application,
        val fileContent: FileContent,
        val path: Path,
        val userDocumentMetadata: UserDocumentMetadata
) {
    companion object {
        fun fullQualifiedFilePathFor(path: Path, fileName: FileName) =
                Document(Application(""),
                        FileContent(fileName, FileContentType(""), ByteArray(0)),
                        path,
                        DocumentMetadata.empty())
                        .fullQualifiedFilePath()

    }

    fun metadataWithSystemMetadataFor(storage: Storage) =
            userDocumentMetadata.content.plus(fileBasedMetadataFor(storage, path, fileContent.fileName))


    private fun fileBasedMetadataFor(storage: Storage, path: Path, fileName: FileName): Map<String, String> =
            mapOf(
                    "fullQualifiedFilePath" to fullQualifiedFilePathFor(storage),
                    "bucket" to storage.bucket,
                    "path" to path.value,
                    "fileName" to fileName.name,
                    "extension" to fileName.extension
            )

    fun fullQualifiedFilePath() =
            "${listOf(path.value, fileContent.fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileContent.fileName.extension}"

    private fun fullQualifiedFilePathFor(storage: Storage) =
            "${listOf(storage.bucket, path.value, fileContent.fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileContent.fileName.extension}"
}

data class Application(val value: String)

data class FileContent(val fileName: FileName, val contentType: FileContentType, val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileContent

        if (fileName != other.fileName) return false
        if (contentType != other.contentType) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}
data class FileName(val name: String, val extension: String) {
    companion object {
        fun fileNameFrom(completeFileName: String): FileName {
            val fileExt = completeFileName.split(".").last()
            println(completeFileName)
            println(fileExt)
            val fileName = completeFileName.removeSuffix(".$fileExt")
            return FileName(fileName, fileExt)
        }
    }
}
data class FileContentType(val value: String)

data class Path(val value: String)

data class DocumentMetadata(val content: Map<String, String>) {
    companion object {
        fun empty() = DocumentMetadata(emptyMap())
    }
}

typealias UserDocumentMetadata = DocumentMetadata


data class DocumentMetadataPage(val documents: List<DocumentMetadata>, val page: Int, val pageSize: Int)
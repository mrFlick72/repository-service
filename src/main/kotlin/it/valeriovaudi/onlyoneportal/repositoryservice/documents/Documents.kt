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
                Document(Application.empty(),
                        FileContent(fileName, FileContentType.empty(), ByteArray(0)),
                        path,
                        DocumentMetadata.empty())
                        .fullQualifiedFilePath()

        fun fileBasedMetadataFor(storage: Storage, path: Path, fileName: FileName): Map<String, String> =
                mapOf(
                        "fullQualifiedFilePath" to fullQualifiedFilePathFor(storage, path, fileName),
                        "bucket" to storage.bucket,
                        "path" to path.value,
                        "fileName" to fileName.name,
                        "extension" to fileName.extension
                )

        fun fullQualifiedFilePathFor(storage: Storage, path: Path, fileName: FileName) =
                "${listOf(storage.bucket, path.value, fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileName.extension}"

    }

    fun metadataWithSystemMetadataFor(storage: Storage) =
            userDocumentMetadata.content.plus(fileBasedMetadataFor(storage, path, fileContent.fileName))


    fun fullQualifiedFilePath() =
            "${listOf(path.value, fileContent.fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileContent.fileName.extension}"

}


data class Application(val value: String) {
    companion object {
        fun empty(): Application = Application("")
    }
}

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

data class FileContentType(val value: String) {
    companion object {
        fun empty(): FileContentType = FileContentType("")
    }
}

data class Path(val value: String)

data class DocumentMetadata(val content: Map<String, String>) {
    companion object {
        fun empty() = DocumentMetadata(emptyMap())
    }
}

typealias UserDocumentMetadata = DocumentMetadata


data class DocumentMetadataPage(val documents: List<DocumentMetadata>, val page: Int, val pageSize: Int, val total: Int)
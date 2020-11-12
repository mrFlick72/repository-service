package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.Storage

data class Document(
        val application: Application,
        val fileContent: FileContent,
        val path: Path,
        val documentMetadata: DocumentMetadata
) {
    fun metadataWithSystemMetadataFor(storage: Storage) =
            documentMetadata.content.plus(fileBasedMetadataFor(storage, path, fileContent.fileName))


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

    fun fullQualifiedFilePathFor(storage: Storage) =
            "${listOf(storage.bucket, path.value, fileContent.fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileContent.fileName.extension}"
}

object DocumentHelper {
    fun metadata(storage: Storage,
                 path: Path,
                 fileName: FileName,
                 documentMetadata: DocumentMetadata) =
            documentMetadata.content.plus(fileBasedMetadataFor(storage, path, fileName))


    fun fileBasedMetadataFor(storage: Storage, path: Path, fileName: FileName): Map<String, String> =
            mapOf(
                    "fullQualifiedFilePath" to s3FullQualifiedFilePath(storage, path, fileName),
                    "bucket" to storage.bucket,
                    "path" to path.value,
                    "fileName" to fileName.name,
                    "extension" to fileName.extension
            )

    fun s3KeyFor(path: Path, fileName: FileName) =
            "${listOf(path.value, fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileName.extension}"

    fun s3FullQualifiedFilePath(storage: Storage, path: Path, fileName: FileName) =
            "${listOf(storage.bucket, path.value, fileName.name).filter { it.isNotBlank() }.joinToString("/")}.${fileName.extension}"
}

data class Application(val value: String)

data class DocumentMetadataPage(val documents: List<DocumentMetadata>, val page: Int, val pageSize: Int)

data class Path(val value: String)
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

data class DocumentMetadata(val content: Map<String, String>) {
    companion object {
        fun empty() = DocumentMetadata(emptyMap())
    }
}

data class FileContentType(val value: String)
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


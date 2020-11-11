package it.valeriovaudi.onlyoneportal.repositoryservice.documents

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


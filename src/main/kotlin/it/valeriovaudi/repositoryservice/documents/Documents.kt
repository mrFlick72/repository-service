package it.valeriovaudi.repositoryservice.documents

data class Application(val value: String)
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

data class FileContentType(val value: String)
data class FileContent(val fileName: FileName, val contentType: FileContentType, val content: ByteArray)
package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.FileName
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Path
import it.valeriovaudi.onlyoneportal.repositoryservice.extentions.toSha256

interface ESIdGenerator<T> {

    fun generateId(criteria: T): String
}

class DocumentMetadataEsIdGenerator() : ESIdGenerator<Map<String, String>> {
    override fun generateId(criteria: Map<String, String>): String =
            criteria.getOrElse("fullQualifiedFilePath") {
                throw RuntimeException("fullQualifiedFilePath field not present as metadata it is required")
            }.toSha256()

}

object DocumentEsIdGenerator : ESIdGenerator<Triple<Storage, Path, FileName>> {
    override fun generateId(criteria: Triple<Storage, Path, FileName>): String =
            Document.Companion.fullQualifiedFilePathFor(criteria.first, criteria.second, criteria.third).toSha256()

}
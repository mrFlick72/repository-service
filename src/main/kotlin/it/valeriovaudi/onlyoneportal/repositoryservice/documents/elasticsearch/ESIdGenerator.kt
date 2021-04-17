package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Document
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.FileName
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Path
import it.valeriovaudi.onlyoneportal.repositoryservice.extentions.toSha256

interface ESIdGenerator<T> {

    fun generateId(criteria: T): String
}

class DocumentEsIdGenerator : ESIdGenerator<Triple<Storage, Path, FileName>> {
    override fun generateId(criteria: Triple<Storage, Path, FileName>): String =
            Document.fullQualifiedFilePathFor(criteria.first, criteria.second, criteria.third).toSha256()
}
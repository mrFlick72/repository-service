package it.valeriovaudi.onlyoneportal.repositoryservice.documents.elasticsearch

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
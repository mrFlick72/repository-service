package it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Application
import java.util.*

data class ApplicationStorageConfig(val application: Application, val storage: Storage, val updateSignals: Optional<UpdateSignals>)
data class UpdateSignals(val sqsQueue: String)
data class Storage(val bucket: String)

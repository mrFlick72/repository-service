package it.valeriovaudi.onlyoneportal.repositoryservice.application

import java.util.*

data class ApplicationName(val value: String) {
    companion object {
        fun empty(): ApplicationName = ApplicationName("")
    }
}

data class Application(val applicationName: ApplicationName, val storage: Storage, val updateSignals: Optional<UpdateSignals>) {
    companion object {
        fun empty(): Application = Application(ApplicationName.empty(), Storage(""), Optional.empty())
    }
}

data class UpdateSignals(val sqsQueue: String)
data class Storage(val bucket: String)

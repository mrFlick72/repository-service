package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageConfig
import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.ApplicationStorageRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage.Storage
import java.util.*

object TestFixture {

    fun iGenerator(id: UUID) = object : ESIdGenerator<Map<String, String>> {
        override fun generateId(criteria: Map<String, String>): String {
            return id.toString()
        }
    }

    val testableApplicationStorageRepository = object : ApplicationStorageRepository {
        override fun storageConfigurationFor(application: Application): Optional<ApplicationStorageConfig> {
            if (application == Application("an_app")) {
                return Optional.of(ApplicationStorageConfig(application, Storage("A_BUCKET"), Optional.empty()))
            } else {
                TODO()
            }
        }

    }
}
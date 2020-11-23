package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationRepository
import it.valeriovaudi.onlyoneportal.repositoryservice.application.ApplicationStorageConfig
import it.valeriovaudi.onlyoneportal.repositoryservice.application.Storage
import java.util.*

object TestFixture {

    val testableApplicationStorageRepository = object : ApplicationRepository {
        override fun storageConfigurationFor(application: Application): Optional<ApplicationStorageConfig> {
            if (application == Application("an_app")) {
                return Optional.of(ApplicationStorageConfig(application, Storage("A_BUCKET"), Optional.empty()))
            } else {
                TODO()
            }
        }

    }
}
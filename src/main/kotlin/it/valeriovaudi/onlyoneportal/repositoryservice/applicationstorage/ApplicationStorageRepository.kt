package it.valeriovaudi.onlyoneportal.repositoryservice.applicationstorage

import it.valeriovaudi.onlyoneportal.repositoryservice.documents.Application
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.*

interface ApplicationStorageRepository {
    fun storageConfigurationFor(application: Application): Optional<ApplicationStorageConfig>
}

class YamlApplicationStorageRepository(private val storage: YamlApplicationStorageMapping) : ApplicationStorageRepository {
    override fun storageConfigurationFor(application: Application) =
            Optional.ofNullable(storage.content[application.value])
                    .map {
                        ApplicationStorageConfig(
                                application,
                                Storage(it.bucket),
                                Optional.ofNullable(it.updateSignalSqsQueue).map(::UpdateSignals)
                        )
                    }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "in-memory.storage")
data class YamlApplicationStorageMapping(val content: Map<String, ApplicationStorageFeature>)

data class ApplicationStorageFeature(val bucket: String, val updateSignalSqsQueue: String? = null)
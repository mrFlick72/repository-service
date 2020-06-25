package it.valeriovaudi.repositoryservice

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.*

interface ApplicationStorageRepository {
    fun storageConfigurationFor(application: Application): Optional<ApplicationStorageConfig>
}

data class ApplicationStorageConfig(val application: Application, val storage: Storage, val updateSignals: Optional<UpdateSignals>)
data class UpdateSignals(val sqsQueue: String)
data class Storage(val bucket: String)

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
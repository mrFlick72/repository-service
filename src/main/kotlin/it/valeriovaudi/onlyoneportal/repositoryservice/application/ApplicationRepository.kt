package it.valeriovaudi.onlyoneportal.repositoryservice.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.*

interface ApplicationRepository {
    fun findApplicationFor(applicationName: ApplicationName): Optional<Application>
    fun findApplicationFor(storage: Storage): Optional<Application>
}

class YamlApplicationRepository(private val configurationStorage: YamlApplicationStorageMapping) :
    ApplicationRepository {
    override fun findApplicationFor(applicationName: ApplicationName) =
        Optional.ofNullable(configurationStorage.content[applicationName.value])
            .map {
                Application(
                    applicationName,
                    Storage(it.bucket),
                    Optional.ofNullable(it.updateSignalSqsQueue).map(::UpdateSignals)
                )
            }

    override fun findApplicationFor(storage: Storage): Optional<Application> {
        return Optional.ofNullable(
            configurationStorage.content.entries.find { it.value.bucket == storage.bucket }
        ).map {
            Application(
                ApplicationName(it.key),
                Storage(it.value.bucket),
                Optional.ofNullable(it.value.updateSignalSqsQueue).map(::UpdateSignals)
            )
        }

    }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "in-memory.storage")
data class YamlApplicationStorageMapping(val content: Map<String, ApplicationStorageFeature>)

data class ApplicationStorageFeature(val bucket: String, val updateSignalSqsQueue: String? = null)
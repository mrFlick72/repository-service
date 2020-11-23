package it.valeriovaudi.onlyoneportal.repositoryservice.application

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.*

interface ApplicationRepository {
    fun findApplicationFor(applicationName: ApplicationName): Optional<Application>
}

class YamlApplicationRepository(private val storage: YamlApplicationStorageMapping) : ApplicationRepository {
    override fun findApplicationFor(applicationName: ApplicationName) =
            Optional.ofNullable(storage.content[applicationName.value])
                    .map {
                        Application(
                                applicationName,
                                Storage(it.bucket),
                                Optional.ofNullable(it.updateSignalSqsQueue).map(::UpdateSignals)
                        )
                    }
}

@ConstructorBinding
@ConfigurationProperties(prefix = "in-memory.storage")
data class YamlApplicationStorageMapping(val content: Map<String, ApplicationStorageFeature>)

data class ApplicationStorageFeature(val bucket: String, val updateSignalSqsQueue: String? = null)
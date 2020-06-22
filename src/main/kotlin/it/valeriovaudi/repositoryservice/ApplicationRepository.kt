package it.valeriovaudi.repositoryservice

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.*

interface ApplicationStorageRepository {
    fun storageConfigurationFor(application: Application): Optional<ApplicationStorageConfig>
}

data class ApplicationStorageConfig(val application: Application, val bucket: Storage)

sealed class Storage
data class S3Bucket(val name: String) : Storage()


class YamlApplicationStorageRepository(private val storage : YamlApplicationStorageStorage) : ApplicationStorageRepository {
    override fun storageConfigurationFor(application: Application) =
        Optional.ofNullable(storage.content[application.value])
                .map { ApplicationStorageConfig(application, S3Bucket(it)) }

}

@ConstructorBinding
@ConfigurationProperties(prefix = "in-memory.storage")
data class YamlApplicationStorageStorage(val content : Map<String, String>)
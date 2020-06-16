package it.valeriovaudi.repositoryservice

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router

@Configuration
class DocumentEndPoint(private val documentRepository: DocumentRepository) {

    @Bean
    fun messageEndPointRoute() =
            router {
                GET("/documents/{application}") {
                    val fileName = it.queryParam("fileName").orElse("")
                    val fileExtension = it.queryParam("fileExt").orElse("")
                    documentRepository.findOneDocumentFor(
                            Application(it.pathVariable("application")),
                            it.queryParam("path").map { Path(it) }.orElse(Path("")),
                            FileName(fileName, fileExtension)
                    ).flatMap { fileContent ->
                        ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$fileName.$fileExtension")
                                .contentType(MediaType.valueOf(fileContent.contentType.value))
                                .body(BodyInserters.fromValue(fileContent.content))
                    }
                }
            }
}
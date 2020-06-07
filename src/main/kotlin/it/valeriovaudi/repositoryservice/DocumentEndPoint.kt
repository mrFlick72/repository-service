package it.valeriovaudi.repositoryservice

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router

@Configuration
class DocumentEndPoint(private val documentRepository: DocumentRepository) {

    @Bean
    fun messageEndPointRoute() =
            router {
                GET("/documents/{application}") {
                    documentRepository.findOneDocumentFor(
                            Application(it.pathVariable("application")),
                            it.queryParam("path").map { Path(it) }.orElse(Path("")),
                            FileName(
                                    it.queryParam("fileName").orElse(""),
                                    it.queryParam("fileExt").orElse("")
                            )
                    ).flatMap { messages -> ok().body(BodyInserters.fromValue(messages)) }
                }
            }
}
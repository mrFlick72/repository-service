package it.valeriovaudi.repositoryservice.documents

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.nio.charset.Charset

@Configuration
class DocumentEndPoint(private val documentRepository: DocumentRepository) {

    @Bean
    fun messageEndPointRoute() =
            router {
                GET("/documents/{application}") {
                    val fileName = it.queryParam("fileName").orElse("")
                    val fileExtension = it.queryParam("fileExt").orElse("")
                    val path = it.queryParam("path").map { Path(it) }.orElse(Path(""))
                    documentRepository.findOneDocumentFor(
                            Application(it.pathVariable("application")),
                            path,
                            FileName(fileName, fileExtension)
                    ).flatMap { fileContent ->
                        ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$fileName.$fileExtension")
                                .contentType(MediaType.valueOf(fileContent.contentType.value))
                                .body(BodyInserters.fromValue(fileContent.content))
                    }.switchIfEmpty(notFound().build())
                }

                PUT("/documents/{application}", contentType(MediaType.MULTIPART_FORM_DATA)) { requesrt ->
                    val application = Application(requesrt.pathVariable("application"))

                    requesrt.multipartData().flatMap {
                        val path = it["path"]?.get(0)!!.content().map { Path(it.toString(Charset.defaultCharset())) }.toMono()
                        val file = (it["file"]?.get(0)!! as FilePart).toMono()
                        val fileContent = (it["file"]?.get(0)!! as FilePart).content().toMono()
                        Mono.zip(path, file, fileContent)
                    }.map { t ->
                        val filePart = t.t2
                        Pair(t.t1,
                                FileContent(
                                        fileName = FileName.fileNameFrom(filePart.filename()),
                                        contentType = FileContentType(filePart.headers().contentType.toString()),
                                        content = t.t3.asInputStream().readAllBytes()
                                )
                        )
                    }.flatMap { documentRepository.saveDocumentFor(application, it.first, it.second) }
                            .flatMap { noContent().build() }
                }
            }
}
package it.valeriovaudi.onlyoneportal.repositoryservice.documents

import com.fasterxml.jackson.databind.ObjectMapper
import it.valeriovaudi.onlyoneportal.repositoryservice.documents.DocumentMetadataPageRepresentation.Companion.fromDomainToRepresentation
import it.valeriovaudi.onlyoneportal.repositoryservice.extentions.queryParamExtractor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters.fromValue
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.nio.charset.Charset

@Configuration
class DocumentEndPoint(private val documentRepository: DocumentRepository) {

    @Bean
    fun messageEndPointRoute(objectMapper: ObjectMapper) =
            router {
                PUT("/documents/{application}", contentType(MediaType.MULTIPART_FORM_DATA)) { requester ->
                    val application = Application(requester.pathVariable("application"))
                    requester.multipartData().flatMap {
                        val metadata = it["metadata"].orEmpty().first().content()
                                .map { it.toString(Charset.defaultCharset()) }
                                .toMono()
                                .map { objectMapper.readValue(it, Map::class.java) }
                                .map { query -> query as Map<String, String> }

                        val path = it["path"]?.get(0)!!.content().map { Path(it.toString(Charset.defaultCharset())) }.toMono()
                        val file = (it["file"]?.get(0)!! as FilePart).toMono()
                        val fileContent = (it["file"]?.get(0)!! as FilePart).content().toMono()
                        Mono.zip(path, file, fileContent, metadata)
                    }.map { t ->
                        val filePart = t.t2
                        Triple(FileContent(
                                fileName = FileName.fileNameFrom(filePart.filename()),
                                contentType = FileContentType(filePart.headers().contentType.toString()),
                                content = t.t3.asInputStream().readAllBytes()
                        ), t.t1, t.t4)
                    }.flatMap { documentRepository.saveDocumentFor(Document(application, it.first, it.second, DocumentMetadata(it.third))) }
                            .flatMap { noContent().build() }
                }

                PUT("/documents/{application}") {
                    val application = Application(it.pathVariable("application"))
                    val queryAsMap = it.body(BodyExtractors.toMono(Map::class.java))

                    queryAsMap.flatMap { query -> documentRepository.findDocumentsFor(application, DocumentMetadata(query as Map<String, String>)) }
                            .map(::fromDomainToRepresentation)
                            .flatMap { representation -> ok().body(fromValue(representation)) }

                }

                GET("/documents/{application}") {
                    val fileName = it.queryParamExtractor("fileName")
                    val fileExtension = it.queryParamExtractor("fileExt")
                    val path = it.queryParamExtractor("path", Path::class.java)
                    val application = it.pathVariable("application")

                    documentRepository.findOneDocumentFor(Application(application), path, FileName(fileName, fileExtension)
                    ).flatMap { fileContent ->
                        ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$fileName.$fileExtension")
                                .contentType(MediaType.valueOf(fileContent.contentType.value))
                                .body(fromValue(fileContent.content))
                    }.switchIfEmpty(notFound().build())
                }

                DELETE("/documents/{application}") {
                    val fileName = it.queryParamExtractor("fileName")
                    val fileExtension = it.queryParamExtractor("fileExt")
                    val path = it.queryParamExtractor("path", Path::class.java)
                    val application = it.pathVariable("application")

                    documentRepository.deleteDocumentFor(Application(application), path, FileName(fileName, fileExtension)
                    ).flatMap { noContent().build() }
                            .switchIfEmpty(notFound().build())
                }
            }
}

data class DocumentMetadataPageRepresentation(val documents: List<Map<String, String>>, val page: Int, val pageSize: Int, val total: Int) {
    companion object {
        fun fromDomainToRepresentation(domain: DocumentMetadataPage): DocumentMetadataPageRepresentation =
                DocumentMetadataPageRepresentation(
                        domain.documents.map { it.content.filter { it.key != "bucket" } },
                        domain.page, domain.pageSize, domain.total)
    }
}
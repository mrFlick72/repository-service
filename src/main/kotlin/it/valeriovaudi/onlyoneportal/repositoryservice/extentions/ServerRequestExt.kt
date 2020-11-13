package it.valeriovaudi.onlyoneportal.repositoryservice.extentions

import org.springframework.web.reactive.function.server.ServerRequest

fun ServerRequest.queryParamExtractor(paramName: String, defaultValue: String = ""): String =
        this.queryParam(paramName).orElse(defaultValue)

fun <T> ServerRequest.queryParamExtractor(paramName: String, clazz: Class<T>): T =
        this.queryParam(paramName)
                .map { clazz.getDeclaredConstructor().newInstance(it) }.orElse(clazz.getDeclaredConstructor().newInstance(""))


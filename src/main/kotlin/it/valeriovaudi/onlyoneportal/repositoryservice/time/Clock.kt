package it.valeriovaudi.onlyoneportal.repositoryservice.time

import java.time.Instant
import java.time.LocalDateTime

class Clock {
    fun now() = TimeStamp.now()
}

data class TimeStamp(val localDateTime: LocalDateTime) {
    companion object {
        fun now(): TimeStamp = TimeStamp(LocalDateTime.now())
        fun nowInMilliSecondsAsString(): String = Instant.now().toEpochMilli().toString()
    }
}
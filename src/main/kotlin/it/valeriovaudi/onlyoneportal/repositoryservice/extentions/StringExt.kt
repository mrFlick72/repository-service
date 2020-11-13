package it.valeriovaudi.onlyoneportal.repositoryservice.extentions

import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

val logger = LoggerFactory.getLogger(StringExt::class.java.name)

class StringExt


fun String.toSha256(): String {
    lateinit var messageDigest: MessageDigest
    try {
        messageDigest = MessageDigest.getInstance("SHA-256")
    } catch (e: NoSuchAlgorithmException) {
        logger.error(e.message, e)
    }

    val digest = messageDigest.digest(this.toByteArray())
    return bytesToHex(digest)
}

private fun bytesToHex(bytes: ByteArray): String {
    val sb = StringBuilder()
    for (b in bytes) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString()
}

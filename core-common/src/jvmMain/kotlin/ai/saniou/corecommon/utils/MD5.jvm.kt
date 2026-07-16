package ai.saniou.corecommon.utils

import java.security.MessageDigest

actual fun String.toMD5(): String = encodeToByteArray().toMD5()

actual fun ByteArray.toMD5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(this)
    return digest.joinToString("") { "%02x".format(it) }
}

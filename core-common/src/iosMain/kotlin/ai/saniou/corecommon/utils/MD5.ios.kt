package ai.saniou.corecommon.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSMutableData
import platform.Foundation.create
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import kotlinx.cinterop.get

@OptIn(ExperimentalForeignApi::class)
actual fun String.toMD5(): String {
    return encodeToByteArray().toMD5()
}

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.toMD5(): String {
    if (isEmpty()) return "d41d8cd98f00b204e9800998ecf8427e"
    val digest = NSMutableData.create(length = CC_MD5_DIGEST_LENGTH.toULong()) ?: return ""

    usePinned { data ->
        digest.mutableBytes?.let { digestBytes ->
            CC_MD5(data.addressOf(0), size.toUInt(), digestBytes.reinterpret())
        }
    }

    val out = StringBuilder()
    val ptr = digest.bytes?.reinterpret<kotlinx.cinterop.ByteVar>()
    if (ptr != null) {
        for (i in 0 until CC_MD5_DIGEST_LENGTH) {
            val byte = ptr[i].toInt()
            val hex = (byte and 0xFF).toString(16)
            if (hex.length == 1) {
                out.append('0')
            }
            out.append(hex)
        }
    }
    return out.toString()
}

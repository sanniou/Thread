package ai.saniou.corecommon.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import kotlinx.cinterop.get

@OptIn(ExperimentalForeignApi::class)
actual fun String.toMD5(): String {
    val data = (this as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return ""
    val digest = NSMutableData.create(length = CC_MD5_DIGEST_LENGTH.toULong()) ?: return ""

    data.bytes?.let { dataBytes ->
        digest.mutableBytes?.let { digestBytes ->
            CC_MD5(dataBytes, data.length.toUInt(), digestBytes.reinterpret())
        }
    }

    val out = StringBuilder()
    val ptr = digest.bytes?.reinterpret<kotlinx.cinterop.ByteVar>()
    if (ptr != null) {
        for (i in 0 until CC_MD5_DIGEST_LENGTH) {
            // ptr is a CPointer, we can use array access syntax []
            // which is an operator get extension on CPointer.
            // However, the error suggests receiver type mismatch or unresolved reference for 'get'.
            // In recent Kotlin Native versions, pointer arithmetic/access might need explicit helper or `ptr[i]` works if imported correctly.
            // Let's use `ptr[i]` but ensure imports or try pointer arithmetic if needed.
            // Actually, `ptr[i]` is correct for CPointer<ByteVar>.
            // The error "MatchGroupCollection.get" suggests it's trying to resolve to something else entirely, likely due to missing imports or type inference issues.
            // It seems standard CPointer get is not being found.
            // We need: import kotlinx.cinterop.get
            
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

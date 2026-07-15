package ai.saniou.thread.data.storage

import ai.saniou.thread.data.platform.AndroidPlatformContext

actual fun getStorageDirectory(): String {
    return AndroidPlatformContext.requireContext().filesDir.absolutePath
}

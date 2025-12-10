package ai.saniou.thread.data.storage

import java.io.File

actual fun getStorageDirectory(): String {
    val homeDir = System.getProperty("user.home")
    val storageDir = File(homeDir, ".thread")
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }
    return storageDir.absolutePath
}

package ai.saniou.thread.data.storage

import android.content.Context
import ai.saniou.nmb.NmbApplication


actual fun getStorageDirectory(): String {
    val context = NmbApplication.instance
    return context.filesDir.absolutePath
}

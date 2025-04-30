package ai.saniou.nmb.data.storage

import android.content.Context
import ai.saniou.nmb.NmbApplication


actual fun getStorageDirectory(): String {
    val context = NmbApplication.instance
    return context.filesDir.absolutePath
}

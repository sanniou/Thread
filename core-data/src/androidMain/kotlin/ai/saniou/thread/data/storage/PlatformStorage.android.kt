package ai.saniou.thread.data.storage

import android.content.Context
import ai.saniou.forum.ForumApplication


actual fun getStorageDirectory(): String {
    val context = NmbApplication.instance
    return context.filesDir.absolutePath
}

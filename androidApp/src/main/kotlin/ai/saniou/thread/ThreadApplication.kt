package ai.saniou.thread

import ai.saniou.thread.data.platform.AndroidPlatformContext
import android.app.Application

class ThreadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatformContext.initialize(this)
    }
}

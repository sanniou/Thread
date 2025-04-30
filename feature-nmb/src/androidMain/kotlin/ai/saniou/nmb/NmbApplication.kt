package ai.saniou.nmb

import android.app.Application

/**
 * NMB 应用程序类
 */
class NmbApplication : Application() {
    
    companion object {
        lateinit var instance: NmbApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

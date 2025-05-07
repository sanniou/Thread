package ai.saniou.nmb

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import ai.saniou.nmb.data.image.ImageLoaderFactory

/**
 * NMB 应用程序类
 * 用于初始化全局组件
 */
class NmbApplication : Application() {

    companion object {
        lateinit var instance: NmbApplication
            private set
    }

    @OptIn(ExperimentalCoilApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

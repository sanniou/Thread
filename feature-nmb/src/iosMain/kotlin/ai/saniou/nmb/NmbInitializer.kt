package ai.saniou.nmb

import ai.saniou.nmb.data.image.ImageLoaderFactory
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi

/**
 * iOS平台的初始化器
 */
@OptIn(ExperimentalCoilApi::class)
object NmbInitializer {
    /**
     * 初始化应用
     * 在iOS平台上，这个方法应该在应用启动时调用
     */
    fun initialize() {
        // 初始化Coil
        SingletonImageLoader.setInstance(ImageLoaderFactory.getImageLoader())
    }
}

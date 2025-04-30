package ai.saniou.nmb.data.image

import ai.saniou.nmb.NmbApplication
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcher
import coil3.request.CachePolicy
import coil3.util.DebugLogger
import okhttp3.OkHttpClient
import java.io.File

/**
 * Android平台的图片加载器工厂实现
 */
@OptIn(ExperimentalCoilApi::class)
actual object ImageLoaderFactory {
    private var imageLoader: ImageLoader? = null
    
    /**
     * 获取单例ImageLoader实例
     */
    actual fun getImageLoader(): ImageLoader {
        if (imageLoader == null) {
            val context = NmbApplication.instance
            val cacheDirectory = File(context.cacheDir, "image_cache")
            
            imageLoader = ImageLoader.Builder(context)
                .networkFetcher(OkHttpNetworkFetcher(OkHttpClient.Builder().build()))
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDirectory)
                        .maxSizeBytes(50 * 1024 * 1024) // 50MB
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .respectCacheHeaders(false) // 忽略服务器的缓存控制头
                .logger(DebugLogger())
                .build()
        }
        
        return imageLoader!!
    }
    
    /**
     * 清除图片缓存
     */
    actual suspend fun clearCache() {
        imageLoader?.diskCache?.clear()
        imageLoader?.memoryCache?.clear()
    }
}

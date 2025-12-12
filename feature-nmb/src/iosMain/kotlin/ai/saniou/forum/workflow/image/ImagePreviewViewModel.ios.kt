package ai.saniou.forum.workflow.image

import ai.saniou.thread.data.manager.CdnManager
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

/**
 * iOS平台保存图片实现
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun ImagePreviewViewModel.saveImage(di: DI, imgPath: String, ext: String) {
    val cdnManager: CdnManager by di.instance()

    // 构建完整的图片URL
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, false)

    withContext(Dispatchers.Default) {
        try {
            // 创建NSURL
            val url = NSURL.URLWithString(imageUrl) ?: throw Exception("无效的URL")

            // 下载图片数据
            val data = NSData.dataWithContentsOfURL(url) ?: throw Exception("无法下载图片")

            // 创建UIImage
            val image = UIImage.imageWithData(data) ?: throw Exception("无法创建图片")

            // 保存到相册
            UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        } catch (e: Exception) {
            throw Exception("保存图片失败: ${e.message}")
        }
    }
}

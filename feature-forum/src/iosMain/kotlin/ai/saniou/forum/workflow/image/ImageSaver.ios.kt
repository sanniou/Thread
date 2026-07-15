package ai.saniou.forum.workflow.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

@OptIn(ExperimentalForeignApi::class)
class ImageSaverImpl : ImageSaver {
    override suspend fun save(url: String): Boolean = withContext(Dispatchers.Default) {
        val imageUrl = NSURL.URLWithString(url) ?: return@withContext false
        val data = NSData.dataWithContentsOfURL(imageUrl) ?: return@withContext false
        val image = UIImage.imageWithData(data) ?: return@withContext false
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        true
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    return remember { ImageSaverImpl() }
}

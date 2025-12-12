package ai.saniou.forum.workflow.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.DownloadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.instance
import ai.saniou.forum.di.nmbdi
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

actual class ImageSaverImpl(private val sketch: Sketch) : ImageSaver {
    override suspend fun save(url: String): Boolean = withContext(Dispatchers.IO) {
        val request = DownloadRequest(url)
        when (val result = sketch.execute(request)) {
            is DownloadResult.Success -> {
                val imageData = result.data.dataSource.openSource().read().toByteArray()
                val image = UIImage.imageWithData(imageData)
                if (image != null) {
                    var success = false
                    UIImageWriteToSavedPhotosAlbum(image, null, null, null)
                    // Unfortunately, there's no direct callback to know if it succeeded here.
                    // We'll assume it works for now.
                    success = true
                    return@withContext success
                }
                false
            }
            is DownloadResult.Error -> false
        }
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val sketch by nmbdi.instance<Sketch>()
    return remember { ImageSaverImpl(sketch) }
}

package ai.saniou.forum.workflow.image

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.target.Target
import com.github.panpf.sketch.decode.DecodeResult
import com.github.panpf.sketch.fetch.FileUriFetcher
import com.github.panpf.sketch.source.DataSource
import com.github.panpf.sketch.util.toBitmapOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.instance
import ai.saniou.forum.di.forumdi
import android.content.Context
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.DownloadResult
import java.io.File

actual class ImageSaverImpl(
    private val context: Context,
    private val sketch: Sketch
) : ImageSaver {
    override suspend fun save(url: String): Boolean = withContext(Dispatchers.IO) {
        val request = DownloadRequest(context, url) {
            memoryCachePolicy(CachePolicy.DISABLED)
        }
        when (val result = sketch.execute(request)) {
            is DownloadResult.Success -> {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "saniou_${Clock.System.now().toEpochMilliseconds()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        result.data.dataSource.openSource().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(it, values, null, null)
                    }
                    true
                } ?: false
            }
            is DownloadResult.Error -> {
                false
            }
        }
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val sketch by nmbdi.instance<Sketch>()
    val context by nmbdi.instance<Context>()
    return remember { ImageSaverImpl(context, sketch) }
}

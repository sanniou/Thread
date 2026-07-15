package ai.saniou.forum.workflow.image

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.time.Clock

class ImageSaverImpl(
    private val context: Context,
) : ImageSaver {
    override suspend fun save(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val extension = url.substringBefore('?').substringAfterLast('.', "jpg")
                .takeIf { it.length in 2..5 } ?: "jpg"
            val values = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "thread_${Clock.System.now().toEpochMilliseconds()}.$extension",
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/$extension")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create image in MediaStore")
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    URL(url).openStream().use { input -> input.copyTo(output) }
                } ?: error("Unable to open image output stream")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
        }.isSuccess
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    val context = LocalContext.current
    return remember(context) { ImageSaverImpl(context) }
}

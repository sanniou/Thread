package ai.saniou.forum.workflow.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import org.jetbrains.compose.resources.getString
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_a927c74919

class ImageSaverImpl : ImageSaver {
    override suspend fun save(url: String): Boolean {
        val dialogTitle = getString(Res.string.s_a927c74919)
        return withContext(Dispatchers.IO) {
            val extension = url.substringBefore('?').substringAfterLast('.', "jpg")
                .takeIf { it.length in 2..5 } ?: "jpg"
            val chooser = JFileChooser().apply {
                this.dialogTitle = dialogTitle
                selectedFile = File("thread-image.$extension")
            }
            if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                return@withContext false
            }
            runCatching {
                val target = chooser.selectedFile.let { file ->
                    if (file.extension.isBlank()) File(file.parentFile, "${file.name}.$extension") else file
                }
                URI(url).toURL().openStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }.isSuccess
        }
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    return remember { ImageSaverImpl() }
}

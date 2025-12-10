package ai.saniou.nmb.workflow.image

import ai.saniou.thread.data.manager.CdnManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

/**
 * Android平台保存图片实现
 */
actual suspend fun ImagePreviewViewModel.saveImage(imgPath: String, ext: String) {
    val cdnManager: CdnManager by di.instance()
    val context: Context by di.instance()

    // 构建完整的图片URL
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, false)

    withContext(Dispatchers.IO) {
        try {
            // 创建文件名
            val fileName = "NMB_${imgPath.replace("/", "_")}$ext"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用MediaStore API
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(ext))
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/NMB"
                    )
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: throw Exception("创建文件失败")

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    downloadImage(imageUrl, outputStream)
                } ?: throw Exception("无法打开输出流")
            } else {
                // Android 9及以下使用传统File API
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val nmbDir = File(imagesDir, "NMB")
                if (!nmbDir.exists()) {
                    nmbDir.mkdirs()
                }

                val file = File(nmbDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    downloadImage(imageUrl, outputStream)
                }
            }
        } catch (e: Exception) {
            throw Exception("保存图片失败: ${e.message}")
        }
    }
}

/**
 * 下载图片
 */
private suspend fun downloadImage(imageUrl: String, outputStream: OutputStream) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connect()

            val inputStream: InputStream = connection.getInputStream()
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
            inputStream.close()
        } catch (e: Exception) {
            throw Exception("下载图片失败: ${e.message}")
        }
    }
}

/**
 * 获取MIME类型
 */
private fun getMimeType(ext: String): String {
    return when (ext.lowercase()) {
        ".jpg", ".jpeg" -> "image/jpeg"
        ".png" -> "image/png"
        ".gif" -> "image/gif"
        ".webp" -> "image/webp"
        else -> "image/jpeg"
    }
}

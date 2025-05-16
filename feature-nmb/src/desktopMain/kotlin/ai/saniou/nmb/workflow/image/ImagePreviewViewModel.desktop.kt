package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * 桌面平台保存图片实现
 */
actual suspend fun ImagePreviewViewModel.saveImage(di: DI, imgPath: String, ext: String) {
    val cdnManager: CdnManager by di.instance()

    // 构建完整的图片URL
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, false)

    withContext(Dispatchers.IO) {
        try {
            // 创建文件名
            val fileName = "NMB_${imgPath.replace("/", "_")}$ext"

            // 打开文件选择器
            val fileChooser = JFileChooser().apply {
                dialogTitle = "保存图片"
                fileSelectionMode = JFileChooser.FILES_ONLY
                selectedFile = File(fileName)
                fileFilter = FileNameExtensionFilter(
                    "图片文件 (${ext.substring(1)})",
                    ext.substring(1)
                )
            }

            val result = fileChooser.showSaveDialog(null)

            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFile = fileChooser.selectedFile

                // 确保文件有正确的扩展名
                val file = if (!selectedFile.name.endsWith(ext, ignoreCase = true)) {
                    File(selectedFile.absolutePath + ext)
                } else {
                    selectedFile
                }

                // 下载并保存图片
                val url = URL(imageUrl)
                val connection = url.openConnection()
                connection.connect()

                val inputStream: InputStream = connection.getInputStream()
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } else {
                // 用户取消了保存操作
                throw Exception("保存操作已取消")
            }
        } catch (e: Exception) {
            throw Exception("保存图片失败: ${e.message}")
        }
    }
}

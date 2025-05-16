package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.manager.CdnManager
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.instance
import org.w3c.dom.HTMLAnchorElement

/**
 * Web平台保存图片实现
 */
actual suspend fun ImagePreviewViewModel.saveImage(imgPath: String, ext: String) {
    val cdnManager: CdnManager by di.instance()

    // 构建完整的图片URL
    val imageUrl = cdnManager.buildImageUrl(imgPath, ext, false)

    withContext(Dispatchers.Default) {
        try {
            // 创建一个临时的a标签用于下载
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = imageUrl
            link.download = "NMB_${imgPath.replace("/", "_")}$ext"

            // 添加到文档中并触发点击
            document.body?.appendChild(link)
            link.click()

            // 清理
            document.body?.removeChild(link)
        } catch (e: Exception) {
            throw Exception("保存图片失败")
        }
    }
}

package ai.saniou.nmb.workflow.image

import ai.saniou.nmb.data.NmbScreen
import ai.saniou.nmb.workflow.home.NavigationDestination
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 图片预览页面导航目标
 */
@OptIn(ExperimentalEncodingApi::class)
object ImagePreviewNavigationDestination : NavigationDestination {
    override val route = NmbScreen.ImagePreview.name
    const val imgPathArg = "imgPath"
    const val extArg = "ext"
    val routeWithArgs = "$route/{$imgPathArg}/{$extArg}"

    /**
     * 创建完整的导航路径
     * 对路径参数进行 Base64 编码，避免特殊字符导致的问题
     */
    fun createRoute(imgPath: String, ext: String): String {
        // 使用 Base64 编码，更加通用且兼容所有平台
        val encodedImgPath = Base64.encode(imgPath.encodeToByteArray())
        val encodedExt = Base64.encode(ext.encodeToByteArray())
        return "$route/$encodedImgPath/$encodedExt"
    }

    /**
     * 解码路径参数
     */
    fun decodePath(encodedPath: String): String {
        return try {
            val decodedBytes = Base64.decode(encodedPath)
            decodedBytes.decodeToString()
        } catch (e: Exception) {
            // 解码失败时返回原始字符串
            encodedPath
        }
    }
}

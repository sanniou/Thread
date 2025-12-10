package ai.saniou.thread.data.storage

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS 平台的存储路径提供者
 */

actual fun getStorageDirectory(): String {
    // 使用 NSSearchPathForDirectoriesInDomains 获取文档目录
    val paths = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    )

    // 返回第一个路径，如果没有则返回空字符串
    return (paths.firstOrNull() as? String) ?: ""
}

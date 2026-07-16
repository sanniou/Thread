package ai.saniou.coreui.state

import ai.saniou.thread.domain.refresh.FailureClassifier
import ai.saniou.thread.domain.refresh.RefreshFailureKind

/**
 * 错误类型枚举
 */
enum class AppErrorType {
    NETWORK,
    SERVER,
    AUTHENTICATION,
    RATE_LIMIT,
    UNKNOWN
}

/**
 * 统一的应用错误模型
 * 用于在 UI 层展示错误状态
 *
 * @property type 错误类型，用于决定展示什么样的错误页（如网络错误、服务器错误）
 * @property message 错误提示信息
 * @property throwable 原始异常
 * @property onRetry 重试回调
 */
data class AppError(
    val type: AppErrorType = AppErrorType.UNKNOWN,
    val message: String,
    val throwable: Throwable? = null,
    val onRetry: (() -> Unit)? = null
)

fun Throwable.toAppError(onRetry: (() -> Unit)? = null): AppError {
    val kind = FailureClassifier.classify(this)
    val type = when (kind) {
        RefreshFailureKind.OFFLINE, RefreshFailureKind.TIMEOUT -> AppErrorType.NETWORK
        RefreshFailureKind.AUTHENTICATION -> AppErrorType.AUTHENTICATION
        RefreshFailureKind.RATE_LIMIT -> AppErrorType.RATE_LIMIT
        RefreshFailureKind.REMOTE -> AppErrorType.SERVER
        RefreshFailureKind.UNKNOWN -> AppErrorType.UNKNOWN
    }
    val userMessage = when (kind) {
        RefreshFailureKind.OFFLINE -> "网络不可用，请检查连接"
        RefreshFailureKind.TIMEOUT -> "请求超时，请稍后重试"
        RefreshFailureKind.AUTHENTICATION -> "登录状态已失效，请重新登录"
        RefreshFailureKind.RATE_LIMIT -> "请求过于频繁，请稍后再试"
        RefreshFailureKind.REMOTE -> message ?: "远端服务暂时不可用"
        RefreshFailureKind.UNKNOWN -> message ?: "操作失败，请稍后重试"
    }
    return AppError(type, userMessage, this, onRetry)
}

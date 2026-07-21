package ai.saniou.coreui.state

import ai.saniou.thread.domain.refresh.FailureClassifier
import ai.saniou.thread.domain.refresh.RefreshFailureKind
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import thread.core_ui.generated.resources.Res
import thread.core_ui.generated.resources.s_d656314c49
import thread.core_ui.generated.resources.s_2a975f5214
import thread.core_ui.generated.resources.s_597b79526b
import thread.core_ui.generated.resources.s_14c9516e3f
import thread.core_ui.generated.resources.s_56fe93471e
import thread.core_ui.generated.resources.s_891d3990d9

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
 * @property message 错误提示信息（远端原文或已解析文案）
 * @property throwable 原始异常
 * @property onRetry 重试回调
 * @property messageRes 可选本地化资源；在 UI 中优先于 [message]
 */
data class AppError(
    val type: AppErrorType = AppErrorType.UNKNOWN,
    val message: String = "",
    val throwable: Throwable? = null,
    val onRetry: (() -> Unit)? = null,
    val messageRes: StringResource? = null,
)

@Composable
fun AppError.localizedMessage(): String {
    messageRes?.let { return stringResource(it) }
    return message
}

fun Throwable.toAppError(onRetry: (() -> Unit)? = null): AppError {
    val kind = FailureClassifier.classify(this)
    val type = when (kind) {
        RefreshFailureKind.OFFLINE, RefreshFailureKind.TIMEOUT -> AppErrorType.NETWORK
        RefreshFailureKind.AUTHENTICATION -> AppErrorType.AUTHENTICATION
        RefreshFailureKind.RATE_LIMIT -> AppErrorType.RATE_LIMIT
        RefreshFailureKind.REMOTE -> AppErrorType.SERVER
        RefreshFailureKind.UNKNOWN -> AppErrorType.UNKNOWN
    }
    val res = when (kind) {
        RefreshFailureKind.OFFLINE -> Res.string.s_d656314c49
        RefreshFailureKind.TIMEOUT -> Res.string.s_2a975f5214
        RefreshFailureKind.AUTHENTICATION -> Res.string.s_597b79526b
        RefreshFailureKind.RATE_LIMIT -> Res.string.s_14c9516e3f
        RefreshFailureKind.REMOTE -> if (message.isNullOrBlank()) Res.string.s_56fe93471e else null
        RefreshFailureKind.UNKNOWN -> if (message.isNullOrBlank()) Res.string.s_891d3990d9 else null
    }
    val userMessage = when (kind) {
        RefreshFailureKind.REMOTE, RefreshFailureKind.UNKNOWN -> message.orEmpty()
        else -> ""
    }
    return AppError(type, userMessage, this, onRetry, res)
}

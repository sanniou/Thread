package ai.saniou.coreui.state

/**
 * 错误类型枚举
 */
enum class AppErrorType {
    NETWORK,
    SERVER,
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

/**
 * 将 Throwable 转换为 AppError
 *
 * 这里可以根据具体的异常类型进行映射，例如：
 * - IOException -> AppErrorType.NETWORK
 * - HttpException -> AppErrorType.SERVER
 */
fun Throwable.toAppError(onRetry: (() -> Unit)? = null): AppError {
    // TODO: 根据实际使用的网络库（如 Ktor/Retrofit）添加具体的异常判断
    // 目前简单粗暴地根据是否是 IO 异常来判断是否是网络错误
    // 实际项目中应该根据项目依赖的具体异常类型进行判断
    val isNetworkError = this::class.simpleName?.contains("UnknownHostException") == true ||
            this::class.simpleName?.contains("ConnectException") == true ||
            this::class.simpleName?.contains("SocketTimeoutException") == true ||
            this.message?.contains("Failed to connect") == true

    return if (isNetworkError) {
        AppError(
            type = AppErrorType.NETWORK,
            message = "网络连接失败，请检查您的网络设置",
            throwable = this,
            onRetry = onRetry
        )
    } else {
        AppError(
            type = AppErrorType.SERVER,
            message = this.message ?: "服务器繁忙，请稍后再试",
            throwable = this,
            onRetry = onRetry
        )
    }
}
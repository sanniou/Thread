package ai.saniou.thread.network;

sealed class SaniouResult<T> {
    data class Success<T>(val data: T) : SaniouResult<T>()
    class Error(val ex: Throwable) : SaniouResult<Nothing>()

    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(ex: Throwable) = Error(ex).apply {
            println("==" + ex.cause + "\n" + ex.message + "\n" + ex.printStackTrace())
            ex.cause?.run {
                if (ex.cause != ex) {
                    println(ex.cause!!.message + "\n" + ex.cause!!.printStackTrace())
                }
            }
        }
    }
}

fun <T> SaniouResult<T>.toResult(): Result<T> {
    return when (this) {
        is SaniouResult.Success -> Result.success(data)
        is SaniouResult.Error -> Result.failure(ex)
    }
}

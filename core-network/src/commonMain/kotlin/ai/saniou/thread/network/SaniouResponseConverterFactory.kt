package ai.saniou.thread.network

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.toByteArray

class SaniouResponseConverterFactory : Converter.Factory {

    override fun suspendResponseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit,
    ): Converter.SuspendResponseConverter<HttpResponse, *>? {
        return when (typeData.typeInfo.type) {
            SaniouResult::class -> object :
                Converter.SuspendResponseConverter<HttpResponse, Any> {
                override suspend fun convert(result: KtorfitResult): Any {
                    return when (result) {
                        is KtorfitResult.Success -> {
                            return try {
                                SaniouResult.success(
                                    result.response.body<Any>(
                                        typeData.typeArgs.first().typeInfo
                                    )
                                )
                            } catch (ex: Throwable) {
                                // nmb eg: {"success":false,"error":"必须登入领取饼干后才可以访问"}
                                // discrouse : {"errors":["您执行此操作的次数过多。请等待 15 秒后再试。"],"error_type":"rate_limit","extras":{"wait_seconds":15,"time_left":"15 秒"}}
                                try {
                                    val message = result.response.bodyAsChannel().toByteArray()
                                        .decodeUnicodeEscapes()
                                    return SaniouResult.error(
                                        RuntimeException(message.split(":").last().trim())
                                    )
                                } catch (ex: Throwable) {
                                    // do nothing
                                }
                                SaniouResult.error(ex)
                            }
                        }

                        is KtorfitResult.Failure -> {
                            SaniouResult.error(result.throwable)
                        }
                    }
                }
            }

            String::class -> object :
                Converter.SuspendResponseConverter<HttpResponse, String> {
                override suspend fun convert(result: KtorfitResult): String {
                    return when (result) {
                        is KtorfitResult.Success -> {
                            result.response.bodyAsChannel().toByteArray().decodeUnicodeEscapes()
                                .drop(1).dropLast(1)
                        }

                        is KtorfitResult.Failure -> {
                            result.throwable.message ?: "Unknown error"
                        }
                    }
                }
            }


            else -> super.suspendResponseConverter(typeData, ktorfit)
        }
    }
}

fun ByteArray.decodeUnicodeEscapes(): String {
    val str = this.decodeToString()
    val result = StringBuilder(str.length)
    var i = 0
    while (i < str.length) {
        if (i + 5 < str.length && str[i] == '\\' && str[i + 1] == 'u') {
            try {
                val hex = str.substring(i + 2, i + 6)
                val codePoint = hex.toInt(16)
                result.append(codePoint.toChar())
                i += 6
                continue
            } catch (e: NumberFormatException) {
                // 无效的Unicode转义，保持原样
            }
        }
        result.append(str[i])
        i++
    }
    return result.toString()
}

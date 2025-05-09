package ai.saniou.corecommon.data;

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.KtorfitResult.Failure
import de.jensklingenberg.ktorfit.converter.KtorfitResult.Success
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.toByteArray

class SaniouResponseConverterFactory : Converter.Factory {

    override fun suspendResponseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit
    ): Converter.SuspendResponseConverter<HttpResponse, *>? {
        return when (typeData.typeInfo.type) {
            SaniouResponse::class -> object :
                Converter.SuspendResponseConverter<HttpResponse, SaniouResponse<*>> {
                override suspend fun convert(result: KtorfitResult): SaniouResponse<*> {
                    return when (result) {
                        is Success -> {
                            return try {
                                SaniouResponse.success(
                                    result.response.body<SaniouResponse<*>>(
                                        typeData.typeArgs.first().typeInfo
                                    )
                                )
                            } catch (ex: Throwable) {
                                SaniouResponse.error(ex)
                            }
                        }

                        is Failure -> {
                            SaniouResponse.error(result.throwable)
                        }
                    }
                }
            }

            String::class -> object :
                Converter.SuspendResponseConverter<HttpResponse, String> {
                override suspend fun convert(result: KtorfitResult): String {
                    return when (result) {
                        is Success -> {
                            result.response.bodyAsChannel().toByteArray().decodeUnicodeEscapes().drop(1).dropLast(1)
                        }

                        is Failure -> {
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
        if (i + 5 < str.length && str[i] == '\\' && str[i+1] == 'u') {
            try {
                val hex = str.substring(i+2, i+6)
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

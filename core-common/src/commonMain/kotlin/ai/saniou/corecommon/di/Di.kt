package ai.saniou.corecommon.di

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.KtorfitResult.Failure
import de.jensklingenberg.ktorfit.converter.KtorfitResult.Success
import de.jensklingenberg.ktorfit.converter.TypeData
import de.jensklingenberg.ktorfit.ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.bindMultiton

val coreCommon by DI.Module {

    bindMultiton<String, Ktorfit> { baseUrl ->
        ktorfit(baseUrl)
    }

//    bindSingleton<Ktorfit> {
//        ktorfit(this.instance<String>("baseUrl"))
//    }

}

private fun ktorfit(string: String): Ktorfit = ktorfit {
    baseUrl(string)
    httpClient(
        HttpClient {
            ContentEncoding {
                gzip()
            }

            useDefaultTransformers = false
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }
                level = LogLevel.ALL
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
            install(DefaultRequest) {
                header(
                    "Cookie",
                    "userhash=%B2%21%02%14%A4%23t%A5%E8%7D%81%3E%FC%40%0F%A8T%AD%1A%A8%B5%C6%CFI"
                )
            }
        }
    )
    converterFactories(
        SaniouResponseConverterFactory(),
//        FlowConverterFactory(),
//        CallConverterFactory()
    )
}

class SaniouResponseConverterFactory : Converter.Factory {

    override fun suspendResponseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit
    ): Converter.SuspendResponseConverter<HttpResponse, *>? {
        if (typeData.typeInfo.type == SaniouResponse::class) {
            return object : Converter.SuspendResponseConverter<HttpResponse, Any> {
                override suspend fun convert(result: KtorfitResult): Any {
                    return when (result) {
                        is Success -> {
                            return try {
                                SaniouResponse.success(result.response.body<Any>(typeData.typeArgs.first().typeInfo))
                            } catch (ex: Throwable) {
                                SaniouResponse.error(ex)
                            }
                        }

                        is Failure -> {
                            SaniouResponse.error(result.throwable)
                        }

                        else -> SaniouResponse.error(RuntimeException("Result is $result"))
                    }
                }
            }
        }
        return super.suspendResponseConverter(typeData, ktorfit)
    }


}

sealed class SaniouResponse<T> {
    data class Success<T>(val data: T) : SaniouResponse<T>()
    class Error(val ex: Throwable) : SaniouResponse<Nothing>()

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

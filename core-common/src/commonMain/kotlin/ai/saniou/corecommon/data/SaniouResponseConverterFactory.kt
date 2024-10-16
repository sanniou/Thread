package ai.saniou.corecommon.data;

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.KtorfitResult.Failure
import de.jensklingenberg.ktorfit.converter.KtorfitResult.Success
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

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

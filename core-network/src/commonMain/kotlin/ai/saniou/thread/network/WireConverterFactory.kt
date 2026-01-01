package ai.saniou.thread.network

import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import io.ktor.utils.io.toByteArray
import kotlin.reflect.KClass

class WireConverterFactory : Converter.Factory {

    override fun responseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit
    ): Converter.ResponseConverter<HttpResponse, *>? {
        // We return null here because we can't generically find the ProtoAdapter in KMP.
        // Users should use Flow<ByteArray> and decode manually, or Flow<HttpResponse>.
        return null
    }

    override fun suspendResponseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit
    ): Converter.SuspendResponseConverter<HttpResponse, *>? {
        return null
    }
}
package ai.saniou.thread.network

import com.squareup.wire.Message
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.ContentConverter
import io.ktor.util.reflect.TypeInfo
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.toByteArray

class WireContentConverter : ContentConverter {
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent? {
        if (value is Message<*, *>) {
            return ByteArrayContent(value.encode(), contentType)
        }
        return null
    }

    override suspend fun deserialize(
        charset: Charset,
        typeInfo: TypeInfo,
        content: ByteReadChannel
    ): Any? {
        val kClass = typeInfo.type
        val adapter = WireUtils.getAdapter(kClass)
        if (adapter != null) {
            val byteArray = content.toByteArray()
            return adapter.decode(byteArray)
        }
        return null
    }
}
package ai.saniou.thread.network

import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.toByteArray
import kotlin.reflect.KClass

class WireResponseConverterFactory : Converter.Factory {

    override fun suspendResponseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit
    ): Converter.SuspendResponseConverter<HttpResponse, *>? {
        val type = typeData.typeInfo.type
        // Note: isAssignableFrom might not work in KMP common without kotlin-reflect or similar.
        // But assuming we can check if it implements Message.
        // Actually, in KMP 'isSubclassOf' or similar requires kotlin-reflect.
        // For now, we can check by name or try catch? Or assume simple case.
        // Wire messages implement Message interface.
        
        // Simple check: if package starts with 'tieba.', it's likely our proto.
        // Or if we can cast?
        
        // Strategy: We assume the type is a Message if we can find its ADAPTER.
        // But finding ADAPTER generically is the hard part in KMP without reflection.
        
        // Temporary solution:
        // We only support this on JVM/Android fully via reflection if needed, 
        // OR we register the adapter manually? No, too many.
        
        // Let's assume we are on JVM for now or use a KMP compatible way if Wire provides one.
        // Wire generated code has: companion object { @JvmField val ADAPTER = ... }
        
        // For this task, I will leave a TODO for full KMP reflection support 
        // and implement a basic check that might rely on platform specifics 
        // or just return a converter that tries to decode using a known adapter if passed?
        // But Ktorfit doesn't pass the adapter.
        
        // Workaround: We will use a helper function that might be platform specific 
        // or try to use `ProtoAdapter.get(kClass)` if available.
        
        return null // Placeholder until we confirm ProtoAdapter availability
    }
}
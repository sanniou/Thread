package ai.saniou.thread.network

import com.squareup.wire.ProtoAdapter
import kotlin.reflect.KClass

actual object WireUtils {
    actual fun <T : Any> getAdapter(kClass: KClass<T>): ProtoAdapter<T>? {
        return try {
            val adapterField = kClass.java.getField("ADAPTER")
            @Suppress("UNCHECKED_CAST")
            adapterField.get(null) as? ProtoAdapter<T>
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
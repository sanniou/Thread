package ai.saniou.thread.network

import com.squareup.wire.ProtoAdapter
import kotlin.reflect.KClass

/** Native does not support the JVM reflection used to discover Wire's generated ADAPTER field. */
actual object WireUtils {
    actual fun <T : Any> getAdapter(kClass: KClass<T>): ProtoAdapter<T>? = null
}

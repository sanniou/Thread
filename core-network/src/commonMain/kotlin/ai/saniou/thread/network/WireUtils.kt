package ai.saniou.thread.network

import com.squareup.wire.ProtoAdapter
import kotlin.reflect.KClass

expect object WireUtils {
    fun <T : Any> getAdapter(kClass: KClass<T>): ProtoAdapter<T>?
}
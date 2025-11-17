package ai.saniou.nmb.data.storage

import io.github.irgaly.kottage.put
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.time.Duration

class CommonStorage(scope: CoroutineScope) : BasicStorage(scope, "nmb-common") {

    suspend inline fun <reified T : Any> saveValue(
        key: String,
        value: T?,
        expireTime: Duration? = null,
    ) {
        return saveValue(key, value, typeOf<T>(), expireTime)
    }

    suspend fun <T : Any> saveValue(
        key: String,
        value: T?,
        type: KType,
        expireTime: Duration? = null,
    ) {
        value?.run {
            storage.put(key, value, type, expireTime)
        } ?: run {
            storage.remove(key)
        }
    }


    suspend inline fun <reified T : Any> getValue(key: String): T? {
        return getValue(key, typeOf<T>())
    }

    suspend fun <T : Any> getValue(key: String, type: KType): T? {
        val result = storage.getOrNull<T>(key, type)
        return result
    }
}

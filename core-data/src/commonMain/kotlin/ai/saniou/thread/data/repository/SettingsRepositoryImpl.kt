package ai.saniou.thread.data.repository

import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.repository.SettingsRepository
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class SettingsRepositoryImpl(
    private val db: Database,
) : SettingsRepository {

    suspend inline fun <reified T : Any> saveValue(
        key: String,
        value: T?,
    ) {
        return saveValue(key, value, typeOf<T>())
    }

    suspend inline fun <reified T : Any> getValue(key: String): T? {
        return getValue(key, typeOf<T>())
    }

    inline fun <reified T : Any> observeValue(key: String): Flow<T?> {
        return observeValue(key, typeOf<T>())
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> saveValue(key: String, value: T?, type: KType) {
        withContext(Dispatchers.IO) {
            value?.let {
                val serializer = Json.serializersModule.serializer(type) as KSerializer<T>
                val stringValue = Json.encodeToString(serializer, it)
                db.keyValueQueries.insertKeyValue(key, stringValue)
            } ?: db.keyValueQueries.deleteKeyValue(key)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Any> getValue(key: String, type: KType): T? {
        return withContext(Dispatchers.IO) {
            db.keyValueQueries.getKeyValue(key).executeAsOneOrNull()?.value_?.let {
                val serializer = Json.serializersModule.serializer(type) as KSerializer<T>
                Json.decodeFromString(serializer, it)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> observeValue(key: String, type: KType): Flow<T?> {
        return db.keyValueQueries.getKeyValue(key)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { entity ->
                entity?.value_?.let {
                    val serializer = Json.serializersModule.serializer(type) as KSerializer<T>
                    Json.decodeFromString(serializer, it)
                }
            }
    }
}

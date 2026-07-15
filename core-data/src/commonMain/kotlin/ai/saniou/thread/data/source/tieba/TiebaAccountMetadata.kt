package ai.saniou.thread.data.source.tieba

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TiebaAccountMetadata(
    val stoken: String,
    val tbs: String = "",
    val portrait: String = "",
    val zid: String = "",
)

internal val tiebaAccountJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal fun TiebaAccountMetadata.serialize(): String = tiebaAccountJson.encodeToString(this)

internal fun String.toTiebaAccountMetadata(): TiebaAccountMetadata? =
    runCatching { tiebaAccountJson.decodeFromString<TiebaAccountMetadata>(this) }.getOrNull()

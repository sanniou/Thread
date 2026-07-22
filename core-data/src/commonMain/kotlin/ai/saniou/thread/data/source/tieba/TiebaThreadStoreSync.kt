package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Official thread store (addstore / rmstore) used when users bookmark a Tieba topic.
 */
class TiebaThreadStoreSync(
    private val api: OfficialTiebaApi,
    private val webApi: WebTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) {
    val sourceId: String = TiebaMapper.SOURCE_ID

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    suspend fun addStore(threadId: String, postId: String = threadId) {
        require(threadId.isNotBlank()) { "贴吧收藏需要主题 ID" }
        val payload = json.encodeToString(
            listOf(
                CollectPayload(
                    tid = threadId,
                    pid = postId.ifBlank { threadId },
                    status = 1,
                ),
            ),
        )
        val response = api.addStore(
            data = payload,
            stoken = parameterProvider.getSToken(),
            client_user_token = parameterProvider.getUid().takeIf(String::isNotBlank),
        )
        if (response.errorCode != 0) {
            throw IllegalStateException(response.errorMsg.ifBlank { "贴吧收藏失败 (${response.errorCode})" })
        }
    }

    suspend fun removeStore(threadId: String) {
        require(threadId.isNotBlank()) { "取消收藏需要主题 ID" }
        val tbs = parameterProvider.ensureTbs(webApi)
        val response = api.removeStore(
            threadId = threadId,
            forumId = "null",
            tbs = tbs,
            stoken = parameterProvider.getSToken(),
            user_id = parameterProvider.getUid().takeIf(String::isNotBlank),
            client_user_token = parameterProvider.getUid().takeIf(String::isNotBlank),
        )
        if (response.errorCode != 0) {
            throw IllegalStateException(response.errorMsg.ifBlank { "取消贴吧收藏失败 (${response.errorCode})" })
        }
    }

    @Serializable
    private data class CollectPayload(
        val tid: String,
        val pid: String,
        val status: Int = 1,
    )
}

/** Bookmark id shape used by TopicDetail: `{sourceId}.Topic.{topicId}` */
internal fun parseTiebaTopicBookmarkId(bookmarkId: String): String? {
    val prefix = "${TiebaMapper.SOURCE_ID}.Topic."
    if (!bookmarkId.startsWith(prefix)) return null
    return bookmarkId.removePrefix(prefix).takeIf(String::isNotBlank)
}

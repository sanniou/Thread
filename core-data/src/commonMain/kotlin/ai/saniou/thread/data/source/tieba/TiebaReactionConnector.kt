package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.domain.source.ReactionConnector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TiebaReactionConnector(
    private val source: TiebaSource,
    private val api: MiniTiebaApi,
    private val webApi: WebTiebaApi,
    private val officialApi: OfficialTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) : ReactionConnector {
    override val sourceId: String = source.id

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun upvote(topicId: String, targetPostId: String): Result<Unit> = runCatching {
        val tbs = parameterProvider.ensureTbs(webApi)
        val response = api.agree(
            postId = targetPostId,
            threadId = topicId,
            client_user_token = parameterProvider.getUid().takeIf(String::isNotBlank),
            tbs = tbs,
            stoken = parameterProvider.getSToken().takeIf(String::isNotBlank),
        )
        if (!response.errorCode.isNullOrBlank() && response.errorCode != "0") {
            throw IllegalStateException(response.errorMsg ?: "贴吧点赞失败 (${response.errorCode})")
        }
    }

    /**
     * Tieba "点踩" uses the same opAgree endpoint with agree_type=5 (see MiniTiebaApi.disagreeFlow).
     * The Call variant defaults to agree_type=2 (cancel like); we always pass 5 for true downvote.
     */
    override suspend fun downvote(topicId: String, targetPostId: String): Result<Unit> = runCatching {
        val tbs = parameterProvider.ensureTbs(webApi)
        val stoken = parameterProvider.getSToken().takeIf(String::isNotBlank)
            ?: throw IllegalStateException("请先登录贴吧账号后再点踩")
        val response = api.disagree(
            postId = targetPostId,
            threadId = topicId,
            client_user_token = parameterProvider.getUid().takeIf(String::isNotBlank),
            agree_type = 5,
            op_type = 0,
            tbs = tbs,
            stoken = stoken,
        )
        if (!response.errorCode.isNullOrBlank() && response.errorCode != "0") {
            throw IllegalStateException(response.errorMsg ?: "贴吧点踩失败 (${response.errorCode})")
        }
    }

    /**
     * 推荐流「不感兴趣」→ Official [submitDislike].
     * Payload matches TiebaLite: JSON array of DislikeBean (tid / dislike_ids / fid / click_time / extra).
     */
    @OptIn(ExperimentalTime::class)
    override suspend fun submitNotInterested(
        topicId: String,
        channelId: String?,
        reasonIds: String,
        extra: String,
        clickTimeMs: Long,
    ): Result<String> = runCatching {
        val stoken = parameterProvider.getSToken().takeIf(String::isNotBlank)
            ?: throw IllegalStateException("请先登录贴吧账号后再标记不感兴趣")
        val click = if (clickTimeMs > 0L) clickTimeMs else Clock.System.now().toEpochMilliseconds()
        val bean = TiebaDislikeBean(
            threadId = topicId,
            dislikeIds = reasonIds,
            forumId = channelId?.takeIf { it.isNotBlank() && it != "0" },
            clickTime = click,
            extra = extra,
        )
        val payload = json.encodeToString(listOf(bean))
        val response = officialApi.submitDislike(
            dislike = payload,
            dislike_from = "homepage",
            stoken = stoken,
        )
        if (response.errorCode != 0) {
            throw IllegalStateException(
                response.errorMsg.ifBlank { "标记不感兴趣失败 (${response.errorCode})" },
            )
        }
        "已标记不感兴趣"
    }
}

@Serializable
private data class TiebaDislikeBean(
    @SerialName("tid") val threadId: String,
    @SerialName("dislike_ids") val dislikeIds: String = "",
    @SerialName("fid") val forumId: String? = null,
    @SerialName("click_time") val clickTime: Long,
    val extra: String = "",
)

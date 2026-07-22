package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.domain.source.ReactionConnector

class TiebaReactionConnector(
    private val source: TiebaSource,
    private val api: MiniTiebaApi,
    private val webApi: WebTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) : ReactionConnector {
    override val sourceId: String = source.id

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
}

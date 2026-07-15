package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.domain.source.ReactionConnector

class TiebaReactionConnector(
    private val source: TiebaSource,
    private val api: MiniTiebaApi,
    private val parameterProvider: TiebaParameterProvider,
) : ReactionConnector {
    override val sourceId: String = source.id

    override suspend fun upvote(topicId: String, targetPostId: String): Result<Unit> = runCatching {
        val response = api.agree(
            postId = targetPostId,
            threadId = topicId,
            client_user_token = parameterProvider.getUid().takeIf(String::isNotBlank),
            tbs = parameterProvider.getTbs().takeIf(String::isNotBlank),
            stoken = parameterProvider.getSToken().takeIf(String::isNotBlank),
        )
        if (!response.errorCode.isNullOrBlank() && response.errorCode != "0") {
            throw IllegalStateException(response.errorMsg ?: "贴吧点赞失败 (${response.errorCode})")
        }
    }
}

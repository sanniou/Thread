package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.nmb.required
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.source.LoginConnector
import ai.saniou.thread.domain.source.PostingConnector
import ai.saniou.thread.network.SaniouResult
import kotlin.time.Clock

class DiscoursePostingConnector(
    private val source: DiscourseSource,
    private val api: DiscourseApi,
) : PostingConnector {
    override val sourceId: String = source.id

    override suspend fun createThread(channelId: String, draft: PostDraft): PostResult {
        require(draft.attachment == null) { "Discourse attachment upload is not available yet" }
        val title = draft.title?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Discourse topics require a title")
        return api.createPost(raw = draft.content, title = title, category = channelId).toPostResult()
    }

    override suspend fun createReply(topicId: String, draft: PostDraft): PostResult {
        require(draft.attachment == null) { "Discourse attachment upload is not available yet" }
        return api.createPost(raw = draft.content, topicId = topicId).toPostResult()
    }

    private fun SaniouResult<ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCreatePostResponse>.toPostResult(): PostResult =
        when (this) {
            is SaniouResult.Success -> PostResult(
                sourceId = sourceId,
                postId = data.id.toString(),
                topicId = data.topicId.toString(),
            )
            is SaniouResult.Error -> throw ex
        }
}

class DiscourseLoginConnector(
    private val source: DiscourseSource,
    private val credentialProvider: DiscourseCredentialProvider,
) : LoginConnector {
    override val sourceId: String = source.id
    override val strategy: LoginStrategy = source.loginStrategy

    override suspend fun login(inputs: Map<String, String>): Account {
        val apiKey = inputs.required("apiKey")
        credentialProvider.update(apiKey)
        val now = Clock.System.now()
        return Account(
            id = "$sourceId:${apiKey.hashCode()}",
            sourceId = sourceId,
            alias = inputs["alias"].orEmpty().ifBlank { source.name },
            value = apiKey,
            uid = null,
            avatar = null,
            extraData = null,
            sort = 0,
            isCurrent = true,
            lastUsedAt = now,
            createdAt = now,
        )
    }
}

package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.source.LoginConnector
import ai.saniou.thread.domain.source.PostingConnector
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlin.time.Clock

class NmbPostingConnector(
    private val api: NmbXdApi,
) : PostingConnector {
    override val sourceId: String = NMBSourceId

    override suspend fun createThread(channelId: String, draft: PostDraft): PostResult {
        val forumId = channelId.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid NMB channel id: $channelId")
        return api.postThread(forumId, draft.content, draft.toOptionalParts()).toResult()
    }

    override suspend fun createReply(topicId: String, draft: PostDraft): PostResult {
        val replyTopicId = topicId.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid NMB topic id: $topicId")
        return api.postReply(replyTopicId, draft.content, draft.toOptionalParts()).toResult(topicId)
    }

    private fun String.toResult(topicId: String? = null): PostResult = PostResult(
        sourceId = sourceId,
        topicId = topicId,
        message = extractNmbError(this),
    )

    private fun PostDraft.toOptionalParts() = formData {
        name?.let { append("name", it) }
        title?.let { append("title", it) }
        if (water) append("water", "true")
        attachment?.let { file ->
            append(
                key = "image",
                value = file.bytes,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, file.contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                },
            )
        }
    }
}

class NmbLoginConnector(
    private val source: NmbSource,
) : LoginConnector {
    override val sourceId: String = source.id
    override val strategy: LoginStrategy = source.loginStrategy

    override suspend fun login(inputs: Map<String, String>): Account {
        val cookie = inputs.required("cookie")
        val now = Clock.System.now()
        return Account(
            id = "$sourceId:${cookie.hashCode()}",
            sourceId = sourceId,
            alias = inputs["alias"].orEmpty().ifBlank { "饼干" },
            value = cookie,
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

private fun extractNmbError(html: String): String? {
    val marker = "<p class=\"error\">"
    val start = html.indexOf(marker).takeIf { it >= 0 }?.plus(marker.length) ?: return null
    val end = html.indexOf("</p>", start).takeIf { it >= 0 } ?: return null
    return html.substring(start, end)
}

internal fun Map<String, String>.required(key: String): String =
    this[key]?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Missing required login field: $key")

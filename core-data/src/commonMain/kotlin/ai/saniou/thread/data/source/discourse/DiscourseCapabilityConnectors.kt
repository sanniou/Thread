package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.DiscourseApi
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCreatePostResponse
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUploadResponse
import ai.saniou.thread.data.source.nmb.required
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.source.LoginConnector
import ai.saniou.thread.domain.source.PostingConnector
import ai.saniou.thread.network.SaniouResult
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlin.time.Clock

class DiscoursePostingConnector internal constructor(
    override val sourceId: String,
    private val transport: DiscoursePostTransport,
) : PostingConnector {
    constructor(source: DiscourseSource, api: DiscourseApi) : this(
        sourceId = source.id,
        transport = ApiDiscoursePostTransport(api),
    )

    override suspend fun createThread(channelId: String, draft: PostDraft): PostResult {
        val title = draft.title?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Discourse topics require a title")
        val raw = prepareRaw(draft)
        require(raw.isNotBlank()) { "主题内容和附件不能同时为空" }
        return transport.createPost(raw = raw, title = title, category = channelId).toPostResult()
    }

    override suspend fun createReply(topicId: String, draft: PostDraft): PostResult {
        val raw = prepareRaw(draft)
        require(raw.isNotBlank()) { "回复内容和附件不能同时为空" }
        return transport.createPost(raw = raw, topicId = topicId).toPostResult()
    }

    private suspend fun prepareRaw(draft: PostDraft): String {
        val uploaded = draft.attachment?.let { transport.upload(it) }
        return buildList {
            draft.content.trim().takeIf(String::isNotBlank)?.let(::add)
            uploaded?.let { asset ->
                val label = asset.originalFilename.ifBlank { draft.attachment?.fileName.orEmpty() }
                    .replace("]", "\\]")
                add("![$label](${asset.shortUrl ?: asset.url})")
            }
        }.joinToString("\n\n")
    }

    private fun DiscourseCreatePostResponse.toPostResult() = PostResult(
        sourceId = sourceId,
        postId = id.toString(),
        topicId = topicId.toString(),
    )
}

internal interface DiscoursePostTransport {
    suspend fun upload(attachment: ai.saniou.thread.domain.model.forum.PostAttachment): DiscourseUploadResponse

    suspend fun createPost(
        raw: String,
        title: String? = null,
        category: String? = null,
        topicId: String? = null,
    ): DiscourseCreatePostResponse
}

private class ApiDiscoursePostTransport(
    private val api: DiscourseApi,
) : DiscoursePostTransport {
    override suspend fun upload(
        attachment: ai.saniou.thread.domain.model.forum.PostAttachment,
    ): DiscourseUploadResponse {
        require(attachment.bytes.isNotEmpty()) { "附件不能为空" }
        require(attachment.bytes.size <= MAX_UPLOAD_BYTES) { "Discourse 附件不能超过 20 MB" }
        val parts = formData {
            append(
                key = "file",
                value = attachment.bytes,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, attachment.contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${attachment.fileName}\"")
                },
            )
        }
        return api.upload(parts = parts).dataOrThrow()
    }

    override suspend fun createPost(
        raw: String,
        title: String?,
        category: String?,
        topicId: String?,
    ): DiscourseCreatePostResponse = api.createPost(
        raw = raw,
        title = title,
        category = category,
        topicId = topicId,
    ).dataOrThrow()

    private fun <T> SaniouResult<T>.dataOrThrow(): T = when (this) {
        is SaniouResult.Success -> data
        is SaniouResult.Error -> throw ex
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 20 * 1024 * 1024
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

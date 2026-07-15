package ai.saniou.thread.data.repository

import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.repository.PostRepository
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class PostRepositoryImpl(
    private val nmbXdApi: NmbXdApi
) : PostRepository {
    override suspend fun post(
        fid: Int,
        draft: PostDraft,
    ): String {
        return nmbXdApi.postThread(fid, draft.content, draft.toOptionalParts())
    }

    override suspend fun reply(
        resto: Int,
        draft: PostDraft,
    ): String {
        return nmbXdApi.postReply(resto, draft.content, draft.toOptionalParts())
    }

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

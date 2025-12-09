package ai.saniou.thread.data.repository

import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.domain.repository.PostRepository
import io.ktor.client.request.forms.formData
import io.ktor.http.content.PartData
import io.ktor.utils.io.InternalAPI

class PostRepositoryImpl(
    private val nmbXdApi: NmbXdApi
) : PostRepository {
    @OptIn(InternalAPI::class)
    override suspend fun post(
        fid: Int,
        content: String,
        name: String?,
        title: String?,
        water: Boolean,
        image: PartData?
    ): String {
        val optionalParts = formData {
            name?.let { append("name", it) }
            title?.let { append("title", it) }
            if (water) append("water", "true")
            image?.let { append("image", it) }
        }
        return nmbXdApi.postThread(fid, content, optionalParts)
    }

    @OptIn(InternalAPI::class)
    override suspend fun reply(
        resto: Int,
        content: String,
        name: String?,
        title: String?,
        water: Boolean,
        image: PartData?
    ): String {
        val optionalParts = formData {
            name?.let { append("name", it) }
            title?.let { append("title", it) }
            if (water) append("water", "true")
            image?.let { append("image", it) }
        }
        return nmbXdApi.postReply(resto, content, optionalParts)
    }
}

package ai.saniou.nmb.domain

import ai.saniou.nmb.data.repository.ForumRepository
import io.ktor.client.request.forms.formData
import io.ktor.http.content.PartData
import io.ktor.utils.io.InternalAPI

@OptIn(InternalAPI::class)

class PostUseCase(
    private val forumRepository: ForumRepository
) {
    suspend fun post(
        fid: Int,
        content: String,
        name: String? = null,
        title: String? = null,
        water: Boolean = false,
        image: PartData? = null
    ): String {
        val optionalParts = formData {
            name?.let { append("name", it) }
            title?.let { append("title", it) }
            if (water) append("water", "true")
            image?.let { append("image", it) }
        }
        return forumRepository.postThread(fid, content, optionalParts)
    }

    suspend fun reply(
        resto: Int,
        content: String,
        name: String? = null,
        title: String? = null,
        water: Boolean = false,
        image: PartData? = null
    ): String {
        val optionalParts = formData {
            name?.let { append("name", it) }
            title?.let { append("title", it) }
            if (water) append("water", "true")
            image?.let { append("image", it) }
        }
        return forumRepository.postReply(resto, content, optionalParts)
    }
}

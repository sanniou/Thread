package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.repository.PostRepository
import io.ktor.http.content.PartData

class PostUseCase(
    private val postRepository: PostRepository
) {
    suspend fun post(
        fid: Int,
        content: String,
        name: String? = null,
        title: String? = null,
        water: Boolean = false,
        image: PartData? = null
    ): String {
        return postRepository.post(fid, content, name, title, water, image)
    }

    suspend fun reply(
        resto: Int,
        content: String,
        name: String? = null,
        title: String? = null,
        water: Boolean = false,
        image: PartData? = null
    ): String {
        return postRepository.reply(resto, content, name, title, water, image)
    }
}
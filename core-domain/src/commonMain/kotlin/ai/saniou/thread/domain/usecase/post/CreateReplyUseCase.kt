package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.repository.PostRepository
import io.ktor.http.content.PartData

class CreateReplyUseCase(private val postRepository: PostRepository) {
    suspend operator fun invoke(
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

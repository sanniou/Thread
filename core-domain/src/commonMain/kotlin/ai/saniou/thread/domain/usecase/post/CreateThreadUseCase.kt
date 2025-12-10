package ai.saniou.thread.domain.usecase.post

import ai.saniou.thread.domain.repository.PostRepository
import io.ktor.http.content.PartData

class CreateThreadUseCase(private val postRepository: PostRepository) {
    suspend operator fun invoke(
        fid: Int,
        content: String,
        name: String? = null,
        title: String? = null,
        water: Boolean = false,
        image: PartData? = null
    ): String {
        return postRepository.post(fid, content, name, title, water, image)
    }
}

package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.model.Tag
import ai.saniou.thread.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow

class GetTagsUseCase(private val tagRepository: TagRepository) {
    operator fun invoke(): Flow<List<Tag>> = tagRepository.getAllTags()
}
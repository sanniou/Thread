package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.repository.ReaderRepository

class GetArticleUseCase(private val repository: ReaderRepository) {
    suspend operator fun invoke(id: String): Article? = repository.getArticle(id)
}
package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.model.Article
import ai.saniou.thread.domain.repository.ReaderRepository
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetArticlesUseCase(private val repository: ReaderRepository) {
    operator fun invoke(
        feedSourceId: String? = null,
        query: String = ""
    ): Flow<PagingData<Article>> = repository.getArticlesPaging(feedSourceId, query)
}
package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetBookmarksUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(query: String? = null, tags: List<String>? = null): Flow<PagingData<Bookmark>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { bookmarkRepository.getBookmarks(query, tags) }
        ).flow
    }
}

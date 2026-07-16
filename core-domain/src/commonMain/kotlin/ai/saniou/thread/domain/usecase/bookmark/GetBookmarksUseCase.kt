package ai.saniou.thread.domain.usecase.bookmark

import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.BookmarkRepository
import androidx.paging.Pager
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

class GetBookmarksUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(query: String? = null, tags: List<String>? = null): Flow<PagingData<Bookmark>> {
        return Pager(
            config = threadPagingConfig(),
            pagingSourceFactory = { bookmarkRepository.getBookmarks(query, tags) }
        ).flow
    }
}

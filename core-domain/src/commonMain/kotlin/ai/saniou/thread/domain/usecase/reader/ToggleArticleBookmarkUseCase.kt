package ai.saniou.thread.domain.usecase.reader

import ai.saniou.thread.domain.model.bookmark.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import ai.saniou.thread.domain.repository.ReaderRepository
import kotlin.time.Clock

class ToggleArticleBookmarkUseCase(
    private val repository: ReaderRepository,
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(id: String, currentStatus: Boolean) {
        val newStatus = !currentStatus
        repository.markArticleAsBookmarked(id, newStatus)

        if (newStatus) {
            val article = repository.getArticle(id)
            if (article != null) {
                val bookmark = Bookmark.Quote(
                    id = "article_${article.id}", // Ensure unique ID for bookmark
                    createdAt = Clock.System.now(),
                    tags = emptyList(),
                    content = article.title, // Use title as content for bookmark
                    sourceId = article.id,
                    sourceType = "article"
                )
                bookmarkRepository.addBookmark(bookmark)
            }
        } else {
             // We construct the ID based on our convention to remove it
             bookmarkRepository.removeBookmark("article_$id")
        }
    }
}

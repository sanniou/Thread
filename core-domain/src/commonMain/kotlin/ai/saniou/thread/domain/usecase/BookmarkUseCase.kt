package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Bookmark
import ai.saniou.thread.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow

/**
 * 获取所有书签的业务用例。
 */
class GetBookmarksUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(): Flow<List<Bookmark>> = bookmarkRepository.getBookmarks()
}

/**
 * 切换书签状态的业务用例。
 * 这个 UseCase 封装了“添加”和“移除”两个操作，更符合用户的单一业务动作。
 */
class AddBookmarkUseCase(private val bookmarkRepository: BookmarkRepository) {
    suspend operator fun invoke(postId: String, content: String, tag: String? = null) =
        bookmarkRepository.addBookmark(postId, content, tag)
}

class RemoveBookmarkUseCase(private val bookmarkRepository: BookmarkRepository) {
    suspend operator fun invoke(postId: String) = bookmarkRepository.removeBookmark(postId)
}

/**
 * 检查一个帖子是否已被收藏的业务用例。
 */
class IsBookmarkedUseCase(private val bookmarkRepository: BookmarkRepository) {
    operator fun invoke(postId: String): Flow<Boolean> = bookmarkRepository.isBookmarked(postId)
}

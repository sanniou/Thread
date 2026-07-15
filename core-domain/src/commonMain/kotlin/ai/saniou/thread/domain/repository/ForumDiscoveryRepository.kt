package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/** Source-aware discovery facade consumed by feature modules. */
interface ForumSearchRepository {
    fun searchTopics(sourceId: String, query: String): Flow<PagingData<Topic>>

    fun searchComments(sourceId: String, query: String): Flow<PagingData<Comment>>
}

/** Source-aware author-content facade consumed by feature modules. */
interface UserContentRepository {
    fun getUserTopics(sourceId: String, userId: String): Flow<PagingData<Topic>>

    fun getUserComments(sourceId: String, userId: String): Flow<PagingData<Comment>>
}

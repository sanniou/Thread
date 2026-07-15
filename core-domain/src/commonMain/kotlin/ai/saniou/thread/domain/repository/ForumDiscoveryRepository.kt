package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/** Common local discovery contract implemented by connectors with searchable content. */
interface ForumSearchRepository {
    fun searchTopics(query: String): Flow<PagingData<Topic>>

    fun searchComments(query: String): Flow<PagingData<Comment>>
}

/** Common user-content contract implemented by connectors that expose author history. */
interface UserContentRepository {
    fun getUserTopics(userId: String): Flow<PagingData<Topic>>

    fun getUserComments(userId: String): Flow<PagingData<Comment>>
}

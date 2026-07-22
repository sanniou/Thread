package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.ForumSearchRepository
import ai.saniou.thread.domain.repository.UserContentRepository
import ai.saniou.thread.domain.source.ConnectorRegistry
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ForumSearchRepositoryImpl(
    private val registry: ConnectorRegistry,
) : ForumSearchRepository {
    override fun searchTopics(sourceId: String, query: String): Flow<PagingData<Topic>> =
        registry.search(sourceId)?.searchTopics(query) ?: flowOf(PagingData.empty())

    override fun searchComments(sourceId: String, query: String): Flow<PagingData<Comment>> =
        registry.search(sourceId)?.searchComments(query) ?: flowOf(PagingData.empty())

    override fun searchChannels(sourceId: String, query: String): Flow<PagingData<Channel>> =
        registry.search(sourceId)?.searchChannels(query) ?: flowOf(PagingData.empty())

    override fun searchUsers(sourceId: String, query: String): Flow<PagingData<Author>> =
        registry.search(sourceId)?.searchUsers(query) ?: flowOf(PagingData.empty())

    override fun searchChannelTopics(
        sourceId: String,
        channelId: String,
        channelName: String,
        query: String,
    ): Flow<PagingData<Topic>> =
        registry.search(sourceId)?.searchChannelTopics(channelId, channelName, query)
            ?: flowOf(PagingData.empty())
}

class UserContentRepositoryImpl(
    private val registry: ConnectorRegistry,
) : UserContentRepository {
    override fun getUserTopics(sourceId: String, userId: String): Flow<PagingData<Topic>> =
        registry.userContent(sourceId)?.getUserTopics(userId) ?: flowOf(PagingData.empty())

    override fun getUserComments(sourceId: String, userId: String): Flow<PagingData<Comment>> =
        registry.userContent(sourceId)?.getUserComments(userId) ?: flowOf(PagingData.empty())
}

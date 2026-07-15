package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.repository.Source
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

/** Marker shared by every optional source capability. */
interface SourceConnector {
    val sourceId: String
}

interface ForumSearchConnector : SourceConnector {
    fun searchTopics(query: String): Flow<PagingData<Topic>>
    fun searchComments(query: String): Flow<PagingData<Comment>>
}

interface UserContentConnector : SourceConnector {
    fun getUserTopics(userId: String): Flow<PagingData<Topic>>
    fun getUserComments(userId: String): Flow<PagingData<Comment>>
}

interface PostingConnector : SourceConnector {
    suspend fun createThread(channelId: String, draft: PostDraft): PostResult
    suspend fun createReply(topicId: String, draft: PostDraft): PostResult
}

interface LoginConnector : SourceConnector {
    val strategy: LoginStrategy
    suspend fun login(inputs: Map<String, String>): Account
}

/**
 * Single capability lookup boundary. Features route by source id and never depend on a concrete
 * source implementation.
 */
interface ConnectorRegistry {
    fun source(sourceId: String): Source?
    fun search(sourceId: String): ForumSearchConnector?
    fun userContent(sourceId: String): UserContentConnector?
    fun posting(sourceId: String): PostingConnector?
    fun login(sourceId: String): LoginConnector?
}

class DefaultConnectorRegistry(
    sources: Set<Source>,
    searchConnectors: Set<ForumSearchConnector> = emptySet(),
    userContentConnectors: Set<UserContentConnector> = emptySet(),
    postingConnectors: Set<PostingConnector> = emptySet(),
    loginConnectors: Set<LoginConnector> = emptySet(),
) : ConnectorRegistry {
    private val sources = sources.uniqueBySourceId("source") { it.id }
    private val searches = searchConnectors.uniqueBySourceId("search") { it.sourceId }
    private val userContents = userContentConnectors.uniqueBySourceId("user content") { it.sourceId }
    private val postings = postingConnectors.uniqueBySourceId("posting") { it.sourceId }
    private val logins = loginConnectors.uniqueBySourceId("login") { it.sourceId }

    override fun source(sourceId: String): Source? = sources[sourceId]
    override fun search(sourceId: String): ForumSearchConnector? = searches[sourceId]
    override fun userContent(sourceId: String): UserContentConnector? = userContents[sourceId]
    override fun posting(sourceId: String): PostingConnector? = postings[sourceId]
    override fun login(sourceId: String): LoginConnector? = logins[sourceId]
}

private inline fun <T> Set<T>.uniqueBySourceId(
    capability: String,
    sourceId: (T) -> String,
): Map<String, T> = associateBy(sourceId).also { indexed ->
    require(indexed.size == size) { "Duplicate $capability connector source id" }
}

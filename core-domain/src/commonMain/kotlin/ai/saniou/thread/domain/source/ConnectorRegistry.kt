package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.repository.Source
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Marker shared by every optional source capability. */
interface SourceConnector {
    val sourceId: String
}

interface ForumSearchConnector : SourceConnector {
    fun searchTopics(query: String): Flow<PagingData<Topic>>
    fun searchComments(query: String): Flow<PagingData<Comment>>
    fun searchChannels(query: String): Flow<PagingData<Channel>> = flowOf(PagingData.empty())
    fun searchUsers(query: String): Flow<PagingData<Author>> = flowOf(PagingData.empty())

    /**
     * Search topics **within a channel/forum** (e.g. Tieba Mini searchPost).
     * [channelName] is the forum kw used by Tieba; [channelId] is kept for UI/context.
     * Default: empty (source has no in-channel search).
     */
    fun searchChannelTopics(
        channelId: String,
        channelName: String,
        query: String,
    ): Flow<PagingData<Topic>> = flowOf(PagingData.empty())
}

interface UserContentConnector : SourceConnector {
    fun getUserTopics(userId: String): Flow<PagingData<Topic>>
    fun getUserComments(userId: String): Flow<PagingData<Comment>>
}

interface SubCommentConnector : SourceConnector {
    suspend fun getSubComments(
        topicId: String,
        commentId: String,
        page: Int,
    ): Result<List<Comment>>
}

interface ReactionConnector : SourceConnector {
    suspend fun upvote(topicId: String, targetPostId: String): Result<Unit>
    /** Downvote / 点踩 a topic or post. Default: unsupported. */
    suspend fun downvote(topicId: String, targetPostId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Source '$sourceId' does not support downvote"))

    /**
     * Feed / 推荐流「不感兴趣」(e.g. Tieba submitDislike).
     * [reasonIds] / [extra] are platform-opaque CSV strings; empty is valid (generic dislike).
     */
    suspend fun submitNotInterested(
        topicId: String,
        channelId: String? = null,
        reasonIds: String = "",
        extra: String = "",
        clickTimeMs: Long = 0L,
    ): Result<String> =
        Result.failure(UnsupportedOperationException("Source '$sourceId' does not support not-interested"))
}

/**
 * User follow / unfollow and lightweight profile for relation UI.
 * Portrait-based sources (Tieba) resolve portrait from [userId] internally.
 */
interface UserRelationConnector : SourceConnector {
    suspend fun getProfile(userId: String): Result<UserRelationProfile>
    suspend fun follow(userId: String): Result<String>
    suspend fun unfollow(userId: String): Result<String>

    /** Update the currently authenticated user's profile. Default: unsupported. */
    suspend fun updateProfile(request: ProfileEditRequest): Result<String> =
        Result.failure(UnsupportedOperationException("Source '$sourceId' does not support profile edit"))
}

/**
 * Editable profile fields for sources that support self-profile modification.
 * [sex]: 0 unknown / 1 male / 2 female (Tieba convention).
 * [birthdayTimeSec]: unix seconds; 0 means leave/unset.
 */
data class ProfileEditRequest(
    val nickName: String,
    val intro: String,
    val sex: Int = 0,
    val birthdayTimeSec: Long = 0L,
    val birthdayShowStatus: Boolean = false,
)

data class UserRelationProfile(
    val userId: String,
    val name: String,
    val avatar: String? = null,
    val intro: String? = null,
    val isFollowing: Boolean = false,
    val fansCount: Long? = null,
    val followCount: Long? = null,
    val sex: Int? = null,
    val birthdayTimeSec: Long? = null,
    val birthdayShowStatus: Boolean? = null,
)

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
    fun subComments(sourceId: String): SubCommentConnector?
    fun reactions(sourceId: String): ReactionConnector?
    fun userRelation(sourceId: String): UserRelationConnector?
}

class DefaultConnectorRegistry(
    sources: Set<Source>,
    searchConnectors: Set<ForumSearchConnector> = emptySet(),
    userContentConnectors: Set<UserContentConnector> = emptySet(),
    postingConnectors: Set<PostingConnector> = emptySet(),
    loginConnectors: Set<LoginConnector> = emptySet(),
    subCommentConnectors: Set<SubCommentConnector> = emptySet(),
    reactionConnectors: Set<ReactionConnector> = emptySet(),
    userRelationConnectors: Set<UserRelationConnector> = emptySet(),
) : ConnectorRegistry {
    private val sources = sources.uniqueBySourceId("source") { it.id }
    private val searches = searchConnectors.uniqueBySourceId("search") { it.sourceId }
    private val userContents = userContentConnectors.uniqueBySourceId("user content") { it.sourceId }
    private val postings = postingConnectors.uniqueBySourceId("posting") { it.sourceId }
    private val logins = loginConnectors.uniqueBySourceId("login") { it.sourceId }
    private val subComments = subCommentConnectors.uniqueBySourceId("sub-comment") { it.sourceId }
    private val reactions = reactionConnectors.uniqueBySourceId("reaction") { it.sourceId }
    private val userRelations = userRelationConnectors.uniqueBySourceId("user relation") { it.sourceId }

    override fun source(sourceId: String): Source? = sources[sourceId]
    override fun search(sourceId: String): ForumSearchConnector? = searches[sourceId]
    override fun userContent(sourceId: String): UserContentConnector? = userContents[sourceId]
    override fun posting(sourceId: String): PostingConnector? = postings[sourceId]
    override fun login(sourceId: String): LoginConnector? = logins[sourceId]
    override fun subComments(sourceId: String): SubCommentConnector? = subComments[sourceId]
    override fun reactions(sourceId: String): ReactionConnector? = reactions[sourceId]
    override fun userRelation(sourceId: String): UserRelationConnector? = userRelations[sourceId]
}

private inline fun <T> Set<T>.uniqueBySourceId(
    capability: String,
    sourceId: (T) -> String,
): Map<String, T> = associateBy(sourceId).also { indexed ->
    require(indexed.size == size) { "Duplicate $capability connector source id" }
}

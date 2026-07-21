package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.data.source.social.ActivityPubSocialConnector
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.SocialPostEntity
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.ContentRelationKind
import ai.saniou.thread.domain.model.inbox.InboxKind
import ai.saniou.thread.domain.model.social.CursorDirection
import ai.saniou.thread.domain.model.social.SocialCursor
import ai.saniou.thread.domain.model.social.SocialIdentity
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialMedia
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.model.social.SocialRefreshReport
import ai.saniou.thread.domain.model.social.SocialSourceDescriptor
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.SocialRepository
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.paging3.QueryPagingSource
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.time.Clock

class SocialRepositoryImpl(
    private val db: Database,
    private val client: HttpClient,
) : SocialRepository {
    override fun observeSources(): Flow<List<SocialSourceDescriptor>> =
        db.socialQueries.getSocialSources().asFlow().mapToList(ioDispatcher).map { rows ->
            rows.map { SocialSourceDescriptor(it.id, it.displayName, it.baseUrl, it.enabled != 0L) }
        }

    override suspend fun getSources(): List<SocialSourceDescriptor> = withContext(ioDispatcher) {
        db.socialQueries.getSocialSources().executeAsList().map {
            SocialSourceDescriptor(it.id, it.displayName, it.baseUrl, it.enabled != 0L)
        }
    }

    override fun getTimeline(sourceIds: Set<String>?, query: String): Flow<PagingData<SocialPost>> {
        val ids = sourceIds ?: db.socialQueries.getEnabledSocialSources().executeAsList().mapTo(mutableSetOf()) { it.id }
        if (ids.isEmpty()) return flowOf(PagingData.empty())
        return Pager(
            config = threadPagingConfig(pageSize = 30),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.socialQueries,
                    context = Dispatchers.Default,
                    countQuery = db.socialQueries.countSocialTimeline(ids, query.trim()),
                    queryProvider = { limit, offset ->
                        db.socialQueries.getSocialTimelinePaging(ids, query.trim(), limit, offset)
                    },
                )
            },
        ).flow.map { data -> data.map(SocialPostEntity::toDomain) }
    }

    override suspend fun getCachedPosts(
        sourceIds: Set<String>?,
        limit: Long,
        offset: Long,
    ): List<SocialPost> = withContext(ioDispatcher) {
        val ids = sourceIds ?: db.socialQueries.getEnabledSocialSources()
            .executeAsList()
            .mapTo(mutableSetOf()) { it.id }
        if (ids.isEmpty()) emptyList()
        else db.socialQueries.getSocialTimelinePaging(ids, "", limit, offset)
            .executeAsList()
            .map(SocialPostEntity::toDomain)
    }

    override suspend fun getPost(sourceId: String, postId: String): SocialPost? = withContext(ioDispatcher) {
        db.socialQueries.getSocialPost(sourceId, postId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertSource(
        descriptor: SocialSourceDescriptor,
        accessToken: String?,
    ) = withContext(ioDispatcher) {
        val existing = db.socialQueries.getSocialSource(descriptor.id).executeAsOneOrNull()
        db.transaction {
            db.socialQueries.upsertSocialSource(
                id = descriptor.id,
                displayName = descriptor.displayName,
                baseUrl = descriptor.baseUrl.trimEnd('/'),
                enabled = descriptor.enabled.asLong(),
                lastSyncAt = existing?.lastSyncAt,
            )
            if (accessToken != null) {
                if (accessToken.isBlank()) db.keyValueQueries.deleteKeyValue(tokenKey(descriptor.id))
                else db.keyValueQueries.insertKeyValue(tokenKey(descriptor.id), accessToken)
            }
            if (existing != null && existing.baseUrl.trimEnd('/') != descriptor.baseUrl.trimEnd('/')) {
                db.socialQueries.deleteSocialPostsBySource(descriptor.id)
            }
        }
    }

    override suspend fun removeSource(sourceId: String) = withContext(ioDispatcher) {
        db.transaction {
            db.socialQueries.deleteSocialSource(sourceId)
            db.keyValueQueries.deleteKeyValue(tokenKey(sourceId))
        }
    }

    override suspend fun refresh(
        sourceIds: Set<String>?,
        direction: CursorDirection,
    ): SocialRefreshReport = supervisorScope {
        val sources = withContext(ioDispatcher) {
            db.socialQueries.getEnabledSocialSources().executeAsList()
                .filter { sourceIds == null || it.id in sourceIds }
        }
        val results = sources.map { source ->
            async {
                source.id to runCatching {
                    val connector = connector(source.id, source.baseUrl)
                    val storedCursor = withContext(ioDispatcher) {
                        db.socialQueries.getSocialCursor(source.id).executeAsOneOrNull()?.let { cursor ->
                            if (direction == CursorDirection.NEWER) cursor.newerCursor else cursor.olderCursor
                        }
                    }
                    val page = connector.timeline(
                        storedCursor?.let { SocialCursor(it, direction) },
                    ).getOrThrow()
                    persistPage(source.id, page.items, page.newerCursor, page.olderCursor)
                }
            }
        }.awaitAll()
        SocialRefreshReport(
            refreshedSourceIds = results.mapNotNullTo(mutableSetOf()) { (id, result) -> id.takeIf { result.isSuccess } },
            failures = results.mapNotNull { (id, result) ->
                result.exceptionOrNull()?.let { id to (it.message ?: it::class.simpleName ?: "Unknown error") }
            }.toMap(),
        )
    }

    override suspend fun interact(
        sourceId: String,
        postId: String,
        interaction: SocialInteraction,
        enabled: Boolean,
    ): Result<SocialPost> = runCatching {
        val source = withContext(ioDispatcher) {
            db.socialQueries.getSocialSource(sourceId).executeAsOneOrNull()
                ?: error("Social source not found: $sourceId")
        }
        val updated = connector(source.id, source.baseUrl)
            .interact(postId, interaction, enabled).getOrThrow()
        withContext(ioDispatcher) { db.socialQueries.upsert(updated) }
        updated
    }

    private suspend fun connector(sourceId: String, baseUrl: String): ActivityPubSocialConnector {
        val token = withContext(ioDispatcher) {
            db.keyValueQueries.getKeyValue(tokenKey(sourceId)).executeAsOneOrNull()?.content
        } ?: error("Social source '$sourceId' requires an access token")
        return ActivityPubSocialConnector(sourceId, baseUrl, token, client)
    }

    private suspend fun persistPage(
        sourceId: String,
        posts: List<SocialPost>,
        newerCursor: SocialCursor?,
        olderCursor: SocialCursor?,
    ) = withContext(ioDispatcher) {
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            posts.forEach { post ->
                val existing = db.socialQueries.getSocialPost(sourceId, post.id).executeAsOneOrNull()
                val duplicate = post.canonicalUrl?.let {
                    db.socialQueries.getSocialPostByCanonicalUrl(it).executeAsOneOrNull()
                }
                db.socialQueries.upsert(post)
                if (existing == null) {
                    val muted = db.inboxEventQueries.getInboxSourcePreference(sourceId)
                        .executeAsOneOrNull()?.muted ?: 0L
                    db.inboxEventQueries.upsertInboxEvent(
                        id = "social:$sourceId:${post.id}",
                        kind = InboxKind.SUBSCRIPTION_UPDATE.name,
                        sourceId = sourceId,
                        title = post.author.displayName,
                        summary = post.body.stripHtml().take(320),
                        contentKind = ContentReferenceKind.SOCIAL_POST.name,
                        contentId = post.id,
                        contentSourceId = sourceId,
                        parentId = null,
                        canonicalUrl = post.canonicalUrl,
                        occurredAt = post.createdAtEpochMillis,
                        readAt = null,
                        muted = muted,
                        priority = 0,
                    )
                }
                post.replyToId?.takeIf { it != post.id }?.let { targetId ->
                    db.contentGraphQueries.upsertSocialEdge(post, targetId, ContentRelationKind.REPLY_TO, now)
                }
                post.repostOfId?.takeIf { it != post.id }?.let { targetId ->
                    db.contentGraphQueries.upsertSocialEdge(post, targetId, ContentRelationKind.REPOST_OF, now)
                }
                duplicate?.takeIf { it.id != post.id || it.sourceId != post.sourceId }?.let { target ->
                    db.contentGraphQueries.upsertContentEdge(
                        fromKind = ContentReferenceKind.SOCIAL_POST.name,
                        fromId = post.id,
                        fromSourceId = post.sourceId,
                        fromParentId = "",
                        toKind = ContentReferenceKind.SOCIAL_POST.name,
                        toId = target.id,
                        toSourceId = target.sourceId,
                        toParentId = "",
                        relation = ContentRelationKind.CANONICAL_DUPLICATE.name,
                        weight = 1.0,
                        createdAt = now,
                    )
                }
                post.canonicalUrl?.let { url ->
                    db.articleQueries.getArticleByLink(url).executeAsOneOrNull()?.let { article ->
                        db.contentGraphQueries.upsertContentEdge(
                            fromKind = ContentReferenceKind.SOCIAL_POST.name,
                            fromId = post.id,
                            fromSourceId = post.sourceId,
                            fromParentId = "",
                            toKind = ContentReferenceKind.ARTICLE.name,
                            toId = article.id,
                            toSourceId = article.feedSourceId,
                            toParentId = "",
                            relation = ContentRelationKind.CANONICAL_DUPLICATE.name,
                            weight = 1.0,
                            createdAt = now,
                        )
                    }
                }
            }
            db.socialQueries.upsertSocialCursor(
                sourceId = sourceId,
                newerCursor = newerCursor?.value,
                olderCursor = olderCursor?.value,
                updatedAt = now,
            )
            db.socialQueries.updateSocialSourceSync(now, sourceId)
        }
    }

    private fun tokenKey(sourceId: String) = "social_access_token_$sourceId"
}

private suspend fun ai.saniou.thread.db.table.SocialQueries.upsert(post: SocialPost) {
    upsertSocialPost(
        id = post.id,
        sourceId = post.sourceId,
        authorId = post.author.id,
        authorName = post.author.displayName,
        authorHandle = post.author.handle,
        authorAvatarUrl = post.author.avatarUrl,
        authorVerified = post.author.verified.asLong(),
        body = post.body,
        createdAt = post.createdAtEpochMillis,
        contentWarning = post.contentWarning,
        mediaJson = socialJson.encodeToString<List<SocialMedia>>(post.media),
        interactionCountsJson = socialJson.encodeToString<Map<SocialInteraction, Long>>(post.interactionCounts),
        permittedInteractionsJson = socialJson.encodeToString<Set<SocialInteraction>>(post.permittedInteractions),
        activeInteractionsJson = socialJson.encodeToString<Set<SocialInteraction>>(post.activeInteractions),
        canonicalUrl = post.canonicalUrl,
        replyToId = post.replyToId,
        repostOfId = post.repostOfId,
    )
}

private suspend fun ai.saniou.thread.db.table.ContentGraphQueries.upsertSocialEdge(
    post: SocialPost,
    targetId: String,
    relation: ContentRelationKind,
    now: Long,
) = upsertContentEdge(
    fromKind = ContentReferenceKind.SOCIAL_POST.name,
    fromId = post.id,
    fromSourceId = post.sourceId,
    fromParentId = "",
    toKind = ContentReferenceKind.SOCIAL_POST.name,
    toId = targetId,
    toSourceId = post.sourceId,
    toParentId = "",
    relation = relation.name,
    weight = 1.0,
    createdAt = now,
)

private fun SocialPostEntity.toDomain() = SocialPost(
    id = id,
    sourceId = sourceId,
    author = SocialIdentity(authorId, authorName, authorHandle, authorAvatarUrl, authorVerified != 0L),
    body = body,
    createdAtEpochMillis = createdAt,
    contentWarning = contentWarning,
    media = socialJson.decodeFromString(mediaJson),
    interactionCounts = socialJson.decodeFromString(interactionCountsJson),
    permittedInteractions = socialJson.decodeFromString(permittedInteractionsJson),
    activeInteractions = socialJson.decodeFromString(activeInteractionsJson),
    canonicalUrl = canonicalUrl,
    replyToId = replyToId,
    repostOfId = repostOfId,
)

private val socialJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
private fun Boolean.asLong() = if (this) 1L else 0L
private fun String.stripHtml() = replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

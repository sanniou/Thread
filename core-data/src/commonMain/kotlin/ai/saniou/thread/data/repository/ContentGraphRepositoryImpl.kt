package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.ContentGraphEdgeEntity
import ai.saniou.thread.domain.model.content.ContentEdge
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.ContentRelationKind
import ai.saniou.thread.domain.model.content.RelatedContent
import ai.saniou.thread.domain.paging.threadPagingConfig
import ai.saniou.thread.domain.repository.ContentGraphRepository
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant

class ContentGraphRepositoryImpl(
    private val db: Database,
) : ContentGraphRepository {
    override suspend fun upsert(edge: ContentEdge) = withContext(ioDispatcher) {
        db.contentGraphQueries.upsert(edge)
        Unit
    }

    override suspend fun removeFor(reference: ContentReference) = withContext(ioDispatcher) {
        db.contentGraphQueries.deleteContentEdgesFor(
            reference.kind.name, reference.id, reference.sourceId.orEmpty(),
        )
        Unit
    }

    override fun getRelated(reference: ContentReference): Flow<PagingData<RelatedContent>> = Pager(
        config = threadPagingConfig(),
        pagingSourceFactory = {
            QueryPagingSource(
                transacter = db.contentGraphQueries,
                context = Dispatchers.Default,
                countQuery = db.contentGraphQueries.countRelatedEdges(
                    reference.kind.name, reference.id, reference.sourceId.orEmpty(),
                ),
                queryProvider = { limit, offset ->
                    db.contentGraphQueries.getRelatedEdges(
                        reference.kind.name, reference.id, reference.sourceId.orEmpty(), limit, offset,
                    )
                },
            )
        },
    ).flow.map { paging -> paging.map(::resolveRelated) }

    override suspend fun rebuild(reference: ContentReference): Int = withContext(ioDispatcher) {
        val edges = when (reference.kind) {
            ContentReferenceKind.TOPIC -> topicEdges(reference)
            ContentReferenceKind.COMMENT -> commentEdges(reference)
            ContentReferenceKind.ARTICLE -> articleEdges(reference)
            ContentReferenceKind.SOCIAL_POST -> socialEdges(reference)
            ContentReferenceKind.EXTERNAL_URL -> emptyList()
        }.distinctBy { edge -> Triple(edge.to.kind to edge.to.id, edge.to.sourceId, edge.relation) }
            .filterNot { it.from == it.to }
            .take(MAX_EDGES_PER_NODE)
        db.transaction {
            db.contentGraphQueries.deleteOutgoingContentEdges(
                reference.kind.name, reference.id, reference.sourceId.orEmpty(),
            )
            edges.forEach { edge -> db.contentGraphQueries.upsert(edge) }
        }
        edges.size
    }

    private fun topicEdges(reference: ContentReference): List<ContentEdge> {
        val sourceId = checkNotNull(reference.sourceId)
        val topic = db.topicQueries.getTopic(sourceId, reference.id).executeAsOneOrNull() ?: return emptyList()
        val now = Clock.System.now()
        return buildList {
            db.topicQueries.getTopicsByUserHashOffset(sourceId, topic.authorId, 20, 0).executeAsList()
                .filterNot { it.id == topic.id }
                .forEach { related -> add(edge(reference, topicRef(related.sourceId, related.id), ContentRelationKind.SAME_AUTHOR, .62f, now)) }
            db.topicTagQueries.getTagsForTopic(sourceId, topic.id).executeAsList().forEach { tag ->
                db.topicTagQueries.getTopicsForTag(tag.id, 24).executeAsList()
                    .filterNot { it.sourceId == sourceId && it.topicId == topic.id }
                    .forEach { target -> add(edge(reference, topicRef(target.sourceId, target.topicId), ContentRelationKind.SAME_TAG, .72f, now)) }
            }
            (topic.content.orEmpty() + " " + topic.summary.orEmpty()).forumTopicIds().forEach { id ->
                if (id != topic.id) add(edge(reference, topicRef(sourceId, id), ContentRelationKind.REFERENCES, .9f, now))
            }
        }
    }

    private fun commentEdges(reference: ContentReference): List<ContentEdge> {
        val sourceId = checkNotNull(reference.sourceId)
        val comment = db.commentQueries.getCommentById(sourceId, reference.id).executeAsOneOrNull() ?: return emptyList()
        val now = Clock.System.now()
        return buildList {
            add(edge(reference, topicRef(sourceId, comment.topicId), ContentRelationKind.REPLY_TO, 1f, now))
            comment.replyToId?.takeIf { it != comment.id }?.let { parent ->
                add(edge(reference, ContentReference(ContentReferenceKind.COMMENT, parent, sourceId, comment.topicId), ContentRelationKind.REPLY_TO, 1f, now))
            }
            comment.content.forumTopicIds().filterNot { it == comment.topicId }.forEach { id ->
                add(edge(reference, topicRef(sourceId, id), ContentRelationKind.REFERENCES, .9f, now))
            }
        }
    }

    private fun articleEdges(reference: ContentReference): List<ContentEdge> {
        val article = db.articleQueries.getArticleById(reference.id).executeAsOneOrNull() ?: return emptyList()
        val author = article.author?.takeIf(String::isNotBlank) ?: return emptyList()
        val now = Clock.System.now()
        return db.articleQueries.getArticlesByAuthor(author, article.id, 30).executeAsList().map { related ->
            edge(
                reference,
                ContentReference(ContentReferenceKind.ARTICLE, related.id, related.feedSourceId, canonicalUrl = related.link),
                ContentRelationKind.SAME_AUTHOR,
                .64f,
                now,
            )
        }
    }

    private fun socialEdges(reference: ContentReference): List<ContentEdge> {
        val sourceId = checkNotNull(reference.sourceId)
        val post = db.socialQueries.getSocialPost(sourceId, reference.id).executeAsOneOrNull() ?: return emptyList()
        val now = Clock.System.now()
        return buildList {
            post.replyToId?.takeIf { it != post.id }?.let { target ->
                add(edge(reference, socialRef(sourceId, target), ContentRelationKind.REPLY_TO, 1f, now))
            }
            post.repostOfId?.takeIf { it != post.id }?.let { target ->
                add(edge(reference, socialRef(sourceId, target), ContentRelationKind.REPOST_OF, 1f, now))
            }
            db.socialQueries.getSocialPostsByAuthor(sourceId, post.authorId, 30).executeAsList()
                .filterNot { it.id == post.id }
                .forEach { related -> add(edge(reference, socialRef(sourceId, related.id, related.canonicalUrl), ContentRelationKind.SAME_AUTHOR, .6f, now)) }
        }
    }

    private fun resolveRelated(edge: ContentGraphEdgeEntity): RelatedContent {
        val kind = runCatching { ContentReferenceKind.valueOf(edge.toKind) }.getOrDefault(ContentReferenceKind.EXTERNAL_URL)
        val reference = ContentReference(
            kind = kind,
            id = edge.toId,
            sourceId = edge.toSourceId.takeIf(String::isNotBlank),
            parentId = edge.toParentId.takeIf(String::isNotBlank),
        )
        val metadata = metadata(reference)
        return RelatedContent(
            reference = reference,
            relation = runCatching { ContentRelationKind.valueOf(edge.relation) }.getOrDefault(ContentRelationKind.REFERENCES),
            title = metadata.title,
            summary = metadata.summary,
            sourceName = metadata.sourceName,
            publishedAt = Instant.fromEpochMilliseconds(metadata.publishedAt),
            weight = edge.weight.toFloat().coerceIn(0f, 1f),
        )
    }

    private fun metadata(reference: ContentReference): Metadata = when (reference.kind) {
        ContentReferenceKind.TOPIC -> db.topicQueries.getTopic(checkNotNull(reference.sourceId), reference.id)
            .executeAsOneOrNull()?.let { Metadata(it.title ?: "主题 #${it.id}", it.summary ?: it.content.orEmpty(), it.sourceId, maxOf(it.createdAt, it.lastReplyAt)) }
        ContentReferenceKind.COMMENT -> db.commentQueries.getCommentById(checkNotNull(reference.sourceId), reference.id)
            .executeAsOneOrNull()?.let { Metadata(it.title ?: "${it.floor} 楼回复", it.content, it.sourceId, it.createdAt) }
        ContentReferenceKind.ARTICLE -> db.articleQueries.getArticleById(reference.id).executeAsOneOrNull()
            ?.let { Metadata(it.title, it.description, it.feedSourceId, it.publishDate) }
        ContentReferenceKind.SOCIAL_POST -> db.socialQueries.getSocialPost(checkNotNull(reference.sourceId), reference.id)
            .executeAsOneOrNull()?.let { Metadata(it.authorName, it.body, it.sourceId, it.createdAt) }
        ContentReferenceKind.EXTERNAL_URL -> Metadata(reference.canonicalUrl ?: reference.id, "", "web", 0)
    } ?: Metadata("内容 ${reference.id}", "本地缓存已移除", reference.sourceId ?: "unknown", 0)

    private fun edge(
        from: ContentReference,
        to: ContentReference,
        relation: ContentRelationKind,
        weight: Float,
        now: Instant,
    ) = ContentEdge(from, to, relation, weight, now)

    private fun topicRef(sourceId: String, id: String) = ContentReference(ContentReferenceKind.TOPIC, id, sourceId)
    private fun socialRef(sourceId: String, id: String, url: String? = null) =
        ContentReference(ContentReferenceKind.SOCIAL_POST, id, sourceId, canonicalUrl = url)

    private data class Metadata(val title: String, val summary: String, val sourceName: String, val publishedAt: Long)

    private companion object { const val MAX_EDGES_PER_NODE = 120 }
}

private suspend fun ai.saniou.thread.db.table.ContentGraphQueries.upsert(edge: ContentEdge) = upsertContentEdge(
    fromKind = edge.from.kind.name,
    fromId = edge.from.id,
    fromSourceId = edge.from.sourceId.orEmpty(),
    fromParentId = edge.from.parentId.orEmpty(),
    toKind = edge.to.kind.name,
    toId = edge.to.id,
    toSourceId = edge.to.sourceId.orEmpty(),
    toParentId = edge.to.parentId.orEmpty(),
    relation = edge.relation.name,
    weight = edge.weight.toDouble(),
    createdAt = edge.createdAt.toEpochMilliseconds(),
)

private fun String.forumTopicIds(): Set<String> = Regex("(?:/t/|/p/)([0-9]+)")
    .findAll(this).map { it.groupValues[1] }.take(40).toSet()

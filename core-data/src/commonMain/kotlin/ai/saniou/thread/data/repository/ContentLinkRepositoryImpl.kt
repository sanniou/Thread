package ai.saniou.thread.data.repository

import ai.saniou.corecommon.coroutines.ioDispatcher
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.LinkResolution
import ai.saniou.thread.domain.repository.ContentLinkRepository
import ai.saniou.thread.domain.source.SourceCatalog
import kotlinx.coroutines.withContext

/** Resolves only from durable local state. Opening a link never performs a surprise network request. */
class ContentLinkRepositoryImpl(
    private val db: Database,
    private val sourceCatalog: SourceCatalog,
) : ContentLinkRepository {
    override suspend fun resolveUrl(url: String): LinkResolution = withContext(ioDispatcher) {
        val normalized = url.trim()
        if (normalized.isBlank()) return@withContext LinkResolution.Unsupported("链接为空")
        parseThreadUrl(normalized)?.let { return@withContext resolveReferenceLocal(it) }
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            return@withContext LinkResolution.Unsupported("仅支持 Thread、HTTP 和 HTTPS 链接")
        }
        db.articleQueries.getArticleByLink(normalized).executeAsOneOrNull()?.let { article ->
            return@withContext LinkResolution.Internal(
                ContentReference(
                    kind = ContentReferenceKind.ARTICLE,
                    id = article.id,
                    sourceId = article.feedSourceId,
                    canonicalUrl = normalized,
                ),
                availableOffline = true,
            )
        }
        db.socialQueries.getSocialPostByCanonicalUrl(normalized).executeAsOneOrNull()?.let { post ->
            return@withContext LinkResolution.Internal(
                ContentReference(
                    kind = ContentReferenceKind.SOCIAL_POST,
                    id = post.id,
                    sourceId = post.sourceId,
                    canonicalUrl = normalized,
                ),
                availableOffline = true,
            )
        }
        parseForumUrl(normalized)?.let { return@withContext resolveReferenceLocal(it) }
        LinkResolution.External(normalized)
    }

    override suspend fun resolveReference(reference: ContentReference): LinkResolution =
        withContext(ioDispatcher) { resolveReferenceLocal(reference) }

    private suspend fun resolveReferenceLocal(reference: ContentReference): LinkResolution {
        if (reference.kind == ContentReferenceKind.EXTERNAL_URL) {
            val url = reference.canonicalUrl ?: reference.id
            return if (url.startsWith("https://") || url.startsWith("http://")) {
                LinkResolution.External(url)
            } else LinkResolution.Unsupported("外部链接协议不受支持")
        }
        if (reference.kind == ContentReferenceKind.SOCIAL_POST) {
            val post = db.socialQueries.getSocialPost(checkNotNull(reference.sourceId), reference.id)
                .executeAsOneOrNull()
            return LinkResolution.Internal(
                reference.copy(canonicalUrl = post?.canonicalUrl ?: reference.canonicalUrl),
                post != null,
            )
        }
        val available = when (reference.kind) {
            ContentReferenceKind.TOPIC -> db.topicQueries.getTopic(
                checkNotNull(reference.sourceId), reference.id,
            ).executeAsOneOrNull() != null
            ContentReferenceKind.COMMENT -> db.commentQueries.getCommentById(
                checkNotNull(reference.sourceId), reference.id,
            ).executeAsOneOrNull() != null
            ContentReferenceKind.ARTICLE -> db.articleQueries.getArticleById(reference.id)
                .executeAsOneOrNull() != null
            ContentReferenceKind.SOCIAL_POST -> error("Handled above")
            ContentReferenceKind.EXTERNAL_URL -> false
        }
        return LinkResolution.Internal(reference, available)
    }

    private fun parseThreadUrl(url: String): ContentReference? {
        val parts = url.removePrefix("thread://").split('/').filter(String::isNotBlank)
        if (!url.startsWith("thread://")) return null
        return when {
            parts.getOrNull(0) == "forum" && parts.size >= 3 -> ContentReference(
                ContentReferenceKind.TOPIC, parts[2], sourceId = parts[1], canonicalUrl = url,
            )
            parts.getOrNull(0) == "reader" && parts.size >= 2 -> ContentReference(
                ContentReferenceKind.ARTICLE, parts[1], canonicalUrl = url,
            )
            parts.getOrNull(0) == "social" && parts.size >= 3 -> ContentReference(
                ContentReferenceKind.SOCIAL_POST, parts[2], sourceId = parts[1], canonicalUrl = url,
            )
            else -> null
        }
    }

    private fun parseForumUrl(url: String): ContentReference? {
        val pathId = Regex("/(?:p|t|thread)/(?:[^/]+/)?([0-9]+)(?:[/?#]|$)")
            .find(url)?.groupValues?.getOrNull(1) ?: return null
        val sourceId = when {
            "tieba.baidu.com" in url -> "tieba"
            sourceCatalog.descriptors.value.any { descriptor ->
                descriptor.baseUrl?.let { base -> host(base) == host(url) } == true
            } -> sourceCatalog.descriptors.value.first { descriptor ->
                descriptor.baseUrl?.let { base -> host(base) == host(url) } == true
            }.id
            "nmb" in host(url) -> "nmb"
            else -> return null
        }
        return ContentReference(
            kind = ContentReferenceKind.TOPIC,
            id = pathId,
            sourceId = sourceId,
            canonicalUrl = url,
        )
    }

    private fun host(url: String): String = url.substringAfter("://", url)
        .substringBefore('/').substringBefore(':').lowercase()
}

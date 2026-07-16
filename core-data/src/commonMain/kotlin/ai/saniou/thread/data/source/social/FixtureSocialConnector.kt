package ai.saniou.thread.data.source.social

import ai.saniou.thread.domain.model.social.CursorDirection
import ai.saniou.thread.domain.model.social.SocialCapabilities
import ai.saniou.thread.domain.model.social.SocialCursor
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.model.social.SocialTimelinePage
import ai.saniou.thread.domain.source.SocialConnector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Deterministic runtime adapter for connector development, UI fixtures and contract gates. */
class FixtureSocialConnector(
    override val sourceId: String,
    fixtures: List<SocialPost>,
    private val pageSize: Int = 20,
    override val capabilities: SocialCapabilities = SocialCapabilities(
        interactions = SocialInteraction.entries.toSet(),
    ),
) : SocialConnector {
    private val mutex = Mutex()
    private val posts = fixtures.sortedByDescending(SocialPost::createdAtEpochMillis).associateByTo(linkedMapOf()) { it.id }
    private val activeInteractions = mutableMapOf<String, MutableSet<SocialInteraction>>()

    init {
        require(sourceId.isNotBlank())
        require(pageSize in 1..100)
        require(capabilities.maxMediaItems in 0..16)
        require(fixtures.distinctBy(SocialPost::id).size == fixtures.size) { "Duplicate fixture post id" }
        fixtures.forEach { post ->
            require(post.sourceId == sourceId) { "Fixture ${post.id} belongs to ${post.sourceId}, expected $sourceId" }
            require(post.id.isNotBlank())
            require(post.media.size <= capabilities.maxMediaItems)
            require(post.permittedInteractions.all { it in capabilities.interactions })
        }
    }

    override suspend fun timeline(cursor: SocialCursor?): Result<SocialTimelinePage> = runCatching {
        mutex.withLock {
            val offset = cursor?.let(::decodeCursor) ?: 0
            val snapshot = posts.values.toList()
            require(offset in 0..snapshot.size) { "Cursor is outside the fixture timeline" }
            val items = snapshot.drop(offset).take(pageSize)
            val nextOffset = offset + items.size
            SocialTimelinePage(
                items = items,
                newerCursor = if (offset > 0) SocialCursor(
                    encodeCursor((offset - pageSize).coerceAtLeast(0)), CursorDirection.NEWER,
                ) else null,
                olderCursor = if (nextOffset < snapshot.size) SocialCursor(
                    encodeCursor(nextOffset), CursorDirection.OLDER,
                ) else null,
            )
        }
    }

    override suspend fun interact(
        postId: String,
        interaction: SocialInteraction,
        enabled: Boolean,
    ): Result<SocialPost> = runCatching {
        mutex.withLock {
            require(interaction in capabilities.interactions) { "$interaction is unsupported" }
            val current = posts[postId] ?: error("Post not found: $postId")
            require(interaction in current.permittedInteractions) { "$interaction is not permitted for $postId" }
            val active = activeInteractions.getOrPut(postId) { mutableSetOf() }
            val changed = if (enabled) active.add(interaction) else active.remove(interaction)
            if (!changed) return@withLock current
            val oldCount = current.interactionCounts[interaction] ?: 0L
            val nextCount = if (enabled) oldCount + 1 else (oldCount - 1).coerceAtLeast(0)
            current.copy(
                interactionCounts = current.interactionCounts + (interaction to nextCount),
            ).also { posts[postId] = it }
        }
    }

    private fun encodeCursor(offset: Int) = "fixture:$sourceId:$offset"

    private fun decodeCursor(cursor: SocialCursor): Int {
        val prefix = "fixture:$sourceId:"
        require(cursor.value.startsWith(prefix)) { "Cursor belongs to another connector" }
        return cursor.value.removePrefix(prefix).toIntOrNull() ?: error("Malformed fixture cursor")
    }
}

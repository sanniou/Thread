package ai.saniou.thread.data.source.discourse

import ai.saniou.thread.data.source.discourse.remote.dto.DiscoursePost
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopic
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseTopicPoster
import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseUser
import ai.saniou.thread.data.source.discourse.remote.dto.toDomainComment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscourseMappingTest {
    @Test
    fun mapsTopicIdentityTagsCategoryAndAbsoluteMedia() {
        val topic = DiscourseTopic(
            id = 42,
            title = "Architecture",
            slug = "architecture",
            replyCount = 3,
            imageUrl = "/uploads/default/topic.png",
            excerpt = "Summary",
            createdAt = "2026-01-01T00:00:00Z",
            lastPostedAt = "2026-01-02T00:00:00Z",
            categoryId = 7,
            tags = listOf("kotlin"),
            posters = listOf(DiscourseTopicPoster(description = "Original Poster", userId = 9)),
        ).toPost(
            usersMap = mapOf(9L to DiscourseUser(9, "alice", "Alice", "/avatar/{size}/9.png")),
            sourceId = "tech_forum",
            sourceName = "Tech Forum",
            sourceBaseUrl = "https://forum.example/",
            categoryNames = mapOf("7" to "Engineering"),
        )

        assertEquals("Engineering", topic.channelName)
        assertEquals("tech_forum:kotlin", topic.tags.single().id)
        assertEquals("https://forum.example/uploads/default/topic.png", topic.images.single().originalUrl)
        assertEquals("https://forum.example/avatar/80/9.png", topic.author.avatar)
    }

    @Test
    fun extractsImagesAndModerationStateFromPost() {
        val comment = DiscoursePost(
            id = 11,
            name = "Moderator",
            username = "mod",
            avatarTemplate = "/avatar/{size}/mod.png",
            cooked = "<p>Hello</p><img src='/uploads/a.webp'>",
            createdAt = "2026-01-01T00:00:00Z",
            postNumber = 1,
            replyToPostNumber = 1,
            moderator = true,
            trustLevel = 4,
        ).toDomainComment(
            sourceId = "forum",
            sourceName = "Forum",
            threadId = "42",
            sourceBaseUrl = "https://forum.example/",
            replyTargetId = "10",
        )

        assertTrue(comment.isAdmin)
        assertTrue(comment.isPo)
        assertEquals(4, comment.authorLevel)
        assertEquals("Forum", comment.author.sourceName)
        assertEquals("10", comment.replyToId)
        assertEquals("https://forum.example/uploads/a.webp", comment.images.single().originalUrl)
    }
}

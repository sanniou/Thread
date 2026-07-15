package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.PagedResult
import ai.saniou.thread.domain.model.SourceCapabilities
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.user.LoginStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SourceFeedPageTest {

    @Test
    fun commonFeedPageRefreshesCatalogAndPrefersTimeline() = runBlocking {
        val source = FakeSource()

        val result = source.getFeedPage(null)

        assertTrue(result.isSuccess)
        assertEquals(1, source.fetchCount)
        assertEquals("timeline", source.requestedChannelId)
        assertTrue(source.requestedTimeline)
    }

    @Test
    fun unsupportedConnectorFailsBeforeLoadingCatalog() = runBlocking {
        val source = FakeSource(
            capabilities = SourceCapabilities(supportsFeedAggregation = false),
        )

        val result = source.getFeedPage(null)

        assertTrue(result.isFailure)
        assertEquals(0, source.fetchCount)
    }
}

private class FakeSource(
    override val capabilities: SourceCapabilities = SourceCapabilities(),
) : Source {
    override val id: String = "fake"
    override val name: String = "Fake"
    override val isInitialized: Flow<Boolean> = flowOf(true)
    override val loginStrategy: LoginStrategy = LoginStrategy.Api("Fake login")

    private val channels = MutableStateFlow<List<Channel>>(emptyList())
    var fetchCount: Int = 0
    var requestedChannelId: String? = null
    var requestedTimeline: Boolean = false

    override fun observeChannels(): Flow<List<Channel>> = channels

    override suspend fun fetchChannels(): Result<Unit> {
        fetchCount += 1
        channels.value = listOf(
            channel(id = "forum", tag = null),
            channel(id = "timeline", tag = "timeline"),
        )
        return Result.success(Unit)
    }

    override suspend fun getChannelTopics(
        channelId: String,
        cursor: String?,
        isTimeline: Boolean,
    ): Result<PagedResult<Topic>> {
        requestedChannelId = channelId
        requestedTimeline = isTimeline
        return Result.success(PagedResult(emptyList(), prevCursor = null, nextCursor = null))
    }

    override suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic> =
        Result.failure(UnsupportedOperationException())

    override fun getChannel(channelId: String): Flow<Channel?> =
        flowOf(channels.value.firstOrNull { it.id == channelId })
}

private fun channel(id: String, tag: String?): Channel = Channel(
    id = id,
    name = id,
    displayName = id,
    description = "",
    descriptionText = null,
    groupId = "group",
    groupName = "Group",
    sourceName = "fake",
    sort = 0,
    tag = tag,
    topicCount = null,
    postCount = null,
    autoDelete = null,
)

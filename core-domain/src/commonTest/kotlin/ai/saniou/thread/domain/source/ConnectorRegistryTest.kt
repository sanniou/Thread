package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.Source
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class ConnectorRegistryTest {
    @Test
    fun resolvesEachOptionalCapabilityBySourceId() {
        val source = FakeSource("alpha")
        val search = FakeSearchConnector("alpha")
        val registry = DefaultConnectorRegistry(
            sources = setOf(source),
            searchConnectors = setOf(search),
        )

        assertSame(source, registry.source("alpha"))
        assertSame(search, registry.search("alpha"))
        assertNull(registry.posting("alpha"))
        assertNull(registry.source("missing"))
    }

    @Test
    fun rejectsDuplicateCapabilityRegistration() {
        val error = assertFailsWith<IllegalArgumentException> {
            DefaultConnectorRegistry(
                sources = setOf(FakeSource("alpha")),
                searchConnectors = setOf(
                    FakeSearchConnector("alpha", marker = 1),
                    FakeSearchConnector("alpha", marker = 2),
                ),
            )
        }

        assertEquals("Duplicate search connector source id", error.message)
    }
}

private data class FakeSearchConnector(
    override val sourceId: String,
    val marker: Int = 0,
) : ForumSearchConnector {
    override fun searchTopics(query: String): Flow<PagingData<Topic>> = flowOf(PagingData.empty())
    override fun searchComments(query: String): Flow<PagingData<Comment>> = flowOf(PagingData.empty())
}

private class FakeSource(
    override val id: String,
) : Source {
    override val name: String = id
    override val isInitialized: Flow<Boolean> = flowOf(true)
    override val loginStrategy: LoginStrategy = LoginStrategy.Api("Login")

    override fun observeChannels(): Flow<List<Channel>> = flowOf(emptyList())
    override suspend fun fetchChannels(): Result<Unit> = Result.success(Unit)
    override suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic> =
        Result.failure(UnsupportedOperationException())
    override fun getChannel(channelId: String): Flow<Channel?> = flowOf(null)
}

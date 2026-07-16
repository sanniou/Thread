package ai.saniou.thread.domain.source

import ai.saniou.thread.domain.model.PagedResult
import ai.saniou.thread.domain.model.SourceCapabilities
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.PostDraft
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.PostResult
import ai.saniou.thread.domain.repository.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SourceConformanceTest {
    @Test
    fun acceptsCapabilitiesBackedByMatchingConnectors() {
        val source = ContractSource(
            capabilities = SourceCapabilities(supportsReplies = true, supportsAttachments = true),
        )
        val report = SourceConformance.inspect(source, posting = ContractPosting(source.id))

        assertTrue(report.isValid)
        report.requireValid()
    }

    @Test
    fun rejectsAdvertisedCapabilityWithoutConnector() {
        val source = ContractSource(capabilities = SourceCapabilities(supportsSearch = true))

        assertFailsWith<IllegalArgumentException> {
            SourceConformance.inspect(source).requireValid()
        }
    }

    @Test
    fun rejectsHiddenRegisteredCapability() {
        val source = ContractSource()

        assertFailsWith<IllegalArgumentException> {
            SourceConformance.inspect(source, posting = ContractPosting(source.id)).requireValid()
        }
    }
}

private class ContractPosting(override val sourceId: String) : PostingConnector {
    override suspend fun createThread(channelId: String, draft: PostDraft) = PostResult(sourceId)
    override suspend fun createReply(topicId: String, draft: PostDraft) = PostResult(sourceId)
}

private class ContractSource(
    override val capabilities: SourceCapabilities = SourceCapabilities(),
) : Source {
    override val id = "contract"
    override val name = "Contract"
    override val isInitialized: Flow<Boolean> = flowOf(true)
    override val loginStrategy: LoginStrategy = LoginStrategy.Api("Login")
    override fun observeChannels(): Flow<List<Channel>> = flowOf(emptyList())
    override suspend fun fetchChannels() = Result.success(Unit)
    override suspend fun getChannelTopics(channelId: String, cursor: String?, isTimeline: Boolean) =
        Result.success(PagedResult<Topic>(emptyList(), null, null))
    override suspend fun getTopicDetail(threadId: String, page: Int) =
        Result.failure<Topic>(UnsupportedOperationException())
    override fun getChannel(channelId: String): Flow<Channel?> = flowOf(null)
}

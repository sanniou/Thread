package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.forum.Channel as Channel
import ai.saniou.thread.domain.model.forum.Topic as Topic
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.repository.Source
import kotlinx.coroutines.coroutineScope
import kotlin.time.ExperimentalTime

class SourceRepositoryImpl(
    private val sources: Set<Source>,
) : SourceRepository {

    private val sourceMap by lazy { sources.associateBy { it.id } }

    @OptIn(ExperimentalTime::class)
    override suspend fun getAggregatedFeed(page: Int): Result<List<Topic>> = coroutineScope {
        // 简单聚合：并行获取所有来源的第一页数据，然后合并排序
        // 实际应用中需要更复杂的策略，例如根据用户配置、分页等
        TODO()
    }

    override fun getAvailableSources(): List<Source> {
        return sources.toList()
    }

    override fun getSource(sourceId: String): Source? {
        return sources.find { it.id == sourceId }
    }
}

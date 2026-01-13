package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.ClientVersion
import ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.remote.TiebaProtoBuilder
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.source.TrendSource
import app.cash.paging.PagingData
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListRequest
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListRequestData
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequest
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequestData
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListRequest
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListRequestData
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequest
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequestData
import kotlinx.coroutines.flow.Flow
import kotlin.collections.emptyList

class TiebaTrendSource(
    private val officialProtobufTiebaApiV11: OfficialProtobufTiebaApi,
    private val tiebaParameterProvider: TiebaParameterProvider,
) : TrendSource {
    override val id: String = TiebaMapper.SOURCE_ID
    override val name: String = TiebaMapper.SOURCE_NAME

    override fun getTrendTabs(): List<TrendTab> {
        return listOf(
            TrendTab(id = "tieba_hot", name = "热议"),
            TrendTab(id = "tieba_topic", name = "话题"),
            TrendTab(id = "tieba_concern", name = "关注"),
            TrendTab(id = "tieba_recommend", name = "推荐")
        )
    }

    override fun getTrendPagingData(tab: TrendTab, params: TrendParams): Flow<PagingData<TrendItem>> {
        // This method is now deprecated in favor of fetchTrendData + RemoteMediator
        // But we keep it empty or throw exception if called directly,
        // or we can implement a simple Pager if we want to support non-cached mode.
        // For now, since we are moving to Repository-managed PagingData, this can be removed or left as stub.
        // However, the interface still requires it.
        // Ideally, we should refactor the interface to remove this method if all sources support fetchTrendData.
        // But for gradual migration, let's return empty flow here as Repository will use fetchTrendData.
        return kotlinx.coroutines.flow.emptyFlow()
    }

    override suspend fun fetchTrendData(
        tab: TrendTab,
        params: TrendParams,
        page: Int
    ): Result<List<TrendItem>> {
        return try {
            val topics = when (tab.id) {
                "tieba_hot" -> fetchHotThread(page)
                "tieba_topic" -> fetchTopicList(page)
                "tieba_concern" -> fetchConcern(page)
                "tieba_recommend" -> fetchRecommend(page)
                else -> emptyList()
            }
            Result.success(topics.map { topic ->
                TrendItem(
                    topicId = topic.id,
                    sourceId = id,
                    title = topic.title ?: "",
                    contentPreview = topic.content ?: "",
                    rank = null,
                    hotness = topic.agreeCount.toString(),
                    channel = topic.channelName,
                    author = topic.author.name,
                    url = "", // TODO
                    isNew = false,
                    payload = emptyMap()
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchHotThread(page: Int): List<Topic> {
        if (page > 1) return emptyList()
        val request = HotThreadListRequest(
            HotThreadListRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V11
                )
            )
        )
        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V11,
            parameterProvider = tiebaParameterProvider
        )
        val response = officialProtobufTiebaApiV11.hotThreadListFlow(body)
        if (response.error?.error_code != 0 && response.error?.error_code != null) {
            throw Exception("Tieba Error: ${response.error?.error_msg}")
        }
        return TiebaMapper.mapHotThreadListResponseToTopics(response)
    }

    private suspend fun fetchTopicList(page: Int): List<Topic> {
        if (page > 1) return emptyList()
        val request = TopicListRequest(
            TopicListRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V11
                ),
            )
        )
        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V11,
            parameterProvider = tiebaParameterProvider
        )
        val response = officialProtobufTiebaApiV11.topicListFlow(body)
        if (response.error?.error_code != 0 && response.error?.error_code != null) {
            throw Exception("Tieba Error: ${response.error?.error_msg}")
        }
        return TiebaMapper.mapTopicListResponseToTopics(response)
    }

    private suspend fun fetchConcern(page: Int): List<Topic> {
        val request = UserLikeRequest(
            UserLikeRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V11
                ),
                pageTag = if (page == 1) "" else page.toString(),
                loadType = if (page == 1) 1 else 2
            )
        )
        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V11,
            parameterProvider = tiebaParameterProvider
        )
        val response = officialProtobufTiebaApiV11.userLikeFlow(body)
        if (response.error?.error_code != 0 && response.error?.error_code != null) {
            throw Exception("Tieba Error: ${response.error?.error_msg}")
        }
        return TiebaMapper.mapUserLikeResponseToTopics(response)
    }

    private suspend fun fetchRecommend(page: Int): List<Topic> {
        val request = PersonalizedRequest(
            PersonalizedRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V11
                ),
                load_type = if (page == 1) 1 else 2,
                pn = page
            )
        )
        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V11,
            parameterProvider = tiebaParameterProvider
        )
        val response = officialProtobufTiebaApiV11.personalizedFlow(body)
        if (response.error?.error_code != 0 && response.error?.error_code != null) {
            throw Exception("Tieba Error: ${response.error?.error_msg}")
        }
        return TiebaMapper.mapPersonalizedResponseToTopics(response)
    }
}

package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.remote.ClientVersion
import ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.remote.TiebaProtoBuilder
import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.source.TrendSource
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import app.cash.paging.map
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListRequest
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListRequestData
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequest
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequestData
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListRequest
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListRequestData
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequest
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                when (tab.id) {
                    "tieba_hot" -> TiebaHotThreadPagingSource(
                        officialProtobufTiebaApiV11,
                        tiebaParameterProvider
                    )

                    "tieba_topic" -> TiebaTopicListPagingSource(
                        officialProtobufTiebaApiV11,
                        tiebaParameterProvider
                    )

                    "tieba_concern" -> TiebaConcernPagingSource(
                        officialProtobufTiebaApiV11,
                        tiebaParameterProvider
                    )

                    "tieba_recommend" -> TiebaRecommendPagingSource(
                        officialProtobufTiebaApiV11,
                        tiebaParameterProvider
                    )

                    else -> object : PagingSource<Int, Topic>() {
                        override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null
                        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> =
                            LoadResult.Page(emptyList(), null, null)
                    }
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { topic ->
                TrendItem(
                    id = topic.id,
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
            }
        }
    }

    private class TiebaHotThreadPagingSource(
        private val api: OfficialProtobufTiebaApi,
        private val parameterProvider: TiebaParameterProvider
    ) : PagingSource<Int, Topic>() {
        override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
            val page = params.key ?: 1
            if (page > 1) {
                return LoadResult.Page(emptyList(), null, null)
            }
            return try {
                val request = HotThreadListRequest(
                    HotThreadListRequestData(
                        common = TiebaProtoBuilder.buildCommonRequest(
                            parameterProvider,
                            ClientVersion.TIEBA_V11
                        )
                    )
                )
                val body = TiebaProtoBuilder.buildProtobufFormBody(
                    data = request,
                    clientVersion = ClientVersion.TIEBA_V11,
                    parameterProvider = parameterProvider
                )
                val response = api.hotThreadListFlow(body).first()
                if (response.error?.error_code != 0 && response.error?.error_code != null) {
                    return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg}"))
                }
                val topics = TiebaMapper.mapHotThreadListResponseToTopics(response)
                LoadResult.Page(
                    data = topics,
                    prevKey = null,
                    nextKey = null
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    private class TiebaTopicListPagingSource(
        private val api: OfficialProtobufTiebaApi,
        private val parameterProvider: TiebaParameterProvider
    ) : PagingSource<Int, Topic>() {
        override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
            val page = params.key ?: 1
            if (page > 1) {
                return LoadResult.Page(emptyList(), null, null)
            }
            return try {
                val request = TopicListRequest(
                    TopicListRequestData(
                        common = TiebaProtoBuilder.buildCommonRequest(
                            parameterProvider,
                            ClientVersion.TIEBA_V11
                        ),
                    )
                )
                val body = TiebaProtoBuilder.buildProtobufFormBody(
                    data = request,
                    clientVersion = ClientVersion.TIEBA_V11,
                    parameterProvider = parameterProvider
                )
                val response = api.topicListFlow(body).first()
                if (response.error?.error_code != 0 && response.error?.error_code != null) {
                    return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg}"))
                }
                val topics = TiebaMapper.mapTopicListResponseToTopics(response)
                LoadResult.Page(
                    data = topics,
                    prevKey = null,
                    nextKey = null
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    private class TiebaConcernPagingSource(
        private val api: OfficialProtobufTiebaApi,
        private val parameterProvider: TiebaParameterProvider
    ) : PagingSource<Int, Topic>() {
        override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
            val page = params.key ?: 1
            return try {
                val request = UserLikeRequest(
                    UserLikeRequestData(
                        common = TiebaProtoBuilder.buildCommonRequest(
                            parameterProvider,
                            ClientVersion.TIEBA_V11
                        ),
                        pageTag = if (page == 1) "" else page.toString(),
                        loadType = if (page == 1) 1 else 2
                    )
                )
                val body = TiebaProtoBuilder.buildProtobufFormBody(
                    data = request,
                    clientVersion = ClientVersion.TIEBA_V11,
                    parameterProvider = parameterProvider
                )
                val response = api.userLikeFlow(body).first()
                if (response.error?.error_code != 0 && response.error?.error_code != null) {
                    return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg}"))
                }
                val topics = TiebaMapper.mapUserLikeResponseToTopics(response)
                val nextKey = if (topics.isNotEmpty()) page + 1 else null
                LoadResult.Page(
                    data = topics,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    private class TiebaRecommendPagingSource(
        private val api: OfficialProtobufTiebaApi,
        private val parameterProvider: TiebaParameterProvider
    ) : PagingSource<Int, Topic>() {
        override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
            val page = params.key ?: 1
            return try {
                val request = PersonalizedRequest(
                    PersonalizedRequestData(
                        common = TiebaProtoBuilder.buildCommonRequest(
                            parameterProvider,
                            ClientVersion.TIEBA_V11
                        ),
                        load_type = if (page == 1) 1 else 2,
                        pn = page
                    )
                )
                val body = TiebaProtoBuilder.buildProtobufFormBody(
                    data = request,
                    clientVersion = ClientVersion.TIEBA_V11,
                    parameterProvider = parameterProvider
                )
                val response = api.personalizedFlow(body).first()
                if (response.error?.error_code != 0 && response.error?.error_code != null) {
                    return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg}"))
                }
                val topics = TiebaMapper.mapPersonalizedResponseToTopics(response)
                val nextKey = page + 1
                LoadResult.Page(
                    data = topics,
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}

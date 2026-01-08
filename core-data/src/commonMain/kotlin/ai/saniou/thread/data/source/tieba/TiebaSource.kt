package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.source.tieba.remote.ClientVersion
import ai.saniou.thread.data.source.tieba.remote.MiniTiebaApi
import ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.remote.OfficialTiebaApi
import ai.saniou.thread.data.source.tieba.remote.TiebaProtoBuilder
import ai.saniou.thread.data.source.tieba.remote.WebTiebaApi
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceCapabilities
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageRequest
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageRequestData
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorRequest
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorRequestData
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageRequest
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageRequestData
import io.ktor.http.encodeURLQueryComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class TiebaSource(
    private val miniTiebaApi: MiniTiebaApi,
    private val officialTiebaApi: OfficialTiebaApi,
    private val officialProtobufTiebaApiV11: OfficialProtobufTiebaApi,
    private val officialProtobufTiebaApiV12: OfficialProtobufTiebaApi,
    private val webTiebaApi: WebTiebaApi,
    private val database: Database,
    private val accountRepository: AccountRepository,
    private val tiebaParameterProvider: TiebaParameterProvider,
) : Source {
    override val id: String = TiebaMapper.SOURCE_ID
    override val name: String = TiebaMapper.SOURCE_NAME
    override val isInitialized: Flow<Boolean> = flowOf(true)
    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsTrend = false,
        supportsTrendHistory = false,
        supportsPagination = true
    )

    override val loginStrategy: LoginStrategy = LoginStrategy.WebView(
        url = "https://wappass.baidu.com/passport/?login",
        targetCookieKeys = listOf("BDUSS", "STOKEN")
    )

    override fun observeChannels(): Flow<List<Channel>> {
        return database.channelQueries.getChannelsBySource(id)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { it.toDomain(database.channelQueries) }
            }
    }

    override suspend fun fetchChannels(): Result<Unit> = runCatching {
        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = com.huanchengfly.tieba.post.api.models.protos.forumRecommend.ForumRecommendRequest(
                com.huanchengfly.tieba.post.api.models.protos.forumRecommend.ForumRecommendRequestData(
                    common = TiebaProtoBuilder.buildCommonRequest(
                        tiebaParameterProvider,
                        ClientVersion.TIEBA_V11
                    )
                )
            ),
            clientVersion = ClientVersion.TIEBA_V11,
            parameterProvider = tiebaParameterProvider
        )

        val response = officialProtobufTiebaApiV11.forumRecommendFlow(body).first()

        if (response.error?.error_code != 0) {
            throw Exception("Failed to fetch channels: ${response.error?.error_msg} (Code: ${response.error?.error_code})")
        }

        val channels = TiebaMapper.mapForumRecommendResponseToChannels(response)

        database.transaction {
            channels.forEach { channel ->
                database.channelQueries.insertChannel(
                    TiebaMapper.mapChannelToEntity(channel)
                )
            }
        }
    }

    override fun getFeedFlow(feedType: FeedType): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                when (feedType) {
                    FeedType.RECOMMEND -> TiebaRecommendPagingSource(
                        api = officialProtobufTiebaApiV11,
                        parameterProvider = tiebaParameterProvider
                    )

                    FeedType.CONCERN -> TiebaConcernPagingSource(
                        api = officialProtobufTiebaApiV11,
                        parameterProvider = tiebaParameterProvider
                    )

                    else -> object : PagingSource<Int, Topic>() {
                        override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null
                        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> =
                            LoadResult.Page(emptyList(), null, null)
                    }
                }
            }
        ).flow
    }

    override suspend fun getChannelTopics(
        channelId: String,
        page: Int,
        isTimeline: Boolean,
    ): Result<List<Topic>> = runCatching {
        fetchForumTopics(channelId, page)
    }

    private suspend fun fetchForumTopics(channelId: String, page: Int): List<Topic> {
        val channel = database.channelQueries.getChannel(TiebaMapper.SOURCE_ID, channelId)
            .executeAsOneOrNull()

        val forumName = channel?.name ?: channelId

        val request = FrsPageRequest(
            FrsPageRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V12
                ),
                kw = forumName,
                pn = page,
                rn = 30,
                sort_type = 0,
                scr_dip = 3.0,
                scr_h = 1920,
                scr_w = 1080,
                load_type = 0,
                q_type = 2,
                app_pos = TiebaProtoBuilder.buildAppPosInfo(),
                ad_param = TiebaProtoBuilder.buildAdParam()
            )
        )

        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V12,
            parameterProvider = tiebaParameterProvider
        )

        val response =
            officialProtobufTiebaApiV12.frsPageFlow(body, forumName.encodeURLQueryComponent())
                .first()

        if (response.error?.error_code != 0) {
            throw Exception("Tieba Error: ${response.error?.error_msg} (Code: ${response.error?.error_code})")
        }

        return TiebaMapper.mapFrsPageResponseToTopics(response, forumName, forumName)
    }


    override suspend fun getTopicComments(
        threadId: String,
        page: Int,
        isPoOnly: Boolean,
    ): Result<List<Comment>> = runCatching {
        val kz = threadId.toLongOrNull() ?: throw IllegalArgumentException("Invalid threadId: $threadId")

        val request = PbPageRequest(
            PbPageRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V12
                ),
                kz = kz,
                pn = page,
                rn = 30,
                lz = if (isPoOnly) 1 else 0,
                r = 0,
                scr_dip = 3.0,
                scr_h = 1920,
                scr_w = 1080,
                q_type = 2,
                app_pos = TiebaProtoBuilder.buildAppPosInfo(),
                ad_param = com.huanchengfly.tieba.post.api.models.protos.pbPage.AdParam()
            )
        )

        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V12,
            parameterProvider = tiebaParameterProvider
        )

        val response = officialProtobufTiebaApiV12.pbPageFlow(body).first()

        if (response.error?.error_code != 0) {
            throw Exception("Failed to fetch topic comments: ${response.error?.error_msg} (Code: ${response.error?.error_code})")
        }

        TiebaMapper.mapPbPageResponseToComments(response, threadId)
    }

    override suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic> = runCatching {
        val kz = threadId.toLongOrNull() ?: throw IllegalArgumentException("Invalid threadId: $threadId")

        val request = PbPageRequest(
            PbPageRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V12
                ),
                kz = kz,
                pn = page,
                rn = 30,
                lz = 0,
                r = 0,
                scr_dip = 3.0,
                scr_h = 1920,
                scr_w = 1080,
                q_type = 2,
                app_pos = TiebaProtoBuilder.buildAppPosInfo(),
                ad_param = com.huanchengfly.tieba.post.api.models.protos.pbPage.AdParam() // Using specific AdParam for PbPage if needed or default
            )
        )

        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V12,
            parameterProvider = tiebaParameterProvider
        )

        val response = officialProtobufTiebaApiV12.pbPageFlow(body).first()

        if (response.error?.error_code != 0) {
            throw Exception("Failed to fetch topic detail: ${response.error?.error_msg} (Code: ${response.error?.error_code})")
        }
        TiebaMapper.mapPbPageResponseToTopic(response, threadId)
            ?: throw Exception("Failed to map topic")
    }

    override fun getChannel(channelId: String): Flow<Channel?> {
        return database.channelQueries.getChannel(id, channelId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain(database.channelQueries) }
    }

    suspend fun getSubComments(
        topicId: String,
        postId: String,
        page: Int
    ): Result<List<Comment>> = runCatching {
        val kz = topicId.toLongOrNull() ?: throw IllegalArgumentException("Invalid topicId: $topicId")
        val pid = postId.toLongOrNull() ?: throw IllegalArgumentException("Invalid postId: $postId")

        val request = PbFloorRequest(
            PbFloorRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V12
                ),
                kz = kz,
                pid = pid,
                pn = page,
                scr_dip = 3.0,
                scr_h = 1920,
                scr_w = 1080
            )
        )
        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V12,
            parameterProvider = tiebaParameterProvider
        )
        val response = officialProtobufTiebaApiV12.pbFloorFlow(body).first()
        if (response.error?.error_code != 0) {
            throw Exception("Failed to fetch sub-comments: ${response.error?.error_msg} (Code: ${response.error?.error_code})")
        }
        TiebaMapper.mapPbFloorResponseToComments(response, topicId)
    }
}


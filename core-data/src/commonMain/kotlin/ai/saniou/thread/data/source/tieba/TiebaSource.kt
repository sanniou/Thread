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
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageRequest
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageRequestData
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageRequest
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageRequestData
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequest
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedRequestData
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequest
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeRequestData
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
    override val name: String = "百度贴吧"
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

        // We need to implement mapForumRecommendResponseToChannels in TiebaMapper first.
        // Assuming it's similar to mapForumRecommendToChannels but takes ForumRecommendResponse.
        // For now, let's add a placeholder or update Mapper.

        // Wait, I should update Mapper first. But I am editing Source now.
        // Let's assume Mapper has it or I will add it.
        // The previous JSON mapper: mapForumRecommendToChannels(response: ForumRecommend)
        // I need: mapForumRecommendResponseToChannels(response: ForumRecommendResponse)

        val channels = TiebaMapper.mapForumRecommendResponseToChannels(response)

        database.transaction {
            channels.forEach { channel ->
                database.channelQueries.insertChannel(
                    TiebaMapper.mapChannelToEntity(channel)
                )
            }
        }
    }

    override suspend fun getChannelTopics(
        channelId: String,
        page: Int,
        isTimeline: Boolean,
    ): Result<List<Topic>> = runCatching {
        when (channelId) {
            FeedType.RECOMMEND.name.lowercase() -> fetchRecommendTopics(page)
            FeedType.CONCERN.name.lowercase() -> fetchConcernTopics(page)
            else -> fetchForumTopics(channelId, page)
        }
    }

    private suspend fun fetchRecommendTopics(page: Int): List<Topic> {
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

        val response = officialProtobufTiebaApiV11.personalizedFlow(body).first()

        if (response.error?.error_code != 0) {
            throw Exception("Tieba Error: ${response.error?.error_msg} (Code: ${response.error?.error_code})")
        }

        return TiebaMapper.mapPersonalizedResponseToTopics(response)
    }

    private suspend fun fetchConcernTopics(page: Int): List<Topic> {
        val request = UserLikeRequest(
            UserLikeRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V11
                ),
                pageTag = if (page == 1) "" else page.toString(), // TODO: Check if pageTag supports page number
                loadType = if (page == 1) 1 else 2
            )
        )

        val body = TiebaProtoBuilder.buildProtobufFormBody(
            data = request,
            clientVersion = ClientVersion.TIEBA_V11,
            parameterProvider = tiebaParameterProvider
        )

        val response = officialProtobufTiebaApiV11.userLikeFlow(body).first()

        if (response.error?.error_code != 0) {
            throw Exception("Tieba Error: ${response.error?.error_msg} (Code: ${response.error?.error_code})")
        }

        return TiebaMapper.mapUserLikeResponseToTopics(response)
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
        val request = PbPageRequest(
            PbPageRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V12
                ),
                kz = threadId.toLongOrNull() ?: 0L,
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
        val request = PbPageRequest(
            PbPageRequestData(
                common = TiebaProtoBuilder.buildCommonRequest(
                    tiebaParameterProvider,
                    ClientVersion.TIEBA_V12
                ),
                kz = threadId.toLongOrNull() ?: 0L,
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
}

private class TiebaTopicPagingSource(
    private val api: OfficialProtobufTiebaApi,
    private val database: Database,
    private val channelId: String,
    private val parameterProvider: TiebaParameterProvider,
) : PagingSource<Int, Topic>() {

    override fun getRefreshKey(state: PagingState<Int, Topic>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
        val page = params.key ?: 1
        return try {
            val channel = database.channelQueries.getChannel(TiebaMapper.SOURCE_ID, channelId)
                .executeAsOneOrNull()

            val forumName = channel?.name ?: channelId

            val request = FrsPageRequest(
                FrsPageRequestData(
                    common = TiebaProtoBuilder.buildCommonRequest(
                        parameterProvider,
                        ClientVersion.TIEBA_V12
                    ),
                    kw = forumName,
                    pn = page,
                    rn = 30, // Request count
                    sort_type = 0, // 0 = Reply time?
                    scr_dip = 3.0,
                    scr_h = 1920,
                    scr_w = 1080,
                    load_type = 0, // 0 = default?
                    q_type = 2,
                    app_pos = TiebaProtoBuilder.buildAppPosInfo(),
                    ad_param = TiebaProtoBuilder.buildAdParam()
                )
            )

            val body = TiebaProtoBuilder.buildProtobufFormBody(
                data = request,
                clientVersion = ClientVersion.TIEBA_V12,
                parameterProvider = parameterProvider
            )

            val response = api.frsPageFlow(body, forumName.encodeURLQueryComponent())
                .map { it } // No transformation needed, it returns FrsPageResponse now
                // But Flow needs to be collected. Ktorfit returns Flow<Response>.
                // Wait, api.frsPageFlow returns Flow<FrsPageResponse>.
                // We need to get the first item.
                // However, PagingSource.load is suspend.
                // We can use first() on the Flow.
                .first()

            if (response.error?.error_code != 0) {
                return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg} (Code: ${response.error?.error_code})"))
            }

            val topics = TiebaMapper.mapFrsPageResponseToTopics(
                response,
                forumName,
                forumName
            ) // Assuming forumName/Id is correct

            val nextKey = if (response.data_?.page?.has_more == 1) page + 1 else null

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

private class TiebaCommentPagingSource(
    private val api: OfficialProtobufTiebaApi,
    private val threadId: String,
    private val isPoOnly: Boolean,
    private val parameterProvider: TiebaParameterProvider,
) : PagingSource<Int, Comment>() {
    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val page = params.key ?: 1
        return try {
            val request = PbPageRequest(
                PbPageRequestData(
                    common = TiebaProtoBuilder.buildCommonRequest(
                        parameterProvider,
                        ClientVersion.TIEBA_V12
                    ),
                    kz = threadId.toLongOrNull() ?: 0L,
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
                parameterProvider = parameterProvider
            )

            val response = api.pbPageFlow(body).first()

            if (response.error?.error_code != 0) {
                return LoadResult.Error(Exception("Tieba Error: ${response.error?.error_msg} (Code: ${response.error?.error_code})"))
            }

            val comments = TiebaMapper.mapPbPageResponseToComments(response, threadId)

            val nextKey = if (response.data_?.page?.has_more == 1) page + 1 else null

            LoadResult.Page(
                data = comments,
                prevKey = if (page == 1) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

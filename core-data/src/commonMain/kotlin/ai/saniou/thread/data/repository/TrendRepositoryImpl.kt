package ai.saniou.thread.data.repository

import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.GenericRemoteMediator
import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.data.source.tieba.TiebaMapper
import ai.saniou.thread.data.source.tieba.TiebaParameterProvider
import ai.saniou.thread.data.source.tieba.remote.ClientVersion
import ai.saniou.thread.data.source.tieba.remote.OfficialProtobufTiebaApi
import ai.saniou.thread.data.source.tieba.remote.TiebaProtoBuilder
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.Trend
import ai.saniou.thread.domain.model.forum.TrendResult
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.repository.TrendRepository
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TrendRepositoryImpl(
    private val nmbSource: NmbSource,
    private val sourceRepository: SourceRepository,
    private val officialProtobufTiebaApiV11: OfficialProtobufTiebaApi,
    private val tiebaParameterProvider: TiebaParameterProvider,
    private val database: Database,
) : TrendRepository {

    override suspend fun getTrendItems(
        sourceId: String,
        forceRefresh: Boolean,
        dayOffset: Int,
    ): Result<TrendResult> {
        if (sourceId != "nmb") {
            val source = sourceRepository.getSource(sourceId)
                ?: return Result.failure(IllegalStateException("Source not found: $sourceId"))

            return source.getTrendList(forceRefresh, dayOffset)
        }

        val trendThreadId = 50248044L
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val targetDate = today.minus(dayOffset, DateTimeUnit.DAY)

        // 1. Try to get from local cache if not force refreshing
        if (!forceRefresh) {
            if (dayOffset == 0) {
                // For today, check if the latest reply is from today
                val localLatestReply = nmbSource.getLocalLatestReply(trendThreadId)
                if (localLatestReply != null) {
                    val replyDate = localLatestReply.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    if (replyDate == today) {
                        val parsedItems = parseTrendContent(localLatestReply.content)
                        return Result.success(TrendResult(localLatestReply.createdAt, parsedItems))
                    }
                }
            } else {
                // For historical days, check if we have a reply with the target date
                val localReply = nmbSource.getLocalReplyByDate(trendThreadId, targetDate)
                if (localReply != null) {
                    val parsedItems = parseTrendContent(localReply.content)
                    return Result.success(TrendResult(localReply.createdAt, parsedItems))
                }
            }
        }

        // 2. Fetch from network using new Date-Based Strategy
        return try {
            nmbSource.getTrendReplyByDate(targetDate).mapCatching { reply ->
                if (reply == null) {
                    throw IllegalStateException("未找到 $targetDate 的趋势数据")
                }

                val parsedItems = parseTrendContent(reply.content)
                TrendResult(reply.now.toTime(), parsedItems)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTrendContent(content: String): List<Trend> {
        val items = mutableListOf<Trend>()
        // Split content by separator line
        val segments = content.split("—————")

        for (segment in segments) {
            if (segment.isBlank()) continue

            // Regex to extract rank, trend num, forum, new tag
            // Example: 01. Trend 34 [综合版1] <br />
            val headerRegex = """(\d+)\.\s+(Trend\s+\d+)\s+\[(.*?)\](?:\s+(New))?""".toRegex()
            val headerMatch = headerRegex.find(segment) ?: continue

            val rank = headerMatch.groupValues[1]
            val trendNum = headerMatch.groupValues[2]
            val forum = headerMatch.groupValues[3]
            val isNew = headerMatch.groupValues[4].isNotEmpty()

            // Regex to extract thread ID: >>No.67520848
            // 修复：兼容 HTML 实体 >
            val threadIdRegex = """(?:>>|>>)No\.(\d+)""".toRegex()
            val threadIdMatch = threadIdRegex.find(segment)
            val threadId = threadIdMatch?.groupValues?.get(1)?.toLongOrNull() ?: continue

            // Extract content preview: everything after the thread ID line
            val contentStartIndex = threadIdMatch.range.last + 1
            var contentPreview = if (contentStartIndex < segment.length) {
                segment.substring(contentStartIndex).trim()
            } else {
                ""
            }

            // Keep HTML tags and entities for RichText rendering
            contentPreview = contentPreview.trim()

            items.add(Trend(rank, trendNum, forum, isNew, threadId.toString(), contentPreview))
        }

        return items
    }

    override fun getHotThreads(): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                object : PagingSource<Int, Topic>() {
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
                            val response = officialProtobufTiebaApiV11.hotThreadListFlow(body).first()
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
            }
        ).flow
    }

    override fun getTopicList(): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                object : PagingSource<Int, Topic>() {
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
                                        tiebaParameterProvider,
                                        ClientVersion.TIEBA_V11
                                    ),
//                                    sort_type = 1
                                )
                            )
                            val body = TiebaProtoBuilder.buildProtobufFormBody(
                                data = request,
                                clientVersion = ClientVersion.TIEBA_V11,
                                parameterProvider = tiebaParameterProvider
                            )
                            val response = officialProtobufTiebaApiV11.topicListFlow(body).first()
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
            }
        ).flow
    }

    override fun getConcernFeed(): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                object : PagingSource<Int, Topic>() {
                    override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
                        val page = params.key ?: 1
                        return try {
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
                            val response = officialProtobufTiebaApiV11.userLikeFlow(body).first()
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
            }
        ).flow
    }

    override fun getPersonalizedFeed(): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                object : PagingSource<Int, Topic>() {
                    override fun getRefreshKey(state: PagingState<Int, Topic>): Int? = null

                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Topic> {
                        val page = params.key ?: 1
                        return try {
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
        ).flow
    }
}

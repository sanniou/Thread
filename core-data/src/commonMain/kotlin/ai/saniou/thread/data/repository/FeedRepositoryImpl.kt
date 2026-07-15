package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.model.FeedType
import ai.saniou.thread.domain.model.feed.ArticleItem
import ai.saniou.thread.domain.model.feed.FeedRefreshReport
import ai.saniou.thread.domain.model.feed.PostItem
import ai.saniou.thread.domain.model.feed.SourceFeedFailure
import ai.saniou.thread.domain.model.feed.TimelineItem
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.FeedRepository
import ai.saniou.thread.domain.repository.ReaderRepository
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.refresh.RefreshCoordinator
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.supervisorScope

class FeedRepositoryImpl(
    private val sourceRepository: SourceRepository,
    private val readerRepository: ReaderRepository,
    private val refreshCoordinator: RefreshCoordinator,
) : FeedRepository {

    override fun getTimelinePaging(
        sourceIds: Set<String>?,
        includeReader: Boolean,
    ): Flow<PagingData<TimelineItem>> = Pager(
        config = PagingConfig(
            pageSize = TIMELINE_PAGE_SIZE,
            initialLoadSize = TIMELINE_PAGE_SIZE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = {
            TimelinePagingSource(
                sourceRepository = sourceRepository,
                readerRepository = readerRepository,
                sourceIds = sourceIds,
                includeReader = includeReader,
            )
        },
    ).flow

    override suspend fun refreshTimeline(
        sourceIds: Set<String>?,
        includeReader: Boolean,
    ): FeedRefreshReport = supervisorScope {
        val selectedSources = sourceRepository.getAvailableSources()
            .filter { it.capabilities.supportsFeedAggregation }
            .filter { sourceIds == null || it.id in sourceIds }
        val sourceRefresh = async {
            selectedSources.map { source ->
                async {
                    source to refreshCoordinator.execute(
                        key = "forum:${source.id}:catalog",
                        label = "${source.name} 版块",
                        operation = source::fetchChannels,
                    )
                }
            }.awaitAll()
        }
        val readerRefresh = async {
            if (includeReader) readerRepository.refreshAllFeeds() else null
        }

        val sourceResults = sourceRefresh.await()
        val sourceFailures = sourceResults.mapNotNull { (source, result) ->
            result.exceptionOrNull()?.let { error ->
                SourceFeedFailure(
                    sourceId = source.id,
                    sourceName = source.name,
                    message = error.message ?: error::class.simpleName ?: "Unknown error",
                )
            }
        }
        val selectedSourceIds = selectedSources.mapTo(mutableSetOf()) { it.id }
        val failedIds = sourceFailures.mapTo(mutableSetOf()) { it.sourceId }
        val readerReport = readerRefresh.await()

        FeedRefreshReport(
            refreshedSourceIds = selectedSourceIds - failedIds,
            sourceFailures = sourceFailures,
            refreshedReaderSourceIds = readerReport?.refreshedSourceIds.orEmpty(),
            readerFailures = readerReport?.failures.orEmpty(),
        )
    }

    override fun getFeed(sourceId: String, feedType: FeedType): Flow<PagingData<Topic>> {
        val source = sourceRepository.getSource(sourceId) ?: return emptyFlow()
        return source.getFeedFlow(feedType)
    }

    private companion object {
        const val TIMELINE_PAGE_SIZE = 30
    }
}

private class TimelinePagingSource(
    private val sourceRepository: SourceRepository,
    private val readerRepository: ReaderRepository,
    private val sourceIds: Set<String>?,
    private val includeReader: Boolean,
) : PagingSource<Int, TimelineItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TimelineItem> = supervisorScope {
        val page = params.key ?: 1
        val forumDeferred = async { sourceRepository.getAggregatedFeed(page, sourceIds) }
        val readerDeferred = async {
            runCatching {
                if (includeReader) {
                    readerRepository.getRecentArticles(
                        limit = PAGE_SIZE.toLong(),
                        offset = ((page - 1) * PAGE_SIZE).toLong(),
                    )
                } else {
                    emptyList()
                }
            }
        }

        val forumResult = forumDeferred.await()
        val readerResult = readerDeferred.await()
        if (forumResult.isFailure && readerResult.isFailure) {
            val message = listOfNotNull(
                forumResult.exceptionOrNull()?.message,
                readerResult.exceptionOrNull()?.message,
            ).joinToString("; ")
            return@supervisorScope LoadResult.Error(IllegalStateException(message))
        }

        val forumPage = forumResult.getOrNull()
        val articles = readerResult.getOrDefault(emptyList())
        val items = buildList<TimelineItem> {
            addAll(forumPage?.topics.orEmpty().map(::PostItem))
            addAll(articles.map { entry ->
                ArticleItem(
                    article = entry.article,
                    sourceName = entry.sourceName,
                    sourceIconUrl = entry.sourceIconUrl,
                )
            })
        }
            .distinctBy { it.uniqueId }
            .sortedByDescending { it.displayTime }

        val hasMore = forumPage?.hasMore == true || articles.size >= PAGE_SIZE
        LoadResult.Page(
            data = items,
            prevKey = if (page > 1) page - 1 else null,
            nextKey = if (hasMore && items.isNotEmpty()) page + 1 else null,
        )
    }

    override fun getRefreshKey(state: PagingState<Int, TimelineItem>): Int? {
        val anchor = state.anchorPosition ?: return null
        return state.closestPageToPosition(anchor)?.let { page ->
            page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}

package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.domain.model.TrendItem
import ai.saniou.thread.domain.model.TrendParams
import ai.saniou.thread.domain.model.TrendTab
import ai.saniou.thread.domain.source.TrendSource
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class NmbTrendSource(
    private val nmbSource: NmbSource
) : TrendSource {
    override val id: String = "nmb"
    override val name: String = "A岛"

    override fun getTrendTabs(): List<TrendTab> {
        return listOf(
            TrendTab(id = "nmb_trend", name = "趋势", supportsHistory = true)
        )
    }

    override fun getTrendPagingData(tab: TrendTab, params: TrendParams): Flow<PagingData<TrendItem>> {
        return emptyFlow()
    }

    override suspend fun fetchTrendData(
        tab: TrendTab,
        params: TrendParams,
        page: Int
    ): Result<List<TrendItem>> {
        if (page > 1) return Result.success(emptyList())

        return try {
            val trendThreadId = 50248044L
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val targetDate = today.minus(params.dayOffset, DateTimeUnit.DAY)

            // 1. Try to get from local cache if not force refreshing
            if (params.refreshId == 0L) {
                if (params.dayOffset == 0) {
                    // For today, check if the latest reply is from today
                    val localLatestReply = nmbSource.getLocalLatestReply(trendThreadId)
                    if (localLatestReply != null) {
                        val replyDate = localLatestReply.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                        if (replyDate == today) {
                            return Result.success(parseTrendContent(localLatestReply.content))
                        }
                    }
                } else {
                    // For historical days, check if we have a reply with the target date
                    val localReply = nmbSource.getLocalReplyByDate(trendThreadId, targetDate)
                    if (localReply != null) {
                        return Result.success(parseTrendContent(localReply.content))
                    }
                }
            }

            // 2. Fetch from network using new Date-Based Strategy
            val replyResult = nmbSource.getTrendReplyByDate(targetDate)
            val reply = replyResult.getOrNull()
                ?: throw IllegalStateException("未找到 $targetDate 的趋势数据")

            Result.success(parseTrendContent(reply.content))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTrendContent(content: String): List<TrendItem> {
        val items = mutableListOf<TrendItem>()
        // Split content by separator line
        val segments = content.split("—————")

        for (segment in segments) {
            if (segment.isBlank()) continue

            // Regex to extract rank, trend num, forum, new tag
            // Example: 01. Trend 34 [综合版1] <br />
            val headerRegex = """(\d+)\.\s+(Trend\s+\d+)\s+\[(.*?)\](?:\s+(New))?""".toRegex()
            val headerMatch = headerRegex.find(segment) ?: continue

            val rank = headerMatch.groupValues[1].toIntOrNull()
            val trendNum = headerMatch.groupValues[2]
            val forum = headerMatch.groupValues[3]
            val isNew = headerMatch.groupValues[4].isNotEmpty()

            // Regex to extract thread ID: >>No.67520848
            // 修复：兼容 HTML 实体 >
            val threadIdRegex = """(?:>>|>>)No\.(\d+)""".toRegex()
            val threadIdMatch = threadIdRegex.find(segment)
            val threadId = threadIdMatch?.groupValues?.get(1) ?: continue

            // Extract content preview: everything after the thread ID line
            val contentStartIndex = threadIdMatch.range.last + 1
            var contentPreview = if (contentStartIndex < segment.length) {
                segment.substring(contentStartIndex).trim()
            } else {
                ""
            }

            // Keep HTML tags and entities for RichText rendering
            contentPreview = contentPreview.trim()

            items.add(
                TrendItem(
                    id = threadId,
                    sourceId = "nmb",
                    title = "No.$threadId", // NMB usually doesn't have titles, use ID
                    contentPreview = contentPreview,
                    rank = rank,
                    hotness = trendNum,
                    channel = forum,
                    author = null,
                    url = "", // TODO: Construct URL if needed
                    isNew = isNew
                )
            )
        }

        return items
    }
}
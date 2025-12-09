package ai.saniou.nmb.domain

import ai.saniou.nmb.workflow.trend.TrendContract.TrendItem
import ai.saniou.thread.data.source.nmb.NmbSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TrendUseCase(
    private val nmbRepository: NmbSource,
) {

    @OptIn(ExperimentalTime::class)
    suspend fun getTrendItems(forceRefresh: Boolean): Result<Pair<String, List<TrendItem>>> {
        val trendThreadId = 50248044L

        // 1. Try to get from local cache if not force refreshing
        if (!forceRefresh) {
            val localLatestReply = nmbRepository.getLocalLatestReply(trendThreadId)
            if (localLatestReply != null) {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                // Check if cached data is from today
                if (localLatestReply.now.contains(today.toString())) {
                    val parsedItems = parseTrendContent(localLatestReply.content)
                    return Result.success(localLatestReply.now to parsedItems)
                }
            }
        }

        // 2. Fetch from network
        return try {
            nmbRepository.getTrendThread(page = 1).mapCatching { firstPageThread ->
                val replyCount = firstPageThread.replyCount
                val lastPage = (replyCount / 19) + if (replyCount % 19 > 0) 1 else 0

                val lastPageThread =
                    nmbRepository.getTrendThread(page = lastPage.toInt()).getOrThrow()
                val latestReply = lastPageThread.replies.lastOrNull()
                    ?: throw IllegalStateException("无趋势数据")

                val parsedItems = parseTrendContent(latestReply.content)
                latestReply.now to parsedItems
            }
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

            val rank = headerMatch.groupValues[1]
            val trendNum = headerMatch.groupValues[2]
            val forum = headerMatch.groupValues[3]
            val isNew = headerMatch.groupValues[4].isNotEmpty()

            // Regex to extract thread ID: >>No.67520848
            // 修复：兼容 HTML 实体 &gt;
            val threadIdRegex = """(?:>>|&gt;&gt;)No\.(\d+)""".toRegex()
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

            items.add(TrendItem(rank, trendNum, forum, isNew, threadId, contentPreview))
        }

        return items
    }
}

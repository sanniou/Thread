package ai.saniou.thread.data.repository

import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.thread.domain.model.forum.Trend
import ai.saniou.thread.domain.repository.TrendRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TrendRepositoryImpl(
    private val nmbSource: NmbSource,
) : TrendRepository {

    override suspend fun getTrendItems(
        forceRefresh: Boolean,
        dayOffset: Int
    ): Result<Pair<String, List<Trend>>> {
        val trendThreadId = 50248044L

        // 1. Try to get from local cache if not force refreshing and requesting today's trend
        if (!forceRefresh && dayOffset == 0) {
            val localLatestReply = nmbSource.getLocalLatestReply(trendThreadId)
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
            nmbSource.getTrendThread(page = 1).mapCatching { firstPageThread ->
                val replyCount = firstPageThread.replyCount

                // Calculate target index based on dayOffset
                // dayOffset = 0 -> last reply
                // dayOffset = 1 -> second to last reply
                val targetIndex = replyCount - 1 - dayOffset

                if (targetIndex < 0) {
                    throw IllegalStateException("没有更多历史数据了")
                }

                val targetPage = (targetIndex / 19) + 1
                val indexInPage = (targetIndex % 19).toInt()

                val targetPageThread = if (targetPage == 1L) {
                    firstPageThread
                } else {
                    nmbSource.getTrendThread(page = targetPage.toInt()).getOrThrow()
                }

                val replies = targetPageThread.replies
                if (replies.isEmpty()) {
                    throw IllegalStateException("无趋势数据")
                }

                // Handle potential index out of bounds if replyCount was slightly off or replies are missing
                val reply = if (indexInPage < replies.size) {
                    replies[indexInPage]
                } else {
                    replies.last()
                }

                val parsedItems = parseTrendContent(reply.content)
                reply.now to parsedItems
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

            items.add(Trend(rank, trendNum, forum, isNew, threadId, contentPreview))
        }

        return items
    }
}

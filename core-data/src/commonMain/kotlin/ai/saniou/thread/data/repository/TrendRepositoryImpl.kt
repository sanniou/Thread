package ai.saniou.thread.data.repository

import ai.saniou.thread.data.source.nmb.NmbSource
import ai.saniou.corecommon.utils.DateParser
import ai.saniou.corecommon.utils.toTime
import ai.saniou.thread.domain.model.forum.Trend
import ai.saniou.thread.domain.model.forum.TrendResult
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.repository.TrendRepository
import ai.saniou.thread.domain.repository.getValue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TrendRepositoryImpl(
    private val nmbSource: NmbSource,
    private val sourceRepository: SourceRepository
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

            items.add(Trend(rank, trendNum, forum, isNew, threadId.toString(), contentPreview))
        }

        return items
    }
}

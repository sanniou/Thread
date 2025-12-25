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
                val localReply = nmbSource.getLocalReplyByDate(trendThreadId, targetDate.toString())
                if (localReply != null) {
                    val parsedItems = parseTrendContent(localReply.content)
                    return Result.success(TrendResult(localReply.createdAt, parsedItems))
                }
            }
        }

        // 2. Fetch from network

        // Optimization: Try to use cached page anchor to avoid fetching page 1
        val anchor = nmbSource.getTrendAnchor()
        if (anchor != null) {
            val (cachedPage, cachedStart, cachedEnd) = anchor
            var predictedPage: Int? = null
            try {
                // now string format: 2023-12-24(日)00:15:22
                val startDate = DateParser.parseDateFromNowString(cachedStart) ?: throw IllegalArgumentException("Invalid start date")
                val endDate = DateParser.parseDateFromNowString(cachedEnd) ?: throw IllegalArgumentException("Invalid end date")

                if (targetDate in startDate..endDate) {
                    predictedPage = cachedPage
                } else if (targetDate > endDate) {
                    val daysNeeded = endDate.daysUntil(targetDate)
                    val countOnPage = startDate.daysUntil(endDate) + 1
                    val remainingSlots = 19 - countOnPage

                    if (daysNeeded <= remainingSlots) {
                        predictedPage = cachedPage
                    } else {
                        val remainingDays = daysNeeded - remainingSlots
                        val additionalPages = (remainingDays - 1) / 19 + 1
                        predictedPage = cachedPage + additionalPages
                    }
                } else { // targetDate < startDate
                    val daysDiff = targetDate.daysUntil(startDate)
                    val pageOffset = (daysDiff - 1) / 19 + 1
                    predictedPage = cachedPage - pageOffset
                }
            } catch (e: Exception) {
                // Date parse error or calculation error, ignore optimization
            }

            if (predictedPage != null && predictedPage > 0) {
                try {
                    val thread = nmbSource.getTrendThread(page = predictedPage).getOrThrow()
                    val replies = thread.replies
                    val reply = replies.find { it.now.contains(targetDate.toString()) }

                    if (reply != null) {
                        if (replies.isNotEmpty()) {
                            nmbSource.setTrendAnchor(
                                predictedPage,
                                replies.first().now,
                                replies.last().now
                            )
                        }
                        val parsedItems = parseTrendContent(reply.content)
                        // Trend DTO Reply still has 'now' string, but if we get from DB we need to handle Instant
                        // Here 'reply' is from Source (DTO), so 'now' is String.
                        return Result.success(TrendResult(reply.now.toTime(), parsedItems))
                    }
                } catch (e: Exception) {
                    // Fallback to standard logic if prediction fails
                }
            }
        }

        // Standard logic: Fetch page 1 to get total count
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

                // Update anchor for next time
                if (replies.isNotEmpty()) {
                    nmbSource.setTrendAnchor(
                        targetPage.toInt(),
                        replies.first().now,
                        replies.last().now
                    )
                }

                // Calculate corrected dayOffset if the fetched date doesn't match targetDate
                var correctedDayOffset: Int? = null
                val fetchedDate = DateParser.parseDateFromNowString(reply.now)
                if (fetchedDate != null && fetchedDate != targetDate) {
                    correctedDayOffset = today.daysUntil(fetchedDate) * -1
                }

                val parsedItems = parseTrendContent(reply.content)
                TrendResult(reply.now.toTime(), parsedItems, correctedDayOffset)
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
}
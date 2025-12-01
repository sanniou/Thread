package ai.saniou.nmb.workflow.trend

import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.data.storage.CommonStorage
import ai.saniou.nmb.workflow.trend.TrendContract.Effect
import ai.saniou.nmb.workflow.trend.TrendContract.Event
import ai.saniou.nmb.workflow.trend.TrendContract.State
import ai.saniou.nmb.workflow.trend.TrendContract.TrendItem
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val KEY_TREND_ITEMS = "trend_items"
private const val KEY_TREND_DATE = "trend_date"

class TrendViewModel(
    private val nmbRepository: NmbRepository,
    private val commonStorage: CommonStorage
) : ScreenModel {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private val _effect = Channel<Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        loadTrend()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.Refresh -> loadTrend(forceRefresh = true)
            is Event.OnTrendItemClick -> {
                screenModelScope.launch {
                    _effect.send(Effect.NavigateToThread(event.threadId))
                }
            }
            Event.OnInfoClick -> {
                 screenModelScope.launch {
                    _effect.send(Effect.ShowInfoDialog("https://www.nmbxd1.com/t/50248044"))
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadTrend(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            if (!forceRefresh) {
                val cachedItems = commonStorage.getValue<List<TrendItem>>(KEY_TREND_ITEMS)
                val cachedDate = commonStorage.getValue<String>(KEY_TREND_DATE)

                if (!cachedItems.isNullOrEmpty() && !cachedDate.isNullOrBlank()) {
                    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                    // cachedDate format: "2025-11-30(日)21:42:07"
                    // Check if it starts with today's date YYYY-MM-DD
                    if (cachedDate.contains(today.toString())) {
                         _state.update {
                            it.copy(
                                isLoading = false,
                                trendDate = cachedDate,
                                items = cachedItems
                            )
                        }
                        return@launch
                    }
                }
            }

            _state.update { it.copy(isLoading = true, error = null) }

            // Step 1: Get first page to find total pages
            nmbRepository.getTrendThread(page = 1)
                .onSuccess { firstPageThread ->
                    val replyCount = firstPageThread.replyCount
                    val lastPage = (replyCount / 19) + if (replyCount % 19 > 0) 1 else 0

                    // Step 2: Get the last page data
                    nmbRepository.getTrendThread(page = lastPage.toInt())
                        .onSuccess { lastPageThread ->
                             // Get the latest reply
                            val latestReply = lastPageThread.replies.lastOrNull()

                            if (latestReply != null) {
                                val parsedItems = parseTrendContent(latestReply.content)

                                // Cache result
                                commonStorage.saveValue(KEY_TREND_ITEMS, parsedItems)
                                commonStorage.saveValue(KEY_TREND_DATE, latestReply.now)

                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        trendDate = latestReply.now,
                                        items = parsedItems,
                                        rawThread = lastPageThread
                                    )
                                }
                            } else {
                                _state.update { it.copy(isLoading = false, error = "无趋势数据") }
                            }
                        }
                        .onFailure { e ->
                            _state.update { it.copy(isLoading = false, error = e.message) }
                        }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    /**
     * @2025-11-30<br />
     * 01. Trend 34 [综合版1] <br />
     * <font color="#789922">&gt;&gt;No.67520848</font><br />
     * <br />
     * 刷抖音给我看笑了，一个视频说新的治安管理处罚法136条规定要把吸.毒.记录封存，底下一堆说大手发力的...<br />
     * —————<br />
     * <br />
     * 23. Trend 11 [故事] New<br />
     * <font color="#789922">&gt;&gt;No.67533955</font><br />
     * <br />
     * ps：突发灵感，大纲已全，应该会是个很短的小故事，个人感觉应该不错，那么废话唠完咱们走起<br />
     * —————<br />
     * <br />
     * 24. Trend 10 [综合版1] <br />
     * <font color="#789922">&gt;&gt;No.67530603</font><br />
     * <br />
     * 开个串讲讲解放前的妓院妓女(`ε´ )<br />
     * —————
     */
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

            // Remove <br /> tags and font tags for cleaner preview text
            contentPreview = contentPreview
                .replace(Regex("</?font[^>]*>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&")
                .trim()

            items.add(TrendItem(rank, trendNum, forum, isNew, threadId, contentPreview))
        }

        return items
    }
}

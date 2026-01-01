package ai.saniou.thread.feature.history

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.thread.domain.model.history.HistoryArticle
import ai.saniou.thread.domain.model.history.HistoryItem
import ai.saniou.thread.domain.model.history.HistoryPost
import ai.saniou.thread.domain.usecase.history.GetHistoryUseCase
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import app.cash.paging.insertSeparators
import app.cash.paging.map
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface HistoryUiItem {
    data class DateSeparator(val date: String) : HistoryUiItem
    data class Item(val history: HistoryItem) : HistoryUiItem
}

class HistoryViewModel(
    private val getHistoryUseCase: GetHistoryUseCase,
) : ScreenModel {

    private val _typeFilter = MutableStateFlow<String?>(null) // null = All, "post", "article"
    val typeFilter = _typeFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: Flow<PagingData<HistoryUiItem>> = _typeFilter
        .flatMapLatest { type ->
            getHistoryUseCase(type)
        }
        .map { pagingData ->
            pagingData.map { HistoryUiItem.Item(it) }
        }
        .map { pagingData ->
            pagingData.insertSeparators { before: HistoryUiItem.Item?, after: HistoryUiItem.Item? ->
                if (after == null) {
                    // List end
                    return@insertSeparators null
                }

                if (before == null) {
                    // List start
                    return@insertSeparators HistoryUiItem.DateSeparator(getDateLabel(after.history.accessTime))
                }

                val beforeDate = getDateLabel(before.history.accessTime)
                val afterDate = getDateLabel(after.history.accessTime)

                if (beforeDate != afterDate) {
                    HistoryUiItem.DateSeparator(afterDate)
                } else {
                    null
                }
            }
        }
        .cachedIn(screenModelScope)

    fun onFilterChanged(type: String?) {
        _typeFilter.update { type }
    }

    private fun getDateLabel(instant: kotlin.time.Instant): String {
        // 使用 DateTimeFormatter 的逻辑，但这里我们只关心日期分组，不需要精确到分钟
        // 简单策略：
        // 今天 -> "今天"
        // 昨天 -> "昨天"
        // 其他 -> "yyyy-MM-dd"
        // 注意：DateTimeFormatter.kt 中的 toRelativeTimeString 比较适合单条显示，这里我们需要分组标题

        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val date = instant.toLocalDateTime(timeZone)

        return if (date.year == now.year && date.dayOfYear == now.dayOfYear) {
            "今天"
        } else if (date.year == now.year && date.dayOfYear == now.dayOfYear - 1) {
            "昨天"
        } else {
             "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
        }
    }
}

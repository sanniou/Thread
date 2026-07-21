package ai.saniou.thread.feature.history

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.thread.domain.model.history.HistoryArticle
import ai.saniou.thread.domain.model.history.HistoryItem
import ai.saniou.thread.domain.model.history.HistoryPost
import ai.saniou.thread.domain.usecase.history.GetHistoryUseCase
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.getString
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_17e83cc25e
import thread.composeapp.generated.resources.s_59c4fcb09e

sealed interface HistoryUiItem {
    data class DateSeparator(val date: String) : HistoryUiItem
    data class Item(val history: HistoryItem) : HistoryUiItem
}

class HistoryViewModel(
    private val getHistoryUseCase: GetHistoryUseCase,
) : ScreenModel {

    private val _typeFilter = MutableStateFlow<String?>(null) // null = All, "post", "article"
    val typeFilter = _typeFilter.asStateFlow()

    // Preload because insertSeparators transform is not a suspend context.
    private val dateLabels = MutableStateFlow(DateLabels())

    init {
        screenModelScope.launch {
            dateLabels.value = DateLabels(
                today = getString(Res.string.s_17e83cc25e),
                yesterday = getString(Res.string.s_59c4fcb09e),
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyItems: Flow<PagingData<HistoryUiItem>> = _typeFilter
        .flatMapLatest { type ->
            getHistoryUseCase(type)
        }
        .map { pagingData ->
            pagingData.map { HistoryUiItem.Item(it) }
        }
        .map { pagingData ->
            val labels = dateLabels.value
            pagingData.insertSeparators { before: HistoryUiItem.Item?, after: HistoryUiItem.Item? ->
                if (after == null) {
                    return@insertSeparators null
                }

                if (before == null) {
                    return@insertSeparators HistoryUiItem.DateSeparator(getDateLabel(after.history.accessTime, labels))
                }

                val beforeDate = getDateLabel(before.history.accessTime, labels)
                val afterDate = getDateLabel(after.history.accessTime, labels)

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

    private fun getDateLabel(instant: kotlin.time.Instant, labels: DateLabels): String {
        val timeZone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(timeZone)
        val date = instant.toLocalDateTime(timeZone)

        return if (date.year == now.year && date.dayOfYear == now.dayOfYear) {
            labels.today
        } else if (date.year == now.year && date.dayOfYear == now.dayOfYear - 1) {
            labels.yesterday
        } else {
             val month = date.month.ordinal + 1
             "${date.year}-${month.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
        }
    }

    private data class DateLabels(
        val today: String = "Today",
        val yesterday: String = "Yesterday",
    )
}

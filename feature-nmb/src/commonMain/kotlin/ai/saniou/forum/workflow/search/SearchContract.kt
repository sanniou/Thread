package ai.saniou.forum.workflow.search

import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadWithInformation
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface SearchContract {
    data class State(
        val query: String = "",
        val searchType: SearchType = SearchType.THREAD,
        val threadPagingData: Flow<PagingData<ThreadWithInformation>> = emptyFlow(),
        val replyPagingData: Flow<PagingData<ThreadReply>> = emptyFlow(),
    )

    sealed class Event {
        data class QueryChanged(val query: String) : Event()
        data class TypeChanged(val type: SearchType) : Event()
        data object ClearQuery : Event()
    }

    enum class SearchType(val title: String) {
        THREAD("搜串"),
        REPLY("搜回复")
    }
}

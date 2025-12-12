package ai.saniou.reader.workflow.articledetail

import ai.saniou.thread.domain.model.Article

interface ArticleDetailContract {
    data class State(
        val article: Article? = null,
        val isLoading: Boolean = true,
        val error: Throwable? = null,
        val fontSizeScale: Float = 1.0f, // 1.0 is standard size
        val isMenuExpanded: Boolean = false
    )

    sealed interface Event {
        object OnToggleBookmark : Event
        object OnRetry : Event
        data class OnChangeFontSize(val scale: Float) : Event
        data class OnToggleMenu(val expanded: Boolean) : Event
    }
}
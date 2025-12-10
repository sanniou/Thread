package ai.saniou.reader.workflow.articledetail

import ai.saniou.thread.domain.model.Article

interface ArticleDetailContract {
    data class State(
        val article: Article? = null,
        val isLoading: Boolean = true,
        val error: Throwable? = null
    )
}
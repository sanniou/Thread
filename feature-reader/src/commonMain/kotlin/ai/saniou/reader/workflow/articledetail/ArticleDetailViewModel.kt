package ai.saniou.reader.workflow.articledetail

import ai.saniou.thread.domain.usecase.reader.GetArticleUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArticleDetailViewModel(
    private val articleId: String,
    private val getArticleUseCase: GetArticleUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(ArticleDetailContract.State())
    val state = _state.asStateFlow()

    init {
        loadArticle()
    }

    private fun loadArticle() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val article = getArticleUseCase(articleId)
                _state.update { it.copy(article = article, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e, isLoading = false) }
            }
        }
    }
}
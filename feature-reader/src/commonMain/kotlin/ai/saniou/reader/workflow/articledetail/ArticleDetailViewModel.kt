package ai.saniou.reader.workflow.articledetail

import ai.saniou.thread.domain.usecase.reader.GetArticleUseCase
import ai.saniou.thread.domain.usecase.reader.GetFeedSourceUseCase
import ai.saniou.thread.domain.usecase.reader.ToggleArticleBookmarkUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArticleDetailViewModel(
    private val articleId: String,
    private val getArticleUseCase: GetArticleUseCase,
    private val getFeedSourceUseCase: GetFeedSourceUseCase,
    private val toggleArticleBookmarkUseCase: ToggleArticleBookmarkUseCase
) : ScreenModel {

    private val _state = MutableStateFlow(ArticleDetailContract.State())
    val state = _state.asStateFlow()

    init {
        loadArticle()
    }

    fun onEvent(event: ArticleDetailContract.Event) {
        when (event) {
            ArticleDetailContract.Event.OnToggleBookmark -> toggleBookmark()
            ArticleDetailContract.Event.OnRetry -> loadArticle()
            is ArticleDetailContract.Event.OnChangeFontSize -> changeFontSize(event.scale)
            is ArticleDetailContract.Event.OnToggleMenu -> toggleMenu(event.expanded)
        }
    }

    private fun loadArticle() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val article = getArticleUseCase(articleId)
                
                // Load FeedSource Name
                val sourceName = article?.let { 
                    getFeedSourceUseCase(it.feedSourceId)?.name 
                }

                _state.update { 
                    it.copy(
                        article = article, 
                        feedSourceName = sourceName,
                        isLoading = false
                    ) 
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e, isLoading = false) }
            }
        }
    }

    private fun toggleBookmark() {
        val currentArticle = state.value.article ?: return
        screenModelScope.launch {
            try {
                // Optimistic update
                val newStatus = !currentArticle.isBookmarked
                _state.update { 
                    it.copy(article = currentArticle.copy(isBookmarked = newStatus)) 
                }
                
                toggleArticleBookmarkUseCase(currentArticle.id, !newStatus)
            } catch (e: Exception) {
                // Revert on error
                _state.update { 
                    it.copy(article = currentArticle, error = e) 
                }
            }
        }
    }

    private fun changeFontSize(scale: Float) {
        _state.update { it.copy(fontSizeScale = scale) }
    }

    private fun toggleMenu(expanded: Boolean) {
        _state.update { it.copy(isMenuExpanded = expanded) }
    }
}
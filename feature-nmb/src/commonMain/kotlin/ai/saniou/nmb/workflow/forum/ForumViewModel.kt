package ai.saniou.nmb.workflow.forum

import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.workflow.forum.ForumContract.Event
import ai.saniou.nmb.workflow.forum.ForumContract.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ForumViewModel(private val forumUseCase: ForumUseCase) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    private var forumId: Long? = null
    private var fgroupId: Long? = null

    fun onEvent(event: Event) {
        when (event) {
            is Event.LoadForum -> {
                forumId = event.fid
                fgroupId = event.fgroup
                loadForum()
            }
            Event.Refresh -> {
                loadForum()
            }
        }
    }

    private fun loadForum() {
        val fid = forumId ?: return
        val fgroup = fgroupId ?: return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val threads = forumUseCase(fid, fgroup).cachedIn(viewModelScope)
                val forumName = forumUseCase.getForumName(fid)
                _state.update {
                    it.copy(
                        isLoading = false,
                        threads = threads,
                        forumName = forumName
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "加载板块失败: ${e.message}"
                    )
                }
            }
        }
    }
}

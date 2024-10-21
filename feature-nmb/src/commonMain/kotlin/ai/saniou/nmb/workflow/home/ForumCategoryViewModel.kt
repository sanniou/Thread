package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.entity.ForumCategory
import ai.saniou.nmb.domain.ForumCategoryUserCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

class ForumCategoryViewModel(private val forumCategoryUserCase: ForumCategoryUserCase) :
    ViewModel() {

    private val _uiState = MutableStateFlow(
        GroupMemberUiState(
            forums = emptyList(),
            checkState = false,
            onForwardChange = { checked ->
                updateUiState {
                    it.copy(checkState = checked)
                }
            },
            onCategoryClick = { id ->
                updateUiState { state ->
                    state.copy(
                        expandCategory = if (state.expandCategory == id) null else id
                    )
                }
            },
            onForumClick = { id ->
                updateUiState { state ->
                    state.copy(
                        currentForum = id
                    )
                }
            }
        )
    )

    init {
        print("init:$this")
        viewModelScope.launch {
            val forums = forumCategoryUserCase()
            updateUiState { state ->
                state.copy(
                    forums = forums,
                )
            }
        }
    }

    private fun updateUiState(invoke: (GroupMemberUiState) -> GroupMemberUiState) {
        _uiState.update(invoke)
    }

    val uiState = _uiState.asStateFlow()
}

data class GroupMemberUiState(
    var forums: List<ForumCategory>,
    var expandCategory: String? = null,
    var currentForum: String? = null,
    var checkState: Boolean,
    val onForwardChange: (Boolean) -> Unit,
    val onCategoryClick: (String) -> Unit,
    val onForumClick: (String) -> Unit,
)

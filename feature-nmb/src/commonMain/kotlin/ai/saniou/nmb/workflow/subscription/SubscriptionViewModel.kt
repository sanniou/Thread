package ai.saniou.nmb.workflow.subscription

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.storage.SubscriptionStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 订阅列表ViewModel
 */
class SubscriptionViewModel(
    private val nmbXdApi: NmbXdApi,
    private val subscriptionStorage: SubscriptionStorage
) : ViewModel() {

    // 是否显示订阅ID设置对话框
    private val _showSubscriptionIdDialog = MutableStateFlow(false)
    val showSubscriptionIdDialog = _showSubscriptionIdDialog.asStateFlow()

    // 订阅列表UI状态
    private val _uiState = MutableStateFlow<UiStateWrapper>(UiStateWrapper.Loading)
    val uiState: StateFlow<UiStateWrapper> = _uiState.asStateFlow()

    // 当前页码
    private val _currentPage = MutableStateFlow(1L)

    // 数据UI状态
    private val dataUiState = MutableStateFlow(
        SubscriptionUiState(
            feeds = emptyList(),
            currentPage = 1,
            hasNextPage = false,
            onLoadNextPage = { loadNextPage() },
            onRefresh = { refreshSubscription() }
        )
    )

    init {
        // 初始化时加载订阅ID
        loadSubscriptionId()
    }

    /**
     * 加载订阅ID
     */
    private fun loadSubscriptionId() {
        viewModelScope.launch {
            subscriptionStorage.loadLastSubscriptionId()

            if (subscriptionStorage.subscriptionId.value == null) {
                // 如果没有订阅ID，显示设置对话框
                _showSubscriptionIdDialog.value = true
                _uiState.value = UiStateWrapper.Error(Exception("请先设置订阅ID"), "请先设置订阅ID")
            } else {
                // 有订阅ID，加载订阅列表
                loadSubscription()
            }
        }
    }

    /**
     * 加载订阅列表
     */
    private fun loadSubscription() {
        val id = subscriptionStorage.subscriptionId.value ?: return

        viewModelScope.launch {
            try {
                _uiState.value = UiStateWrapper.Loading

                val response = nmbXdApi.feed(id, 1)

                if (response is SaniouResponse.Success) {
                    val feeds = response.data

                    updateUiState { state ->
                        state.copy(
                            feeds = feeds,
                            currentPage = 1,
                            hasNextPage = feeds.isNotEmpty() // 如果有数据，假设有下一页
                        )
                    }

                    _currentPage.value = 1
                    _uiState.value = UiStateWrapper.Success(dataUiState.value)
                } else {
                    _uiState.value = UiStateWrapper.Error(Exception("加载失败"), "加载订阅列表失败")
                }
            } catch (e: Exception) {
                _uiState.value = UiStateWrapper.Error(e, "加载订阅列表失败: ${e.message}")
            }
        }
    }

    /**
     * 加载下一页
     */
    private fun loadNextPage() {
        val id = subscriptionStorage.subscriptionId.value ?: return
        val nextPage = _currentPage.value + 1

        viewModelScope.launch {
            try {
                val response = nmbXdApi.feed(id, nextPage)

                if (response is SaniouResponse.Success) {
                    val newFeeds = response.data

                    if (newFeeds.isNotEmpty()) {
                        // 合并新数据
                        val currentFeeds = dataUiState.value.feeds
                        val combinedFeeds = currentFeeds + newFeeds

                        updateUiState { state ->
                            state.copy(
                                feeds = combinedFeeds,
                                currentPage = nextPage,
                                hasNextPage = newFeeds.isNotEmpty() // 如果有数据，假设有下一页
                            )
                        }

                        _currentPage.value = nextPage
                        _uiState.value = UiStateWrapper.Success(dataUiState.value)
                    }
                }
            } catch (e: Exception) {
                // 加载下一页失败，保持当前状态
            }
        }
    }

    /**
     * 刷新订阅列表
     */
    fun refreshSubscription() {
        loadSubscription()
    }

    /**
     * 设置订阅ID
     */
    fun setSubscriptionId(id: String) {
        viewModelScope.launch {
            subscriptionStorage.addSubscriptionId(id)
            _showSubscriptionIdDialog.value = false
            loadSubscription()
        }
    }

    /**
     * 生成随机订阅ID
     */
    fun generateRandomSubscriptionId() {
        val randomId = subscriptionStorage.generateRandomSubscriptionId()
        setSubscriptionId(randomId)
    }

    /**
     * 显示订阅ID设置对话框
     */
    fun showSubscriptionIdDialog() {
        _showSubscriptionIdDialog.value = true
    }

    /**
     * 隐藏订阅ID设置对话框
     */
    fun hideSubscriptionIdDialog() {
        _showSubscriptionIdDialog.value = false
    }

    /**
     * 取消订阅
     */
    fun unsubscribe(threadId: Long) {
        val id = subscriptionStorage.subscriptionId.value ?: return

        viewModelScope.launch {
            try {
                nmbXdApi.delFeed(id, threadId)

                // 从列表中移除已取消订阅的帖子
                val currentFeeds = dataUiState.value.feeds
                val updatedFeeds = currentFeeds.filter { it.id != threadId }

                updateUiState { state ->
                    state.copy(
                        feeds = updatedFeeds
                    )
                }

                _uiState.value = UiStateWrapper.Success(dataUiState.value)
            } catch (e: Exception) {
                // 取消订阅失败，保持当前状态
            }
        }
    }

    private fun updateUiState(invoke: (SubscriptionUiState) -> SubscriptionUiState) {
        dataUiState.update(invoke)
    }

    fun getSubscriptionId() = subscriptionStorage.subscriptionId
}

/**
 * 订阅列表UI状态
 */
data class SubscriptionUiState(
    val feeds: List<Feed>,
    val currentPage: Long,
    val hasNextPage: Boolean,
    val onLoadNextPage: () -> Unit,
    val onRefresh: () -> Unit
) : UiStateWrapper

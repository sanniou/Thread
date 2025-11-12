package ai.saniou.nmb.workflow.subscription

import ai.saniou.coreui.state.UiStateWrapper
import ai.saniou.nmb.data.entity.Feed
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.domain.SubscriptionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 订阅列表ViewModel
 */
class SubscriptionViewModel(
    private val subscriptionUseCase: SubscriptionUseCase,
    private val subscriptionStorage: SubscriptionStorage
) : ViewModel() {

    // 是否显示订阅ID设置对话框
    private val _showSubscriptionIdDialog = MutableStateFlow(false)
    val showSubscriptionIdDialog = _showSubscriptionIdDialog.asStateFlow()

    // 订阅列表UI状态
    private val _uiState = MutableStateFlow<UiStateWrapper>(UiStateWrapper.Loading)
    val uiState: StateFlow<UiStateWrapper> = _uiState.asStateFlow()

    // 数据UI状态
    private val dataUiState = MutableStateFlow(
        SubscriptionUiState(
            feeds = emptyFlow()
        )
    )

    init {
        // 初始化时加载订阅ID
        loadSubscriptionId()

        viewModelScope.launch {
            subscriptionStorage.subscriptionId
                .filter { it -> !it.isNullOrEmpty() }
                .distinctUntilChanged()
                .collect {
                    loadSubscription(it!!)
                }
        }
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
            }
        }
    }

    /**
     * 加载订阅列表
     */
    private fun loadSubscription(subscriptionKey: String) {

        viewModelScope.launch {
            try {
                _uiState.value = UiStateWrapper.Loading
                val feedFLow = subscriptionUseCase.feed(subscriptionKey)
                updateUiState { state ->
                    state.copy(
                        feeds = feedFLow,
                    )
                }
                _uiState.value = UiStateWrapper.Success(dataUiState.value)
            } catch (e: Exception) {
                _uiState.value = UiStateWrapper.Error(e, "加载订阅列表失败: ${e.message}")
            }
        }
    }

    /**
     * 设置订阅ID
     */
    fun setSubscriptionId(id: String) {
        viewModelScope.launch {
            subscriptionStorage.addSubscriptionId(id)
            _showSubscriptionIdDialog.value = false
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
                subscriptionUseCase.delFeed(id, threadId)
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
    val feeds: Flow<PagingData<Feed>>,
) : UiStateWrapper

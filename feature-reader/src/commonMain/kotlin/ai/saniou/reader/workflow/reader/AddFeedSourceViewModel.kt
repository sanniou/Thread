package ai.saniou.reader.workflow.reader

import ai.saniou.thread.domain.model.reader.FeedSource
import ai.saniou.thread.domain.model.reader.FeedType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.getString
import thread.feature_reader.generated.resources.Res
import thread.feature_reader.generated.resources.s_c9819795fe

/**
 * 添加订阅源工作流的 UI 状态
 */
sealed interface AddFeedSourceUiState {
    /**
     * 初始状态，等待用户输入 URL
     */
    object EnterUrl : AddFeedSourceUiState

    /**
     * 正在分析 URL
     * @param url 目标 URL
     */
    data class Analyzing(val url: String) : AddFeedSourceUiState

    /**
     * 分析失败
     * @param url 目标 URL
     * @param error 错误信息
     */
    data class AnalysisFailed(val url: String, val error: String) : AddFeedSourceUiState

    /**
     * 从多个已发现的订阅源中选择
     * @param url 目标 URL
     * @param sources 发现的订阅源列表
     */
    data class SelectSource(val url: String, val sources: List<FeedSource>) : AddFeedSourceUiState

    /**
     * 最终确认订阅源信息
     * @param source 待确认的订阅源
     */
    data class ConfirmSource(val source: FeedSource) : AddFeedSourceUiState
}

/**
 * 管理添加订阅源向导工作流的 ViewModel
 */
class AddFeedSourceViewModel(
    private val sourceToEdit: FeedSource? = null, // 用于编辑模式
    private val urlAnalyzer: UrlAnalyzer = UrlAnalyzer() // 实际项目中应通过 DI 提供
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState: MutableStateFlow<AddFeedSourceUiState>
    val uiState: StateFlow<AddFeedSourceUiState>

    init {
        if (sourceToEdit != null) {
            // 编辑模式：直接进入确认页面
            _uiState = MutableStateFlow(AddFeedSourceUiState.ConfirmSource(sourceToEdit))
        } else {
            // 添加模式：从输入 URL 开始
            _uiState = MutableStateFlow(AddFeedSourceUiState.EnterUrl)
        }
        uiState = _uiState.asStateFlow()
    }

    /**
     * 当用户输入 URL 并点击下一步时调用
     */
    @OptIn(ExperimentalUuidApi::class)
    fun onUrlEntered(url: String) {
        if (url.isBlank()) {
            scope.launch {
                _uiState.value = AddFeedSourceUiState.AnalysisFailed(url, getString(Res.string.s_c9819795fe))
            }
            return
        }
        _uiState.value = AddFeedSourceUiState.Analyzing(url)

        // 使用分析器快速判断类型
        val feedType = urlAnalyzer.analyze(url)

        // 基于分析结果直接进入确认步骤
        val newSource = FeedSource(
            id = Uuid.random().toString(),
            name = url.substringAfterLast('/').substringBefore('.'), // 简单的名称提取
            url = url,
            type = feedType,
            description = null,
            iconUrl = null,
            selectorConfig = emptyMap(),
            autoRefresh = true,
            refreshInterval = 3600000 // Default 1 hour
        )
        _uiState.value = AddFeedSourceUiState.ConfirmSource(newSource)
    }

    /**
     * 当用户从多选列表中选择一个订阅源时调用
     */
    fun onSourceSelected(source: FeedSource) {
        _uiState.value = AddFeedSourceUiState.ConfirmSource(source)
    }

    /**
     * 返回上一步
     */
    fun onBack() {
        // 从任何状态返回到初始输入状态
        _uiState.value = AddFeedSourceUiState.EnterUrl
    }

    /**
     * 最终确认并保存订阅源
     */
    fun onConfirm(source: FeedSource, onSuccess: () -> Unit) = onSuccess()
}

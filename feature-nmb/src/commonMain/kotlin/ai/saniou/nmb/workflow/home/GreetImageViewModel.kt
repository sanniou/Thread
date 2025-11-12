package ai.saniou.nmb.workflow.home

import ai.saniou.nmb.data.storage.GreetImageStorage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 欢迎图片ViewModel
 *
 * 负责获取和管理欢迎图片
 */
class GreetImageViewModel(
    private val greetImageStorage: GreetImageStorage
) : ViewModel() {

    private val _greetImageUrl = MutableStateFlow<String?>(null)
    val greetImageUrl: StateFlow<String?> = _greetImageUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadGreetImage()
    }

    /**
     * 获取带时间戳的欢迎图片URL
     *
     * 使用 https://nmb.ovear.info/h.php?time=currenttimestamp 格式
     */
    @OptIn(ExperimentalTime::class)
    private fun getGreetImageUrlWithTimestamp(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "https://nmb.ovear.info/h.php?time=$timestamp"
    }

    /**
     * 加载欢迎图片
     */
    fun loadGreetImage() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // 尝试从缓存加载
                val cachedUrl = greetImageStorage.getCachedGreetImageUrl()

                if (cachedUrl != null && !greetImageStorage.isGreetImageExpired()) {
                    // 使用缓存数据
                    _greetImageUrl.value = cachedUrl
                } else {
                    // 从网络加载新数据，使用带时间戳的URL
                    val url = getGreetImageUrlWithTimestamp()

                    // 保存到缓存
                    greetImageStorage.saveGreetImageUrl(url)

                    _greetImageUrl.value = url
                }
            } catch (e: Exception) {
                // 如果加载失败但有缓存，使用缓存
                val cachedUrl = greetImageStorage.getCachedGreetImageUrl()
                if (cachedUrl != null) {
                    _greetImageUrl.value = cachedUrl
                } else {
                    // 如果没有缓存，使用带时间戳的URL
                    _greetImageUrl.value = getGreetImageUrlWithTimestamp()
                }
                // 可以添加错误处理逻辑
            } finally {
                _isLoading.value = false
            }
        }
    }

}

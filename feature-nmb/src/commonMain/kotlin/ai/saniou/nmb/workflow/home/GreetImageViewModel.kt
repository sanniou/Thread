package ai.saniou.nmb.workflow.home

import ai.saniou.thread.domain.usecase.GetGreetImageUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 欢迎图片ViewModel
 *
 * 负责获取和管理欢迎图片
 */
class GreetImageViewModel(
    private val getGreetImageUseCase: GetGreetImageUseCase
) : ScreenModel {

    private val _greetImageUrl = MutableStateFlow<String?>(null)
    val greetImageUrl: StateFlow<String?> = _greetImageUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadGreetImage()
    }

    /**
     * 加载欢迎图片
     */
    fun loadGreetImage() {
        screenModelScope.launch {
            _isLoading.value = true
            try {
                _greetImageUrl.value = getGreetImageUseCase()
            } catch (e: Exception) {
                // Error handling can be improved
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

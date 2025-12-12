package ai.saniou.forum.initializer

import ai.saniou.thread.data.manager.CdnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 应用初始化器，负责应用启动时的初始化工作
 */
class AppInitializer(
    private val cdnManager: CdnManager
) {
    private val initializerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 初始化状态
    private val _initializationState =
        MutableStateFlow<InitializationState>(InitializationState.NotStarted)
    val initializationState: StateFlow<InitializationState> = _initializationState.asStateFlow()

    /**
     * 初始化应用
     */
    fun initialize() {
        if (_initializationState.value != InitializationState.NotStarted) {
            return
        }

        _initializationState.value = InitializationState.InProgress

        initializerScope.launch {
            try {
                // 初始化CDN
                val cdnInitialized = cdnManager.initialize()

                // 可以在这里添加其他初始化逻辑

                _initializationState.value = InitializationState.Completed
            } catch (e: Exception) {
                _initializationState.value = InitializationState.Failed(e)
            }
        }
    }
}

/**
 * 初始化状态
 */
sealed class InitializationState {
    object NotStarted : InitializationState()
    object InProgress : InitializationState()
    object Completed : InitializationState()
    data class Failed(val error: Throwable) : InitializationState()
}

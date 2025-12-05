package ai.saniou.nmb.data.manager

import ai.saniou.thread.network.SaniouResponse
import ai.saniou.nmb.data.entity.CdnPath
import ai.saniou.nmb.data.repository.ForumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CDN管理器，负责获取和管理图片CDN地址
 */
class CdnManager(private val forumRepository: ForumRepository) {

    // 默认CDN地址
    private val defaultCdnUrl = "https://image.nmb.best"

    // 当前使用的CDN地址
    private val _currentCdnUrl = MutableStateFlow(defaultCdnUrl)
    val currentCdnUrl: StateFlow<String> = _currentCdnUrl.asStateFlow()

    // 所有可用的CDN地址
    private val _availableCdnPaths = MutableStateFlow<List<CdnPath>>(emptyList())
    val availableCdnPaths: StateFlow<List<CdnPath>> = _availableCdnPaths.asStateFlow()

    // 初始化状态
    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    /**
     * 初始化CDN地址
     *
     * @return 是否初始化成功
     */
    suspend fun initialize(): Boolean {
        if (_initialized.value) {
            return true
        }

        return try {
            when (val response = forumRepository.getCdnPath()) {
                is SaniouResponse.Success -> {
                    val cdnPaths = response.data
                    if (cdnPaths.isNotEmpty()) {
                        _availableCdnPaths.value = cdnPaths

                        // 选择权重最高的CDN地址
                        val bestCdnPath = cdnPaths.maxByOrNull { it.rate }
                        if (bestCdnPath != null) {
                            _currentCdnUrl.value = bestCdnPath.url
                        }

                        _initialized.value = true
                        true
                    } else {
                        // 如果没有获取到CDN地址，使用默认地址
                        _initialized.value = true
                        false
                    }
                }

                else -> {
                    // 如果请求失败，使用默认地址
                    _initialized.value = true
                    false
                }
            }
        } catch (e: Exception) {
            // 发生异常时，使用默认地址
            _initialized.value = true
            false
        }
    }

    /**
     * 获取当前CDN地址
     */
    fun getCurrentCdnUrl(): String {
        return _currentCdnUrl.value
    }

    /**
     * 切换到下一个CDN地址
     *
     * @return 切换后的CDN地址
     */
    fun switchToNextCdn(): String {
        val cdnPaths = _availableCdnPaths.value
        if (cdnPaths.size <= 1) {
            return _currentCdnUrl.value
        }

        val currentIndex = cdnPaths.indexOfFirst { it.url == _currentCdnUrl.value }
        val nextIndex =
            if (currentIndex == -1 || currentIndex == cdnPaths.size - 1) 0 else currentIndex + 1

        _currentCdnUrl.value = cdnPaths[nextIndex].url
        return _currentCdnUrl.value
    }

    /**
     * 构建完整的图片URL
     *
     * @param imgPath 图片路径
     * @param ext 图片扩展名
     * @param isThumb 是否为缩略图
     * @return 完整的图片URL
     */
    fun buildImageUrl(imgPath: String, ext: String, isThumb: Boolean = true): String {
        val cdnUrl = _currentCdnUrl.value.removeSuffix("/")
        val path = if (isThumb) "thumb" else "image"
        return "$cdnUrl/$path/$imgPath$ext"
    }
}

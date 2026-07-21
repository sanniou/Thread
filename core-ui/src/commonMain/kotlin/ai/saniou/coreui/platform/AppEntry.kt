package ai.saniou.coreui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Cross-platform entry bus for deep links, notification clicks and file-open intents.
 * Platform shells only enqueue URLs; common App code owns routing.
 */
class AppEntryController {
    private val _entries = MutableSharedFlow<AppEntry>(extraBufferCapacity = 16)
    val entries: SharedFlow<AppEntry> = _entries.asSharedFlow()

    fun open(url: String, source: AppEntrySource = AppEntrySource.EXTERNAL) {
        val normalized = url.trim()
        if (normalized.isBlank()) return
        _entries.tryEmit(AppEntry(normalized, source))
    }
}

data class AppEntry(
    val url: String,
    val source: AppEntrySource = AppEntrySource.EXTERNAL,
)

enum class AppEntrySource {
    LAUNCH_ARG,
    NOTIFICATION,
    EXTERNAL,
    FILE_IMPORT,
}

val LocalAppEntryController = staticCompositionLocalOf<AppEntryController?> { null }

/** Platform share boundary. Desktop falls back to clipboard when no OS share sheet exists. */
fun interface ShareService {
    /**
     * @return true when the platform accepted the share request.
     */
    fun shareText(text: String, title: String?): Boolean
}

fun ShareService.shareText(text: String): Boolean = shareText(text, null)

val LocalShareService = staticCompositionLocalOf<ShareService?> { null }

/** Local file import/export for portable user-data bundles. */
interface UserDataFileService {
    suspend fun exportText(suggestedFileName: String, text: String): Result<String>
    suspend fun importText(allowedExtensions: Set<String> = setOf("json", "txt")): Result<String>
}

val LocalUserDataFileService = staticCompositionLocalOf<UserDataFileService?> { null }

/**
 * OS notification surface. Click payloads are re-enqueued into [AppEntryController]
 * so routing stays identical to deep links.
 */
interface SystemNotificationService {
    fun isAvailable(): Boolean
    fun notify(
        title: String,
        body: String,
        deepLink: String? = null,
        notificationId: String = title,
    ): Boolean
}

val LocalSystemNotificationService = staticCompositionLocalOf<SystemNotificationService?> { null }

/** Lifecycle-aware background refresh bridge owned by the platform shell. */
interface BackgroundRefreshBridge {
    fun start()
    fun stop()
}

val LocalBackgroundRefreshBridge = staticCompositionLocalOf<BackgroundRefreshBridge?> { null }

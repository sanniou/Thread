package ai.saniou.thread.platform

import ai.saniou.coreui.platform.AppEntryController
import ai.saniou.coreui.platform.AppEntrySource
import ai.saniou.coreui.platform.BackgroundRefreshBridge
import ai.saniou.coreui.platform.ShareService
import ai.saniou.coreui.platform.SystemNotificationService
import ai.saniou.coreui.platform.UserDataFileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume

class DesktopShareService : ShareService {
    override fun shareText(text: String, title: String?): Boolean {
        if (text.isBlank()) return false
        return runCatching {
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(StringSelection(text), null)
            true
        }.getOrDefault(false)
    }
}

class DesktopUserDataFileService : UserDataFileService {
    override suspend fun exportText(suggestedFileName: String, text: String): Result<String> =
        runCatching {
            val file = chooseFile(save = true, suggestedName = suggestedFileName)
                ?: error("已取消导出")
            withContext(Dispatchers.IO) {
                file.parentFile?.mkdirs()
                file.writeText(text, StandardCharsets.UTF_8)
                file.absolutePath
            }
        }

    override suspend fun importText(allowedExtensions: Set<String>): Result<String> =
        runCatching {
            val file = chooseFile(save = false, allowedExtensions = allowedExtensions)
                ?: error("已取消导入")
            withContext(Dispatchers.IO) {
                require(file.isFile) { "文件不存在" }
                require(file.length() <= MAX_IMPORT_BYTES) { "导入文件不能超过 8 MB" }
                file.readText(StandardCharsets.UTF_8)
            }
        }

    private suspend fun chooseFile(
        save: Boolean,
        suggestedName: String = "thread-user-data.json",
        allowedExtensions: Set<String> = setOf("json", "txt"),
    ): File? = suspendCancellableCoroutine { continuation ->
        EventQueue.invokeLater {
            if (!continuation.isActive) return@invokeLater
            val dialog = FileDialog(
                null as Frame?,
                if (save) "导出用户数据" else "导入用户数据",
                if (save) FileDialog.SAVE else FileDialog.LOAD,
            ).apply {
                file = suggestedName
                filenameFilter = java.io.FilenameFilter { _, name ->
                    name.substringAfterLast('.', "").lowercase() in allowedExtensions
                }
                isMultipleMode = false
                isVisible = true
            }
            val selected = dialog.file?.let { name -> File(dialog.directory, name) }
            dialog.dispose()
            if (continuation.isActive) continuation.resume(selected)
        }
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 8L * 1024L * 1024L
    }
}

class DesktopSystemNotificationService(
    private val entryController: AppEntryController,
) : SystemNotificationService {
    private var trayIcon: TrayIcon? = null
    private val deepLinksByCaption = linkedMapOf<String, String>()

    override fun isAvailable(): Boolean = runCatching {
        SystemTray.isSupported()
    }.getOrDefault(false)

    override fun notify(
        title: String,
        body: String,
        deepLink: String?,
        notificationId: String,
    ): Boolean {
        if (!isAvailable()) return false
        return runCatching {
            val icon = trayIcon ?: createTrayIcon().also { trayIcon = it }
            if (!deepLink.isNullOrBlank()) {
                deepLinksByCaption[title] = deepLink
                while (deepLinksByCaption.size > 32) {
                    deepLinksByCaption.remove(deepLinksByCaption.keys.first())
                }
            }
            icon.displayMessage(title, body, TrayIcon.MessageType.INFO)
            true
        }.getOrDefault(false)
    }

    private fun createTrayIcon(): TrayIcon {
        val tray = SystemTray.getSystemTray()
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).apply {
            val g = createGraphics()
            g.color = java.awt.Color(0x1A, 0x73, 0xE8)
            g.fillRoundRect(1, 1, 14, 14, 6, 6)
            g.dispose()
        }
        val icon = TrayIcon(image, "Thread").apply {
            isImageAutoSize = true
            addActionListener {
                val deepLink = deepLinksByCaption.values.lastOrNull()
                if (!deepLink.isNullOrBlank()) {
                    entryController.open(deepLink, AppEntrySource.NOTIFICATION)
                }
            }
        }
        tray.add(icon)
        return icon
    }
}

class DesktopBackgroundRefreshBridge(
    private val onTick: suspend () -> Unit,
    private val intervalMillis: Long = DEFAULT_TICK_MILLIS,
) : BackgroundRefreshBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                runCatching { onTick() }
                delay(intervalMillis)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val DEFAULT_TICK_MILLIS = 15 * 60 * 1000L
    }
}

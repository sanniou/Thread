package ai.saniou.thread

import ai.saniou.forum.workflow.post.AttachmentPicker
import ai.saniou.thread.domain.model.forum.PostAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.coroutines.resume

class DesktopAttachmentPicker : AttachmentPicker {
    override suspend fun pickImage(): PostAttachment? {
        val file = chooseFile() ?: return null
        return withContext(Dispatchers.IO) {
            require(file.length() <= MAX_ATTACHMENT_BYTES) { "图片不能超过 20 MB" }
            PostAttachment(
                fileName = file.name,
                bytes = file.readBytes(),
                contentType = file.extension.toContentType(),
            )
        }
    }

    private suspend fun chooseFile(): File? = suspendCancellableCoroutine { continuation ->
        EventQueue.invokeLater {
            if (!continuation.isActive) return@invokeLater
            val dialog = FileDialog(null as Frame?, "选择图片", FileDialog.LOAD).apply {
                filenameFilter = java.io.FilenameFilter { _, name ->
                    name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
                }
                isMultipleMode = false
                isVisible = true
            }
            val selected = dialog.file?.let { name -> File(dialog.directory, name) }
            dialog.dispose()
            if (continuation.isActive) continuation.resume(selected)
        }
    }

    private fun String.toContentType() = when (lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 20L * 1024L * 1024L
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    }
}

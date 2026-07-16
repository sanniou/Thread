package ai.saniou.forum.workflow.post

import ai.saniou.thread.domain.model.forum.PostAttachment
import androidx.compose.runtime.staticCompositionLocalOf

/** Platform service boundary; posting state and upload stay common. */
fun interface AttachmentPicker {
    suspend fun pickImage(): PostAttachment?
}

val LocalAttachmentPicker = staticCompositionLocalOf<AttachmentPicker?> { null }

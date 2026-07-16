package ai.saniou.coreui.composition

import ai.saniou.thread.domain.model.source.DEFAULT_FORUM_SOURCE_ID
import androidx.compose.runtime.compositionLocalOf

val LocalForumSourceId = compositionLocalOf { DEFAULT_FORUM_SOURCE_ID }

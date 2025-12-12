package ai.saniou.forum.workflow.image

import androidx.compose.runtime.Composable

interface ImageSaver {
    suspend fun save(url: String): Boolean
}

@Composable
expect fun rememberImageSaver(): ImageSaver

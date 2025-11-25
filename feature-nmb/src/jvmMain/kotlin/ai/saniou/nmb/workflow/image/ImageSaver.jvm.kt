package ai.saniou.nmb.workflow.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageSaverImpl() : ImageSaver {
    override suspend fun save(url: String): Boolean = withContext(Dispatchers.IO) {
        TODO()
    }
}

@Composable
actual fun rememberImageSaver(): ImageSaver {
    return remember { ImageSaverImpl() }
}

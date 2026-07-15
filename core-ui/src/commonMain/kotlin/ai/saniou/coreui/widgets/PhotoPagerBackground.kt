package ai.saniou.coreui.widgets

import ai.saniou.coreui.widgets.palette.PhotoPalette
import ai.saniou.coreui.widgets.palette.toSimplePalette
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.BitmapImage
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.request.ImageResult
import com.github.panpf.sketch.request.disallowAnimatedImage
import com.github.panpf.sketch.resize.Precision
import com.github.panpf.sketch.transform.BlurTransformation
import com.github.panpf.sketch.util.toSketchSize
import com.github.panpf.sketch.util.windowContainerSize
import com.kmpalette.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoPagerBackground(
    imageUri: String,
    photoPaletteState: MutableState<PhotoPalette>,
) {
    val colorScheme = MaterialTheme.colorScheme
    val imageState = rememberAsyncImageState()
    LaunchedEffect(imageState, colorScheme) {
        snapshotFlow { imageState.result }.collect { result ->
            val bitmap = ((result as? ImageResult.Success)?.image as? BitmapImage)?.bitmap
                ?: return@collect
            val palette = withContext(Dispatchers.Default) {
                runCatching { Palette.Builder(bitmap).generate().toSimplePalette() }.getOrNull()
            }
            photoPaletteState.value = PhotoPalette(palette, colorScheme)
        }
    }
    val windowsSize = windowContainerSize()
    val imageSize = remember { (windowsSize / 4).toSketchSize() }
    val request = ComposableImageRequest(imageUri) {
        resize(size = imageSize, precision = Precision.SMALLER_SIZE)
        addTransformations(BlurTransformation(radius = 20, maskColor = 0x63000000))
        memoryCachePolicy(CachePolicy.DISABLED)
        resultCachePolicy(CachePolicy.DISABLED)
        disallowAnimatedImage()
        crossfade(alwaysUse = true, durationMillis = 400)
        resizeOnDraw()
    }
    AsyncImage(
        request = request,
        state = imageState,
        contentDescription = "Background",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

package ai.saniou.coreui.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.AsyncImage
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.rememberMaskProgressPainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageOptions
import com.github.panpf.sketch.state.ColorPainterStateImage

@Composable
fun NetworkImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Fit,
) {

    val progressPainter = rememberMaskProgressPainter()
    val state = rememberAsyncImageState(ComposableImageOptions {
        placeholder(ColorPainterStateImage(MaterialTheme.colorScheme.primaryContainer))
        error(ColorPainterStateImage(MaterialTheme.colorScheme.errorContainer))
        crossfade()
        resizeOnDraw()
        // There is a lot more...
    })
    AsyncImage(
        uri = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier.progressIndicator(state, progressPainter),
        contentScale = contentScale,
    )
}
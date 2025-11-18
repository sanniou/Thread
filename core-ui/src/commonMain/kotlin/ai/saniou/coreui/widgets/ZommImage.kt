package ai.saniou.coreui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.panpf.zoomimage.SketchZoomAsyncImage

@Composable
fun ZoomAsyncImage(
    uri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) = SketchZoomAsyncImage(
    uri = uri,
    contentDescription = contentDescription,
    modifier = modifier,
)

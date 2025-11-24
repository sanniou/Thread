package ai.saniou.coreui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.github.panpf.sketch.ability.progressIndicator
import com.github.panpf.sketch.painter.rememberSectorProgressPainter
import com.github.panpf.sketch.rememberAsyncImageState
import com.github.panpf.sketch.request.ComposableImageRequest
import com.github.panpf.sketch.state.ThumbnailMemoryCacheStateImage
import com.github.panpf.zoomimage.SketchZoomAsyncImage
import com.github.panpf.zoomimage.rememberSketchZoomState

/**
 * A composable function that displays an image with zoom functionality.
 * This component provides an enhanced image viewing experience with zoom and pan capabilities.
 *
 * @param uri The URI of the image to be displayed
 * @param thumbnailUrl The URL of the thumbnail image to show while loading the full image
 * @param contentDescription Text description of the image for accessibility
 * @param alignment Alignment parameter used to place the image within its bounds
 * @param contentScale Scale parameter used to determine the aspect ratio scaling of the image
 * @param modifier Modifier to be applied to the image
 */
@Composable
fun ZoomAsyncImage(
    uri: String?,
    thumbnailUrl: String?,
    contentDescription: String?,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier,
) {
    // Create a capturable state for image capture functionality
    val capturableState = rememberCapturableState()
    // Create a zoom state for image zoom and pan functionality
    val zoomState = rememberSketchZoomState()


    // Configure zoom behavior settings
    zoomState.zoomable.setThreeStepScale(true) // Enable three-step zoom (1x, 2x, 3x)
    zoomState.zoomable.setReverseMouseWheelScale(true) // Reverse mouse wheel zoom direction

    // Create an async image state for loading progress tracking
    val imageState = rememberAsyncImageState()
    // Render the zoomable image with all configurations
    SketchZoomAsyncImage(
        request = ComposableImageRequest(uri) {
            placeholder(ThumbnailMemoryCacheStateImage(thumbnailUrl)) // Set thumbnail placeholder
            crossfade(fadeStart = false) // Enable crossfade animation
//            merge(getPlatformSketchZoomAsyncImageSampleImageOptions()) // Platform-specific options (commented out)
        },
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        modifier = modifier
            .progressIndicator(imageState, rememberSectorProgressPainter()) // Add progress indicator
            .capturable(capturableState), // Enable image capture
        state = imageState,
        zoomState = zoomState,
    )
}

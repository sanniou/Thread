package ai.saniou.coreui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Quiet product motion tokens.
 * Prefer short rises and soft fades; never blur-heavy landing-page motion.
 */
object ThreadMotion {
    const val InstantMs = 0
    const val FastMs = 120
    const val MediumMs = 180
    const val SlowMs = 260

    val listItemRise: Dp = 8.dp
    val cardPressScale = 0.985f
}

@Composable
@ReadOnlyComposable
fun threadTween(
    durationMillis: Int = ThreadMotion.MediumMs,
    delayMillis: Int = 0,
): TweenSpec<Float> {
    val reduced = LocalThreadUiPreferences.current.reducedMotion
    return if (reduced) {
        tween(durationMillis = ThreadMotion.InstantMs)
    } else {
        tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        )
    }
}

@Composable
@ReadOnlyComposable
fun <T> threadTweenSpec(
    durationMillis: Int = ThreadMotion.MediumMs,
    delayMillis: Int = 0,
): FiniteAnimationSpec<T> {
    val reduced = LocalThreadUiPreferences.current.reducedMotion
    return if (reduced) {
        snap()
    } else {
        tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = FastOutSlowInEasing,
        )
    }
}

@Composable
@ReadOnlyComposable
fun threadContentSizeSpec(
    durationMillis: Int = ThreadMotion.MediumMs,
): FiniteAnimationSpec<IntSize> = threadTweenSpec(durationMillis = durationMillis)

@Composable
@ReadOnlyComposable
fun <T> threadSpringSpec(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
): AnimationSpec<T> {
    val reduced = LocalThreadUiPreferences.current.reducedMotion
    return if (reduced) {
        snap()
    } else {
        spring(dampingRatio = dampingRatio, stiffness = stiffness)
    }
}

@Composable
@ReadOnlyComposable
fun threadMotionMillis(preferred: Int): Int {
    return if (LocalThreadUiPreferences.current.reducedMotion) ThreadMotion.InstantMs else preferred
}

/** Fade-in/out for [LazyItemScope.animateItem]; null when reducedMotion (no list enter noise). */
@Composable
@ReadOnlyComposable
fun threadListItemFadeSpec(
    durationMillis: Int = ThreadMotion.FastMs,
): FiniteAnimationSpec<Float>? {
    if (LocalThreadUiPreferences.current.reducedMotion) return null
    return threadTweenSpec(durationMillis = durationMillis)
}

/** Placement reordering for [LazyItemScope.animateItem]; null when reducedMotion. */
@Composable
@ReadOnlyComposable
fun threadListItemPlacementSpec(
    durationMillis: Int = ThreadMotion.MediumMs,
): FiniteAnimationSpec<IntOffset>? {
    if (LocalThreadUiPreferences.current.reducedMotion) return null
    return threadTweenSpec(durationMillis = durationMillis)
}

/**
 * Quiet list item enter/exit/reorder animation for LazyColumn/LazyRow items.
 * Requires a stable item [key]. Honors [LocalThreadUiPreferences.reducedMotion].
 */
@Composable
fun LazyItemScope.threadAnimateItem(): Modifier =
    Modifier.animateItem(
        fadeInSpec = threadListItemFadeSpec(),
        placementSpec = threadListItemPlacementSpec(),
        fadeOutSpec = threadListItemFadeSpec(),
    )

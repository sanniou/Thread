package ai.saniou.nmb.workflow.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import thread.feature_nmb.generated.resources.Res
import thread.feature_nmb.generated.resources.favorites
import thread.feature_nmb.generated.resources.history
import thread.feature_nmb.generated.resources.home
import thread.feature_nmb.generated.resources.profile
import thread.feature_nmb.generated.resources.shopping
import thread.feature_nmb.generated.resources.trend

enum class AppDestinations(
    val label: StringResource,
    val icon: ImageVector,
    val contentDescription: StringResource
) {
    HOME(Res.string.home, Icons.Default.Home, Res.string.home),
    TREND(Res.string.trend, Icons.Default.ThumbUp, Res.string.trend),
    FAVORITES(Res.string.favorites, Icons.Default.Favorite, Res.string.favorites),
    HISTORY(Res.string.history, Icons.Default.Face, Res.string.history),
    SHOPPING(Res.string.shopping, Icons.Default.ShoppingCart, Res.string.shopping),
    PROFILE(Res.string.profile, Icons.Default.AccountBox, Res.string.profile),
}

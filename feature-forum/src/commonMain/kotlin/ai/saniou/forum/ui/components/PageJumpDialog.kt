package ai.saniou.forum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.s_053dd510fc
import thread.feature_forum.generated.resources.s_3fbf1853e5
import thread.feature_forum.generated.resources.s_4565fe5a08
import thread.feature_forum.generated.resources.s_f374b88bfd
import thread.feature_forum.generated.resources.subscription_cancel
import thread.feature_forum.generated.resources.subscription_confirm

/**
 * 页面跳转对话框
 */
@Composable
fun PageJumpDialog(
    currentPage: Int,
    totalPages: Int,
    onDismissRequest: () -> Unit,
    onJumpToPage: (Int) -> Unit
) {
    var targetPage by remember { mutableStateOf(currentPage.toString()) }
    var sliderPosition by remember { mutableStateOf(currentPage.toFloat()) }
    var isError by remember { mutableStateOf(false) }

    fun validate(input: String) {
        val page = input.toIntOrNull()
        isError = page == null || page !in 1..totalPages
    }

    LaunchedEffect(currentPage) {
        targetPage = currentPage.toString()
        sliderPosition = currentPage.toFloat()
        validate(targetPage)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(Res.string.s_4565fe5a08), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(Res.string.s_3fbf1853e5), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(Res.string.s_f374b88bfd, totalPages), style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedTextField(
                value = targetPage,
                onValueChange = {
                    targetPage = it
                    validate(it)
                    it.toIntOrNull()?.let { page ->
                        sliderPosition = page.toFloat()
                    }
                },
                isError = isError,
                label = { Text(stringResource(Res.string.s_053dd510fc, totalPages)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    targetPage = it.roundToInt().toString()
                    validate(targetPage)
                },
                valueRange = 1f..totalPages.toFloat(),
                steps = if (totalPages > 1) totalPages - 2 else 0
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaniouTextButton(onClick = onDismissRequest, text = stringResource(Res.string.subscription_cancel))
                SaniouButton(
                    onClick = {
                        onJumpToPage(targetPage.toInt())
                        onDismissRequest()
                    },
                    enabled = !isError,
                    text = stringResource(Res.string.subscription_confirm),
                )
            }
        }
    }
}

package ai.saniou.nmb.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("跳转到页面") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("选择页码")
                    Text("$totalPages 页")
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
                    label = { Text("页码 (1-$totalPages)") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onJumpToPage(targetPage.toInt())
                    onDismissRequest()
                },
                enabled = !isError
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

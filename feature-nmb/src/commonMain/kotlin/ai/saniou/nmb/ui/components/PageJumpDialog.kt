package ai.saniou.nmb.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
    var sliderPosition by remember { mutableStateOf(currentPage.toFloat()) }

    // 使用AlertDialog实现页面跳转对话框
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("跳转到页面") },
        text = {
            Column {
                Text("当前页面: ${sliderPosition.toInt()} / $totalPages")

                // 页面选择器
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 1f..totalPages.toFloat(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onJumpToPage(sliderPosition.toInt())
                    onDismissRequest()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("取消")
            }
        }
    )
}

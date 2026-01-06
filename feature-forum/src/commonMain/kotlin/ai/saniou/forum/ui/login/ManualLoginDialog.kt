package ai.saniou.forum.ui.login

import ai.saniou.coreui.theme.Dimens
import ai.saniou.thread.domain.model.user.LoginField
import ai.saniou.thread.domain.model.user.LoginStrategy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ManualLoginDialog(
    strategy: LoginStrategy.Manual,
    onDismissRequest: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    val inputs = remember { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = strategy.title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = strategy.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Dimens.padding_medium))

                strategy.fields.forEach { field ->
                    OutlinedTextField(
                        value = inputs[field.key] ?: "",
                        onValueChange = {
                            inputs[field.key] = it
                        },
                        label = { Text(field.label) },
                        placeholder = { Text(field.hint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (field.isMultiline) Dimens.size_120 else Dimens.size_56),
                        singleLine = !field.isMultiline,
                        maxLines = if (field.isMultiline) 5 else 1
                    )
                    Spacer(modifier = Modifier.height(Dimens.padding_small))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(inputs.toMap())
                },
                enabled = strategy.fields.all { !it.isRequired || !inputs[it.key].isNullOrBlank() }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

package ai.saniou.forum.ui.login

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.widgets.AdaptiveModal
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.thread.domain.model.user.LoginStrategy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.action_save
import thread.feature_forum.generated.resources.subscription_cancel

@Composable
fun ManualLoginDialog(
    strategy: LoginStrategy.Manual,
    onDismissRequest: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    val inputs = remember { mutableStateMapOf<String, String>() }

    AdaptiveModal(onDismissRequest = onDismissRequest) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text(strategy.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = strategy.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Dimens.padding_medium))
                strategy.fields.forEach { field ->
                    OutlinedTextField(
                        value = inputs[field.key].orEmpty(),
                        onValueChange = { inputs[field.key] = it },
                        label = { Text(field.label) },
                        placeholder = { Text(field.hint) },
                        modifier = Modifier.fillMaxWidth()
                            .height(if (field.isMultiline) Dimens.size_120 else Dimens.size_56),
                        singleLine = !field.isMultiline,
                        maxLines = if (field.isMultiline) 5 else 1,
                    )
                    Spacer(modifier = Modifier.height(Dimens.padding_small))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                SaniouTextButton(onClick = onDismissRequest, text = stringResource(Res.string.subscription_cancel))
                SaniouButton(
                    onClick = { onConfirm(inputs.toMap()) },
                    enabled = strategy.fields.all { !it.isRequired || !inputs[it.key].isNullOrBlank() },
                    text = stringResource(Res.string.action_save),
                )
            }
        }
    }
}

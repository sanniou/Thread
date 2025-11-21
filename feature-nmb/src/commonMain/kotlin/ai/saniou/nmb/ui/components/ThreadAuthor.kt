package ai.saniou.nmb.ui.components

import ai.saniou.coreui.theme.Dimens
import ai.saniou.nmb.data.entity.IBaseAuthor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ThreadAuthor(author: IBaseAuthor, isPo: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_small)
    ) {
        PoTag(isPo)
        Text(
            text = author.userHash,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (isPo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (author.name.isNotBlank() && author.name != "无名氏") {
            Text(
                text = author.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = author.now,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

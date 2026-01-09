package ai.saniou.forum.workflow.topicdetail

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
import ai.saniou.forum.ui.components.Badge
import ai.saniou.forum.ui.components.ThreadAuthor
import ai.saniou.forum.ui.components.ThreadBody
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.TopicMetadata
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.*

@Composable
fun TopicHeader(
    metadata: TopicMetadata,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.padding_standard,
                end = Dimens.padding_standard,
                top = Dimens.padding_large,
                bottom = Dimens.padding_small
            )
    ) {
        // Title
        if (!metadata.title.isNullOrBlank() && metadata.title != stringResource(Res.string.empty_title)) {
            Text(
                text = metadata.title!!,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    lineHeight = 32.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimens.padding_small))
        }

        // Tags / Badges Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source Badge
            if (metadata.sourceName.isNotBlank()) {
                Badge(
                    text = metadata.sourceName.uppercase(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Status Badges
            if (metadata.isAdmin) {
                Badge(
                    text = stringResource(Res.string.flag_admin),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            if (metadata.isSage) {
                Badge(
                    text = "SAGE",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            // Custom Tags
            metadata.tags?.forEach { tag ->
                Badge(
                    text = tag.name,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Polls (Placeholder for now)
        if (metadata.poll != null) {
            Spacer(modifier = Modifier.height(Dimens.padding_medium))
            Text(stringResource(Res.string.poll_not_implemented), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ThreadMainPost(
    comment: Comment,
    refClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.padding_medium),
        ) {
            // Author Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.padding_standard),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ThreadAuthor(
                    author = comment.author,
                    threadTime = comment.createdAt.toRelativeTimeString(),
                    isPo = true,
                    onClick = onUserClick,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.padding_medium))

            // Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { /* Allow inner clicks */ },
                        onLongClick = { showMenu = true }
                    )
                    .padding(horizontal = Dimens.padding_standard)
            ) {
                ThreadBody(
                    content = comment.content,
                    images = comment.images,
                    onReferenceClick = refClick,
                    onImageClick = onImageClick,
                    onImageLongClick = { image -> onBookmarkImage(image) }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.padding_large))

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.padding_small),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 ActionButton(
                    icon = Icons.Default.Reply,
                    text = stringResource(Res.string.reply),
                    onClick = { /* TODO */ }
                )
                ActionButton(
                    icon = Icons.Outlined.ContentCopy,
                    text = stringResource(Res.string.copy),
                    onClick = onCopy
                )
                ActionButton(
                    icon = Icons.Outlined.BookmarkBorder,
                    text = stringResource(Res.string.bookmark),
                    onClick = onBookmark
                )
                ActionButton(
                    icon = Icons.Outlined.Share,
                    text = stringResource(Res.string.share),
                    onClick = { showMenu = true }
                )
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                 DropdownMenuItem(
                    text = { Text(stringResource(Res.string.copy_content)) },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = {
                        onCopy()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.bookmark_thread)) },
                    leadingIcon = { Icon(Icons.Filled.BookmarkBorder, null) },
                    onClick = {
                        onBookmark()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadReply(
    reply: Comment,
    poUserHash: String,
    onReplyClicked: (String) -> Unit,
    refClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPo = remember(reply.author.id, poUserHash) {
        reply.author.id == poUserHash
    }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = { onReplyClicked(reply.id) },
                onLongClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    showMenu = true
                }
            )
            .padding(
                horizontal = Dimens.padding_standard,
                vertical = Dimens.padding_medium
            )
    ) {
        // Header: Author + Time + Floor/ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThreadAuthor(
                author = reply.author,
                threadTime = reply.createdAt.toRelativeTimeString(),
                isPo = isPo,
                onClick = onUserClick,
                badges = {
                    if (reply.isAdmin) {
                        Badge(
                            text = stringResource(Res.string.flag_admin),
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    reply.floor?.let { floor ->
                        Text(
                            text = "#$floor",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Text(
                     text = "ID:${reply.id}",
                     style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                     color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            }
        }

        // Optional Title (for replies)
        if (!reply.title.isNullOrBlank() && reply.title != stringResource(Res.string.empty_title)) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reply.title!!,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(Dimens.padding_small))

        // Content
        ThreadBody(
            content = reply.content,
            images = reply.images,
            onReferenceClick = refClick,
            onImageClick = onImageClick,
            onImageLongClick = { image -> onBookmarkImage(image) }
        )

        // Menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
             DropdownMenuItem(
                text = { Text(stringResource(Res.string.reply)) },
                leadingIcon = { Icon(Icons.Filled.Reply, null) },
                onClick = {
                    onReplyClicked(reply.id)
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.copy_content)) },
                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                onClick = {
                    onCopy()
                    showMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.bookmark_reply)) },
                leadingIcon = { Icon(Icons.Filled.BookmarkBorder, null) },
                onClick = {
                    onBookmark()
                    showMenu = false
                }
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    // A more subtle button style, pill shape or text button
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50)) // Fully rounded for modern feel
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        // Optionally hide text on very small screens or make it an icon-only button if needed
        // For now, keep text but make it small
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

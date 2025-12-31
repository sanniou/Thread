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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopicHeader(
    metadata: TopicMetadata,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Kept for compatibility if needed, but ThreadMainPost now handles most header info.
    // This could be used for a collapsed state or separate metadata view.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.padding_standard)
    ) {
        if (!metadata.title.isNullOrBlank() && metadata.title != "无标题") {
            Text(
                text = metadata.title!!,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimens.padding_small))
        }
    }
}

@Composable
fun ThreadMainPost(
    metadata: TopicMetadata,
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
        tonalElevation = 1.dp, // Subtle elevation for main post
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.padding_large), // More vertical breathing room
        ) {
            // Header: Author & Meta
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.padding_standard),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThreadAuthor(
                    author = metadata.author,
                    threadTime = metadata.createdAt.toRelativeTimeString(),
                    isPo = true,
                    onClick = onUserClick,
                    badges = {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)) {
                            if (metadata.sourceName.isNotBlank()) {
                                Badge(
                                    text = metadata.sourceName.uppercase(),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            if (metadata.isAdmin) {
                                Badge(
                                    text = "ADMIN",
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
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // ID / Floor
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "#${metadata.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.padding_medium))

            // Title (Large & Bold)
            val displayTitle = metadata.title ?: comment.title
            if (!displayTitle.isNullOrBlank() && displayTitle != "无标题") {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        bottom = Dimens.padding_medium,
                        start = Dimens.padding_standard,
                        end = Dimens.padding_standard
                    )
                )
            }

            // Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { /* No-op, allow inner clicks */ },
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

            // Action Bar (Styled)
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimens.padding_standard),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.padding_small)
                    .padding(horizontal = Dimens.padding_small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Using weight to distribute touch targets evenly
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ActionButton(
                        icon = Icons.Default.Reply,
                        text = "回复",
                        onClick = { /* TODO */ }
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ActionButton(
                        icon = Icons.Outlined.ContentCopy,
                        text = "复制",
                        onClick = onCopy
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ActionButton(
                        icon = Icons.Outlined.BookmarkBorder,
                        text = "收藏",
                        onClick = onBookmark
                    )
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    ActionButton(
                        icon = Icons.Outlined.Share,
                        text = "分享",
                        onClick = { showMenu = true }
                    )
                }
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("复制内容") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = {
                        onCopy()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("收藏串") },
                    leadingIcon = { Icon(Icons.Filled.BookmarkBorder, null) },
                    onClick = {
                        onBookmark()
                        showMenu = false
                    }
                )
                if (metadata.sourceUrl.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("打开原链接") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, null) },
                        onClick = {
                            uriHandler.openUri(metadata.sourceUrl)
                            showMenu = false
                        }
                    )
                }
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onReplyClicked(reply.id) },
                    onLongClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showMenu = true
                    }
                )
                .padding(
                    start = Dimens.padding_standard,
                    end = Dimens.padding_standard,
                    top = Dimens.padding_medium,
                    bottom = Dimens.padding_medium
                )
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThreadAuthor(
                    author = reply.author,
                    threadTime = reply.createdAt.toRelativeTimeString(),
                    isPo = isPo,
                    onClick = onUserClick,
                    badges = {
                        if (reply.isAdmin) {
                            Badge(
                                text = "ADMIN",
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Floor Indicator & ID
                Column(horizontalAlignment = Alignment.End) {
                    reply.floor?.let { floor ->
                        Text(
                            text = "#$floor",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                    Text(
                        text = "ID:${reply.id}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.padding_small))

            // Optional Title
            if (!reply.title.isNullOrBlank() && reply.title != "无标题") {
                Text(
                    text = reply.title!!,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = Dimens.padding_small)
                )
            }

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
                    text = { Text("回复") },
                    leadingIcon = { Icon(Icons.Filled.Reply, null) },
                    onClick = {
                        onReplyClicked(reply.id)
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("复制内容") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = {
                        onCopy()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("收藏回复") },
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

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.corner_radius_small))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
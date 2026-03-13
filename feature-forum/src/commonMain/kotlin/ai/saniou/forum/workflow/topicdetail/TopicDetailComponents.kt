package ai.saniou.forum.workflow.topicdetail

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.theme.Dimens
import ai.saniou.forum.ui.components.AnimatedIconButton
import ai.saniou.forum.ui.components.Badge
import ai.saniou.forum.ui.components.CollapsibleThreadBody
import ai.saniou.forum.ui.components.LikeButton
import ai.saniou.forum.ui.components.SubCommentPreview
import ai.saniou.forum.ui.components.ThreadAuthor
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.TopicMetadata
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.bookmark
import thread.feature_forum.generated.resources.bookmark_reply
import thread.feature_forum.generated.resources.bookmark_thread
import thread.feature_forum.generated.resources.copy_content
import thread.feature_forum.generated.resources.empty_title
import thread.feature_forum.generated.resources.flag_admin
import thread.feature_forum.generated.resources.open_original_link
import thread.feature_forum.generated.resources.poll_not_implemented
import thread.feature_forum.generated.resources.reply
import thread.feature_forum.generated.resources.reply_count
import thread.feature_forum.generated.resources.share
import thread.feature_forum.generated.resources.view_po_only

@Composable
fun HeroTopicCard(
    metadata: TopicMetadata,
    comment: Comment?,
    showContentPlaceholder: Boolean,
    refClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onUpvote: () -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = Dimens.padding_medium)
    ) {
        if (!metadata.title.isNullOrBlank() && metadata.title != stringResource(Res.string.empty_title)) {
            Text(
                text = metadata.title!!,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = Dimens.padding_standard,
                    end = Dimens.padding_standard,
                    top = Dimens.padding_large,
                    bottom = Dimens.padding_small
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.padding_standard),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThreadAuthor(
                author = metadata.author,
                threadTime = metadata.createdAt.toRelativeTimeString(),
                isPo = true,
                onClick = onUserClick,
                badges = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (metadata.sourceName.isNotBlank()) {
                            Badge(
                                text = metadata.sourceName.uppercase(),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        metadata.tags.forEach { tag ->
                            Badge(
                                text = tag.name,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Badge(
                            text = "楼主",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.padding_medium))

        if (comment != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = { showMenu = true })
                    .padding(horizontal = Dimens.padding_standard)
            ) {
                CollapsibleThreadBody(
                    content = comment.content,
                    images = comment.images,
                    onReferenceClick = refClick,
                    onImageClick = onImageClick,
                    onImageLongClick = onBookmarkImage
                )
            }
        } else if (showContentPlaceholder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(horizontal = Dimens.padding_standard)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(Dimens.corner_radius_medium)
                    )
            )
        }

        if (metadata.capabilities.hasPoll && metadata.poll != null) {
            Spacer(modifier = Modifier.height(Dimens.padding_medium))
            Text(
                text = stringResource(Res.string.poll_not_implemented),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = Dimens.padding_standard)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.padding_medium))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.padding_small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (metadata.capabilities.hasUpvote) {
                    LikeButton(
                        isLiked = false,
                        count = metadata.agreeCount,
                        onClick = onUpvote
                    )
                    Spacer(modifier = Modifier.width(Dimens.padding_small))
                }

                AnimatedIconButton(
                    onClick = { },
                    icon = Icons.Default.Reply,
                    contentDescription = stringResource(Res.string.reply)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedIconButton(
                    onClick = onBookmark,
                    icon = Icons.Outlined.BookmarkBorder,
                    selectedIcon = Icons.Filled.Bookmark,
                    isSelected = metadata.isCollected == true,
                    contentDescription = stringResource(Res.string.bookmark)
                )
                AnimatedIconButton(
                    onClick = { showMenu = true },
                    icon = Icons.Outlined.Share,
                    contentDescription = stringResource(Res.string.share)
                )
            }
        }

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
            if (metadata.sourceUrl.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.open_original_link)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    replyCount: String,
    isPoOnly: Boolean,
    onTogglePoOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.padding_tiny
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.padding_standard, vertical = Dimens.padding_medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.reply_count, replyCount),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            FilterChip(
                selected = isPoOnly,
                onClick = onTogglePoOnly,
                label = { Text(stringResource(Res.string.view_po_only)) },
                leadingIcon = if (isPoOnly) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                border = null,
                shape = CircleShape,
                modifier = Modifier.height(32.dp)
            )
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
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isPo = remember(reply.author.id, poUserHash) { reply.author.id == poUserHash }
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isHighlighted) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
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
                reply.floor?.let { floor ->
                    Text(
                        text = "#$floor",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "ID:${reply.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

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

        CollapsibleThreadBody(
            content = reply.content,
            images = reply.images,
            onReferenceClick = refClick,
            onImageClick = onImageClick,
            onImageLongClick = onBookmarkImage
        )

        if (reply.subCommentsPreview.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.padding_small))
            SubCommentPreview(
                subComments = reply.subCommentsPreview,
                totalCount = reply.subCommentCount,
                onViewMoreClick = { onReplyClicked(reply.id) },
                onCommentClick = {}
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.padding_small),
            horizontalArrangement = Arrangement.End
        ) {
            AnimatedIconButton(
                onClick = { onReplyClicked(reply.id) },
                icon = Icons.Default.Reply,
                contentDescription = stringResource(Res.string.reply),
                modifier = Modifier.size(32.dp)
            )
            AnimatedIconButton(
                onClick = onBookmark,
                icon = Icons.Outlined.BookmarkBorder,
                contentDescription = stringResource(Res.string.bookmark),
                modifier = Modifier.size(32.dp)
            )
        }

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

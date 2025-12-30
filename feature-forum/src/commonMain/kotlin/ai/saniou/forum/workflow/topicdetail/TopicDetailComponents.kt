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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopicHeader(
    metadata: TopicMetadata,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Dimens.padding_standard,
                end = Dimens.padding_standard,
                top = Dimens.padding_medium,
                bottom = Dimens.padding_large
            )
    ) {
        // Title
        if (!metadata.title.isNullOrBlank() && metadata.title != "无标题") {
            Text(
                text = metadata.title!!,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Dimens.padding_medium))
        }

        // Author and Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ThreadAuthor(
                author = metadata.author,
                threadTime = metadata.createdAt.toRelativeTimeString(),
                isPo = true,
                onClick = onUserClick,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)) {
                if (metadata.sourceName.isNotBlank()) {
                    Badge(
                        text = metadata.sourceName.uppercase(),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
                if (metadata.isAdmin) {
                    Badge(
                        text = "ADMIN",
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                }
                if (metadata.isSage) {
                    Badge(
                        text = "SAGE",
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MainCommentCard(
    comment: Comment,
    onRefClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onCopy: () -> Unit,
    onBookmark: () -> Unit,
    onBookmarkImage: (Image) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.padding_small), // Add slight horizontal padding for card effect
        shape = RoundedCornerShape(Dimens.corner_radius_large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(Dimens.padding_standard)
        ) {
            ThreadBody(
                content = comment.content,
                images = comment.images,
                onReferenceClick = onRefClick,
                onImageClick = onImageClick,
                onImageLongClick = onBookmarkImage
            )

            Spacer(modifier = Modifier.height(Dimens.padding_large))

            // Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(icon = Icons.Outlined.ContentCopy, text = "复制", onClick = onCopy)
                ActionButton(icon = Icons.Outlined.BookmarkBorder, text = "收藏", onClick = onBookmark)
                ActionButton(icon = Icons.Outlined.Share, text = "分享", onClick = onShare)
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
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.padding_medium, vertical = Dimens.padding_small)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.padding_tiny))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(
    comment: Comment,
    poUserHash: String,
    onReplyClick: (String) -> Unit,
    onRefClick: (Long) -> Unit,
    onImageClick: (Image) -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPo = comment.author.id == poUserHash
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onReplyClick(comment.id) },
                onLongClick = onLongClick
            )
            .padding(horizontal = Dimens.padding_standard, vertical = Dimens.padding_medium)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThreadAuthor(
                    author = comment.author,
                    threadTime = comment.createdAt.toRelativeTimeString(),
                    isPo = isPo,
                    onClick = { /* TODO: Navigate to user detail */ },
                    badges = {
                        if (comment.isAdmin) {
                            Badge(
                                text = "ADMIN",
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "#${comment.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.padding_small))

            ThreadBody(
                content = comment.content,
                images = comment.images,
                onReferenceClick = onRefClick,
                onImageClick = onImageClick,
                onImageLongClick = { /* Long click on image in reply is not handled here */ }
            )
        }
    }
}

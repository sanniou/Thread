package ai.saniou.thread.feature.social

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.composition.LocalContentLinkHandler
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.widgets.RelatedContentSection
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.toThreadUrl
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialMediaKind
import ai.saniou.thread.domain.repository.ContentGraphRepository
import ai.saniou.thread.feature.social.SocialDetailContract.Event
import ai.saniou.coreui.platform.LocalShareService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.time.Instant

data class SocialDetailPage(
    val sourceId: String,
    val postId: String,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = localDI()
        val viewModel = rememberScreenModel(tag = "$sourceId/$postId") {
            SocialDetailViewModel(
                sourceId = sourceId,
                postId = postId,
                getSocialPost = di.direct.instance(),
                interactWithSocialPost = di.direct.instance(),
                contentGraphRepository = di.direct.instance(),
            )
        }
        val state by viewModel.state.collectAsState()
        val contentGraphRepository = di.direct.instance<ContentGraphRepository>()
        val graphReference = remember(sourceId, postId) {
            ContentReference(ContentReferenceKind.SOCIAL_POST, postId, sourceId)
        }
        val related = remember(graphReference) { contentGraphRepository.getRelated(graphReference) }
            .collectAsLazyPagingItems()
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current
        val clipboard = rememberThreadClipboard()
        val shareService = LocalShareService.current
        val rootLinkHandler = LocalContentLinkHandler.current

        LaunchedEffect(state.message) {
            state.message?.let {
                snackbar.showSnackbar(it)
                viewModel.onEvent(Event.MessageShown)
            }
        }

        ThreadDetailScaffold(
            title = state.post?.author?.displayName ?: "社交动态",
            eyebrow = "SOCIAL",
            subtitle = state.post?.author?.handle ?: sourceId,
            onBack = { navigator.pop() },
            actions = {
                state.post?.let { post ->
                    val shareText = buildString {
                        append(post.author.displayName)
                        post.author.handle?.let { append(" · ").append(it) }
                        append('\n')
                        append(post.body)
                        val link = post.canonicalUrl ?: ContentReference(
                            ContentReferenceKind.SOCIAL_POST,
                            post.id,
                            post.sourceId,
                        ).toThreadUrl()
                        append('\n').append(link)
                    }
                    IconButton(onClick = {
                        val shared = shareService?.shareText(shareText, post.author.displayName) == true
                        if (!shared) clipboard.copyText(shareText)
                        scope.launch {
                            snackbar.showSnackbar(if (shared) "已通过系统分享" else "内容已复制")
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "分享")
                    }
                    post.canonicalUrl?.let { url ->
                        IconButton(onClick = { uriHandler.openUri(url) }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = "在浏览器打开")
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> {
                        val errorMessage = state.error.orEmpty()
                        ModernEmptyState(
                            icon = Icons.Default.Public,
                            title = "无法打开社交动态",
                            description = errorMessage,
                            action = {
                                TextButton(onClick = { viewModel.onEvent(Event.Retry) }) {
                                    Text("重试")
                                }
                            },
                        )
                    }
                    state.post != null -> {
                        val post = state.post!!
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                post.author.avatarUrl?.let { avatar ->
                                    NetworkImage(
                                        imageUrl = avatar,
                                        contentDescription = post.author.displayName,
                                        modifier = Modifier.size(48.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        post.author.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        listOfNotNull(post.author.handle, post.sourceId).joinToString(" · "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    Instant.fromEpochMilliseconds(post.createdAtEpochMillis).toRelativeTimeString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            post.contentWarning?.takeIf { it.isNotBlank() }?.let { warning ->
                                Text(
                                    warning,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            RichText(text = post.body)
                            post.media.filter { it.kind == SocialMediaKind.IMAGE || it.kind == SocialMediaKind.VIDEO }
                                .forEach { media ->
                                    NetworkImage(
                                        imageUrl = media.previewUrl ?: media.url,
                                        contentDescription = media.altText,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(18.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                SocialAction(
                                    interaction = SocialInteraction.REPLY,
                                    post = post,
                                    icon = Icons.Outlined.ChatBubbleOutline,
                                    onClick = { enabled ->
                                        viewModel.onEvent(Event.Interact(SocialInteraction.REPLY, enabled))
                                    },
                                )
                                SocialAction(
                                    interaction = SocialInteraction.REPOST,
                                    post = post,
                                    icon = Icons.Outlined.Repeat,
                                    onClick = { enabled ->
                                        viewModel.onEvent(Event.Interact(SocialInteraction.REPOST, enabled))
                                    },
                                )
                                SocialAction(
                                    interaction = SocialInteraction.LIKE,
                                    post = post,
                                    icon = Icons.Outlined.FavoriteBorder,
                                    onClick = { enabled ->
                                        viewModel.onEvent(Event.Interact(SocialInteraction.LIKE, enabled))
                                    },
                                )
                                SocialAction(
                                    interaction = SocialInteraction.BOOKMARK,
                                    post = post,
                                    icon = Icons.Outlined.BookmarkBorder,
                                    onClick = { enabled ->
                                        viewModel.onEvent(Event.Interact(SocialInteraction.BOOKMARK, enabled))
                                    },
                                )
                            }
                            RelatedContentSection(
                                items = related,
                                onOpen = { relatedItem ->
                                    rootLinkHandler?.invoke(relatedItem.reference.toThreadUrl())
                                },
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialAction(
    interaction: SocialInteraction,
    post: ai.saniou.thread.domain.model.social.SocialPost,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (Boolean) -> Unit,
) {
    if (interaction !in post.permittedInteractions) return
    val active = interaction in post.activeInteractions
    TextButton(onClick = { onClick(!active) }) {
        Icon(
            icon,
            contentDescription = interaction.name,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        post.interactionCounts[interaction]?.takeIf { it > 0 }?.let { count ->
            Spacer(Modifier.width(4.dp))
            Text(count.toString())
        }
    }
}

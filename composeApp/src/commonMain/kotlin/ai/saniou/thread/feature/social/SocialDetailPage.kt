package ai.saniou.thread.feature.social

import ai.saniou.corecommon.utils.toRelativeTimeString
import ai.saniou.coreui.composition.LocalContentLinkHandler
import ai.saniou.coreui.interaction.rememberThreadClipboard
import ai.saniou.coreui.widgets.NetworkImage
import ai.saniou.coreui.widgets.RelatedContentSection
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.SaniouButton
import ai.saniou.coreui.widgets.SaniouTextButton
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.content.toThreadUrl
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialMediaKind
import ai.saniou.thread.domain.repository.ContentGraphRepository
import ai.saniou.thread.feature.social.SocialDetailContract.Event
import ai.saniou.coreui.platform.LocalShareService
import ai.saniou.coreui.widgets.ThreadLoadingState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import org.jetbrains.compose.resources.stringResource
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_1795a34c15
import thread.composeapp.generated.resources.s_5eee313d53
import thread.composeapp.generated.resources.action_share
import thread.composeapp.generated.resources.s_ac12e825b5
import thread.composeapp.generated.resources.s_adacab81ba
import thread.composeapp.generated.resources.s_d4efdb587a
import thread.composeapp.generated.resources.action_retry
import thread.composeapp.generated.resources.s_f8b857f50a

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
        val shareOkMsg = stringResource(Res.string.s_1795a34c15)
        val shareFallbackMsg = stringResource(Res.string.s_adacab81ba)
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
            title = state.post?.author?.displayName ?: stringResource(Res.string.s_f8b857f50a),
            eyebrow = stringResource(Res.string.s_ac12e825b5),
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
                            snackbar.showSnackbar(if (shared) shareOkMsg else shareFallbackMsg)
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(Res.string.action_share))
                    }
                    post.canonicalUrl?.let { url ->
                        IconButton(onClick = { uriHandler.openUri(url) }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = stringResource(Res.string.s_5eee313d53))
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            val windowInfo = LocalThreadWindowInfo.current
            Box(Modifier.padding(padding).fillMaxSize()) {
                when {
                    state.isLoading -> ThreadLoadingState(modifier = Modifier.fillMaxSize())
                    state.error != null -> {
                        val errorMessage = state.error.orEmpty()
                        ModernEmptyState(
                            icon = Icons.Default.Public,
                            title = stringResource(Res.string.s_d4efdb587a),
                            description = errorMessage,
                            action = {
                                SaniouButton(
                                    onClick = { viewModel.onEvent(Event.Retry) },
                                    text = stringResource(Res.string.action_retry),
                                )
                            },
                        )
                    }
                    state.post != null -> {
                        val post = state.post!!
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    horizontal = windowInfo.pageHorizontalPadding,
                                    vertical = Dimens.padding_standard,
                                ),
                            verticalArrangement = Arrangement.spacedBy(Dimens.padding_medium),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                post.author.avatarUrl?.let { avatar ->
                                    NetworkImage(
                                        imageUrl = avatar,
                                        contentDescription = post.author.displayName,
                                        modifier = Modifier.size(48.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(Modifier.width(Dimens.padding_medium))
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.padding_tiny)) {
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
                            Spacer(Modifier.height(Dimens.padding_large))
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
    SaniouTextButton(onClick = { onClick(!active) }) {
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

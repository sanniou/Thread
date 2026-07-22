package ai.saniou.forum.workflow.search

import ai.saniou.coreui.theme.Dimens
import ai.saniou.coreui.layout.LocalThreadWindowInfo
import ai.saniou.coreui.state.PagingAppendState
import ai.saniou.coreui.state.PagingStateLayout
import ai.saniou.coreui.widgets.BlankLinePolicy
import ai.saniou.coreui.widgets.ModernEmptyState
import ai.saniou.coreui.widgets.RichText
import ai.saniou.coreui.widgets.ThreadCard
import ai.saniou.coreui.widgets.ThreadCommandBar
import ai.saniou.coreui.widgets.ThreadDetailScaffold
import ai.saniou.coreui.widgets.ThreadFilterBar
import ai.saniou.coreui.widgets.ThreadSearchField
import ai.saniou.coreui.theme.threadAnimateItem
import ai.saniou.forum.ui.components.TopicCard
import ai.saniou.forum.workflow.image.ImagePreviewPage
import ai.saniou.forum.workflow.image.ImagePreviewViewModelParams
import ai.saniou.forum.workflow.search.SearchContract.Event
import ai.saniou.forum.workflow.search.SearchContract.SearchType
import ai.saniou.forum.workflow.topic.TopicPage
import ai.saniou.forum.workflow.topicdetail.TopicDetailPage
import ai.saniou.forum.workflow.user.UserDetailPage
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.direct
import org.kodein.di.instance
import org.jetbrains.compose.resources.stringResource
import thread.feature_forum.generated.resources.Res
import thread.feature_forum.generated.resources.channel_search_empty_desc
import thread.feature_forum.generated.resources.channel_search_placeholder
import thread.feature_forum.generated.resources.channel_search_title
import thread.feature_forum.generated.resources.s_197e4877f3
import thread.feature_forum.generated.resources.s_0deb34a438
import thread.feature_forum.generated.resources.eyebrow_discovery
import thread.feature_forum.generated.resources.s_097aa90342
import thread.feature_forum.generated.resources.s_0e346065ab
import thread.feature_forum.generated.resources.s_25d6453d01
import thread.feature_forum.generated.resources.s_32c18b1aa0
import thread.feature_forum.generated.resources.s_8da9c24c50
import thread.feature_forum.generated.resources.s_a7591bfa06
import thread.feature_forum.generated.resources.s_cafc844242
import thread.feature_forum.generated.resources.s_e72e76112a
import thread.feature_forum.generated.resources.s_ff666dc5e4
import thread.feature_forum.generated.resources.search_type_channel
import thread.feature_forum.generated.resources.search_type_user
import thread.feature_forum.generated.resources.search_empty_channel_title
import thread.feature_forum.generated.resources.search_empty_channel_desc
import thread.feature_forum.generated.resources.search_empty_user_title
import thread.feature_forum.generated.resources.search_empty_user_desc

data class SearchPage(
    val sourceId: String,
    val channelId: String? = null,
    val channelName: String? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val di = org.kodein.di.compose.localDI()
        val channelScoped = !channelId.isNullOrBlank() || !channelName.isNullOrBlank()
        val vmTag = if (channelScoped) {
            "$sourceId|channel|${channelId.orEmpty()}|${channelName.orEmpty()}"
        } else {
            sourceId
        }
        val viewModel: SearchViewModel = rememberScreenModel(tag = vmTag) {
            di.direct.instance<SearchViewModelParams, SearchViewModel>(
                arg = SearchViewModelParams(
                    sourceId = sourceId,
                    channelId = channelId,
                    channelName = channelName,
                ),
            )
        }
        val state by viewModel.state.collectAsStateWithLifecycle()

        ThreadDetailScaffold(
            title = stringResource(
                if (channelScoped) Res.string.channel_search_title else Res.string.s_097aa90342,
            ),
            eyebrow = stringResource(Res.string.eyebrow_discovery),
            subtitle = if (channelScoped) {
                channelName?.takeIf { it.isNotBlank() }
                    ?: channelId.orEmpty()
            } else {
                stringResource(Res.string.s_a7591bfa06, sourceId)
            },
            onBack = navigator::pop,
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            ) {
                ThreadCommandBar(
                    modifier = Modifier.fillMaxWidth().widthIn(max = Dimens.contentMaxWidth)
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                            vertical = 16.dp,
                        ),
                    primary = {
                        ThreadSearchField(
                            query = state.query,
                            onQueryChange = { viewModel.onEvent(Event.QueryChanged(it)) },
                            onClear = { viewModel.onEvent(Event.ClearQuery) },
                            placeholder = stringResource(
                                if (channelScoped) Res.string.channel_search_placeholder
                                else Res.string.s_25d6453d01,
                            ),
                        )
                    },
                    secondary = {
                        if (!channelScoped) {
                            val searchTypeLabels = mapOf(
                                SearchType.THREAD to stringResource(Res.string.s_0deb34a438),
                                SearchType.REPLY to stringResource(Res.string.s_197e4877f3),
                                SearchType.CHANNEL to stringResource(Res.string.search_type_channel),
                                SearchType.USER to stringResource(Res.string.search_type_user),
                            )
                            ThreadFilterBar(
                                items = SearchType.entries,
                                selected = state.searchType,
                                label = { searchTypeLabels[it] ?: it.name },
                                onSelect = { viewModel.onEvent(Event.TypeChanged(it)) },
                            )
                        }
                    },
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    if (state.query.isBlank()) {
                        ModernEmptyState(
                            icon = Icons.Default.Search,
                            title = stringResource(Res.string.s_8da9c24c50),
                            description = stringResource(
                                if (channelScoped) Res.string.channel_search_empty_desc
                                else Res.string.s_0e346065ab,
                            ),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        when (state.searchType) {
                            SearchType.THREAD -> ThreadResultList(
                                viewModel = viewModel,
                                onThreadClick = { navigator.push(TopicDetailPage(it)) },
                                onImageClick = { _, img ->
                                    navigator.push(
                                        ImagePreviewPage(
                                            ImagePreviewViewModelParams(initialImages = listOf(img)),
                                        )
                                    )
                                },
                                onUserClick = { userHash ->
                                    navigator.push(UserDetailPage(sourceId, userHash))
                                },
                            )

                            SearchType.REPLY -> ReplyResultList(
                                viewModel = viewModel,
                                onThreadClick = { navigator.push(TopicDetailPage(it)) },
                                onImageClick = { _, img ->
                                    navigator.push(
                                        ImagePreviewPage(
                                            ImagePreviewViewModelParams(initialImages = listOf(img)),
                                        )
                                    )
                                },
                            )

                            SearchType.CHANNEL -> ChannelResultList(
                                viewModel = viewModel,
                                onChannelClick = { channel ->
                                    navigator.push(
                                        TopicPage(
                                            sourceId = sourceId,
                                            forumId = channel.id,
                                            fgroupId = channel.groupId,
                                        )
                                    )
                                },
                            )

                            SearchType.USER -> UserResultList(
                                viewModel = viewModel,
                                onUserClick = { author ->
                                    navigator.push(UserDetailPage(sourceId, author.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ThreadResultList(
        viewModel: SearchViewModel,
        onThreadClick: (String) -> Unit,
        onImageClick: (String, Image) -> Unit,
        onUserClick: (String) -> Unit,
    ) {
        val threads =
            viewModel.state.collectAsStateWithLifecycle().value.threadPagingData.collectAsLazyPagingItems()

        PagingStateLayout(
            items = threads,
            modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
            empty = {
                ModernEmptyState(
                    icon = Icons.Default.Search,
                    title = stringResource(Res.string.s_cafc844242),
                    description = stringResource(Res.string.s_32c18b1aa0),
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(threads.itemCount) { index ->
                    val thread = threads[index] ?: return@items
                    TopicCard(
                        topic = thread,
                        onClick = { onThreadClick(thread.id) },
                        onImageClick = { img -> onImageClick(thread.id, img) },
                        onUserClick = onUserClick,
                        modifier = threadAnimateItem(),
                    )
                }
                item { PagingAppendState(threads) }
            }
        }
    }

    @Composable
    private fun ReplyResultList(
        viewModel: SearchViewModel,
        onThreadClick: (String) -> Unit,
        onImageClick: (String, Image) -> Unit,
    ) {
        val replies =
            viewModel.state.collectAsStateWithLifecycle().value.replyPagingData.collectAsLazyPagingItems()

        PagingStateLayout(
            items = replies,
            modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
            empty = {
                ModernEmptyState(
                    icon = Icons.Default.Search,
                    title = stringResource(Res.string.s_ff666dc5e4),
                    description = stringResource(Res.string.s_e72e76112a),
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(replies.itemCount) { index ->
                    val reply = replies[index] ?: return@items
                    SearchReplyCard(
                        reply = reply,
                        onClick = { onThreadClick(reply.topicId) },
                        onImageClick = { img -> onImageClick(reply.topicId, img) },
                        modifier = threadAnimateItem(),
                    )
                }
                item { PagingAppendState(replies) }
            }
        }
    }

    @Composable
    private fun ChannelResultList(
        viewModel: SearchViewModel,
        onChannelClick: (Channel) -> Unit,
    ) {
        val channels =
            viewModel.state.collectAsStateWithLifecycle().value.channelPagingData.collectAsLazyPagingItems()

        PagingStateLayout(
            items = channels,
            modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
            empty = {
                ModernEmptyState(
                    icon = Icons.Default.Search,
                    title = stringResource(Res.string.search_empty_channel_title),
                    description = stringResource(Res.string.search_empty_channel_desc),
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(channels.itemCount) { index ->
                    val channel = channels[index] ?: return@items
                    SearchChannelCard(
                        channel = channel,
                        onClick = { onChannelClick(channel) },
                        modifier = threadAnimateItem(),
                    )
                }
                item { PagingAppendState(channels) }
            }
        }
    }

    @Composable
    private fun UserResultList(
        viewModel: SearchViewModel,
        onUserClick: (Author) -> Unit,
    ) {
        val users =
            viewModel.state.collectAsStateWithLifecycle().value.userPagingData.collectAsLazyPagingItems()

        PagingStateLayout(
            items = users,
            modifier = Modifier.fillMaxSize().widthIn(max = Dimens.contentMaxWidth),
            empty = {
                ModernEmptyState(
                    icon = Icons.Default.Search,
                    title = stringResource(Res.string.search_empty_user_title),
                    description = stringResource(Res.string.search_empty_user_desc),
                    modifier = Modifier.align(Alignment.Center),
                )
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = LocalThreadWindowInfo.current.pageHorizontalPadding,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(users.itemCount) { index ->
                    val user = users[index] ?: return@items
                    SearchUserCard(
                        author = user,
                        onClick = { onUserClick(user) },
                        modifier = threadAnimateItem(),
                    )
                }
                item { PagingAppendState(users) }
            }
        }
    }
}

@Composable
fun SearchReplyCard(
    reply: Comment,
    onClick: () -> Unit,
    onImageClick: (Image) -> Unit,
    modifier: Modifier = Modifier,
) {
    ThreadCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val authorName = reply.author.name.ifBlank { reply.author.id }
                Text(
                    text = authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "主题 ${reply.topicId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (reply.title.isNullOrBlank().not()) {
                Text(
                    text = reply.title!!,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            RichText(
                text = reply.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                blankLinePolicy = BlankLinePolicy.REMOVE
            )
        }
    }
}

@Composable
fun SearchChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThreadCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            Text(
                text = channel.displayName?.takeIf { it.isNotBlank() } ?: channel.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val subtitle = channel.descriptionText
                ?.takeIf { it.isNotBlank() }
                ?: channel.description.replace(Regex("<[^>]*>"), "").trim()
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val metrics = buildList {
                channel.topicCount?.let { add("$it topics") }
                channel.postCount?.let { add("$it posts") }
            }.joinToString(" · ")
            if (metrics.isNotBlank()) {
                Text(
                    text = metrics,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
fun SearchUserCard(
    author: Author,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ThreadCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.padding_small)
        ) {
            Text(
                text = author.name.ifBlank { author.id },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (author.id.isNotBlank() && author.id != author.name) {
                Text(
                    text = author.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

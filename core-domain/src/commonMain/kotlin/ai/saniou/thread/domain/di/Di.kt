package ai.saniou.thread.domain.di

import ai.saniou.thread.domain.usecase.bookmark.AddBookmarkUseCase
import ai.saniou.thread.domain.usecase.bookmark.GetBookmarksUseCase
import ai.saniou.thread.domain.usecase.bookmark.GetTagsUseCase
import ai.saniou.thread.domain.usecase.bookmark.IsBookmarkedUseCase
import ai.saniou.thread.domain.usecase.bookmark.RemoveBookmarkUseCase
import ai.saniou.thread.domain.usecase.channel.GetFavoriteChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelDetailUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelNameUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelsUseCase
import ai.saniou.thread.domain.usecase.channel.GetChannelTopicsPagingUseCase
import ai.saniou.thread.domain.usecase.history.AddHistoryUseCase
import ai.saniou.thread.domain.usecase.history.GetHistoryUseCase
import ai.saniou.thread.domain.usecase.misc.GetGreetImageUseCase
import ai.saniou.thread.domain.usecase.misc.GetTrendUseCase
import ai.saniou.thread.domain.usecase.notice.GetNoticeUseCase
import ai.saniou.thread.domain.usecase.notice.MarkNoticeAsReadUseCase
import ai.saniou.thread.domain.usecase.post.CreateReplyUseCase
import ai.saniou.thread.domain.usecase.post.CreateThreadUseCase
import ai.saniou.thread.domain.usecase.post.GetReferenceUseCase
import ai.saniou.thread.domain.usecase.post.ToggleFavoriteUseCase
import ai.saniou.thread.domain.usecase.settings.GetSettingsUseCase
import ai.saniou.thread.domain.usecase.settings.SaveSettingsUseCase
import ai.saniou.thread.domain.usecase.source.GetAvailableSourcesUseCase
import ai.saniou.thread.domain.usecase.subscription.GetSubscriptionFeedUseCase
import ai.saniou.thread.domain.usecase.subscription.GetActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.IsSubscribedUseCase
import ai.saniou.thread.domain.usecase.subscription.ObserveActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.SaveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.SyncLocalSubscriptionsUseCase
import ai.saniou.thread.domain.usecase.subscription.ToggleSubscriptionUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicDetailUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicImagesUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicCommentsPagingUseCase
import ai.saniou.thread.domain.usecase.thread.GetTopicCommentsUseCase
import ai.saniou.thread.domain.usecase.thread.UpdateTopicLastAccessTimeUseCase
import ai.saniou.thread.domain.usecase.thread.UpdateTopicLastReadCommentIdUseCase
import ai.saniou.thread.domain.usecase.user.AddCookieUseCase
import ai.saniou.thread.domain.usecase.user.DeleteCookieUseCase
import ai.saniou.thread.domain.usecase.user.GetUserProfileUseCase
import ai.saniou.thread.domain.usecase.reader.AddFeedSourceUseCase
import ai.saniou.thread.domain.usecase.reader.GetArticlesUseCase
import ai.saniou.thread.domain.usecase.reader.DeleteFeedSourceUseCase
import ai.saniou.thread.domain.usecase.reader.*
import ai.saniou.thread.domain.usecase.thread.GetTopicMetadataUseCase
import ai.saniou.thread.domain.usecase.user.UpdateCookieSortUseCase
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val domainModule = DI.Module("domainModule") {

    // Reader
    bindProvider { GetFeedSourcesUseCase(instance()) }
    bindProvider { GetArticleUseCase(instance()) }
    bindProvider { GetArticlesUseCase(instance()) }
    bindProvider { AddFeedSourceUseCase(instance()) }
    bindProvider { UpdateFeedSourceUseCase(instance()) }
    bindProvider { DeleteFeedSourceUseCase(instance()) }
    bindProvider { MarkArticleAsReadUseCase(instance()) }
    bindProvider { RefreshFeedSourceUseCase(instance()) }
    bindProvider { RefreshAllFeedsUseCase(instance()) }
    bindProvider { ToggleArticleBookmarkUseCase(instance()) }
    bindProvider { GetFeedSourceUseCase(instance()) }

    // Feed
    bindProvider { GetSubscriptionFeedUseCase(instance()) }

    // Forum
    bindProvider { GetAvailableSourcesUseCase(instance()) }
    bindProvider { GetChannelsUseCase(instance()) }
    bindProvider { GetFavoriteChannelsUseCase(instance()) }
    bindProvider { GetChannelTopicsPagingUseCase(instance()) }
    bindProvider { GetChannelNameUseCase(instance()) }
    bindProvider { GetChannelDetailUseCase(instance()) }

    // Thread
    bindProvider { GetTopicDetailUseCase(instance()) }
    bindProvider { GetTopicMetadataUseCase(instance()) }
    bindProvider { GetTopicCommentsUseCase(instance()) }
    bindProvider { GetTopicCommentsPagingUseCase(instance()) }
    bindProvider { GetTopicImagesUseCase(instance()) }
    bindProvider { UpdateTopicLastAccessTimeUseCase(instance(), instance()) }
    bindProvider { UpdateTopicLastReadCommentIdUseCase(instance()) }

    // Post
    bindProvider { CreateThreadUseCase(instance()) }
    bindProvider { CreateReplyUseCase(instance()) }
    bindProvider { GetReferenceUseCase(instance()) }
    bindProvider { ToggleFavoriteUseCase(instance()) }

    // Bookmark
    bindProvider { GetBookmarksUseCase(instance()) }
    bindProvider { GetTagsUseCase(instance()) }
    bindProvider { AddBookmarkUseCase(instance()) }
    bindProvider { RemoveBookmarkUseCase(instance()) }
    bindProvider { IsBookmarkedUseCase(instance()) }

    // Subscription
    bindProvider { ToggleSubscriptionUseCase(instance()) }
    bindProvider { IsSubscribedUseCase(instance()) }
    bindProvider { SyncLocalSubscriptionsUseCase(instance()) }
    bindProvider { GetActiveSubscriptionKeyUseCase(instance()) }
    bindProvider { SaveSubscriptionKeyUseCase(instance()) }
    bindProvider { ObserveActiveSubscriptionKeyUseCase(instance()) }

    // User
    bindProvider { GetUserProfileUseCase(instance()) }
    bindProvider { AddCookieUseCase(instance()) }
    bindProvider { DeleteCookieUseCase(instance()) }
    bindProvider { UpdateCookieSortUseCase(instance()) }

    // Notice
    bindProvider { GetNoticeUseCase(instance()) }
    bindProvider { MarkNoticeAsReadUseCase(instance()) }

    // History
    bindProvider { GetHistoryUseCase(instance()) }
    bindProvider { AddHistoryUseCase(instance()) }

    // Settings
    bindProvider { GetSettingsUseCase(instance()) }
    bindProvider { SaveSettingsUseCase(instance()) }

    // Misc
    bindProvider { GetTrendUseCase(instance()) }
    bindProvider { GetGreetImageUseCase(instance()) }
}

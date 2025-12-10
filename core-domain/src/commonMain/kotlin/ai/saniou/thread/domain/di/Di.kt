package ai.saniou.thread.domain.di

import ai.saniou.thread.domain.usecase.bookmark.AddBookmarkUseCase
import ai.saniou.thread.domain.usecase.bookmark.GetBookmarksUseCase
import ai.saniou.thread.domain.usecase.bookmark.IsBookmarkedUseCase
import ai.saniou.thread.domain.usecase.bookmark.RemoveBookmarkUseCase
import ai.saniou.thread.domain.usecase.forum.GetFavoriteForumsUseCase
import ai.saniou.thread.domain.usecase.forum.GetForumDetailUseCase
import ai.saniou.thread.domain.usecase.forum.GetForumNameUseCase
import ai.saniou.thread.domain.usecase.forum.GetForumsUseCase
import ai.saniou.thread.domain.usecase.forum.GetForumThreadsPagingUseCase
import ai.saniou.thread.domain.usecase.history.GetHistoryThreadsUseCase
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
import ai.saniou.thread.domain.usecase.subscription.GetSubscriptionFeedUseCase
import ai.saniou.thread.domain.usecase.subscription.GetActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.IsSubscribedUseCase
import ai.saniou.thread.domain.usecase.subscription.ObserveActiveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.SaveSubscriptionKeyUseCase
import ai.saniou.thread.domain.usecase.subscription.SyncLocalSubscriptionsUseCase
import ai.saniou.thread.domain.usecase.subscription.ToggleSubscriptionUseCase
import ai.saniou.thread.domain.usecase.thread.GetThreadDetailUseCase
import ai.saniou.thread.domain.usecase.thread.GetThreadImagesUseCase
import ai.saniou.thread.domain.usecase.thread.GetThreadRepliesPagingUseCase
import ai.saniou.thread.domain.usecase.thread.GetThreadRepliesUseCase
import ai.saniou.thread.domain.usecase.thread.UpdateThreadLastAccessTimeUseCase
import ai.saniou.thread.domain.usecase.thread.UpdateThreadLastReadReplyIdUseCase
import ai.saniou.thread.domain.usecase.user.AddCookieUseCase
import ai.saniou.thread.domain.usecase.user.DeleteCookieUseCase
import ai.saniou.thread.domain.usecase.user.GetUserProfileUseCase
import ai.saniou.thread.domain.usecase.user.UpdateCookieSortUseCase
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val domainModule = DI.Module("domainModule") {

    // Feed
    bindProvider { GetSubscriptionFeedUseCase(instance()) }

    // Forum
    bindProvider { GetForumsUseCase(instance()) }
    bindProvider { GetFavoriteForumsUseCase(instance()) }
    bindProvider { GetForumThreadsPagingUseCase(instance()) }
    bindProvider { GetForumNameUseCase(instance()) }
    bindProvider { GetForumDetailUseCase(instance()) }

    // Thread
    bindProvider { GetThreadDetailUseCase(instance()) }
    bindProvider { GetThreadRepliesUseCase(instance()) }
    bindProvider { GetThreadRepliesPagingUseCase(instance()) }
    bindProvider { GetThreadImagesUseCase(instance()) }
    bindProvider { UpdateThreadLastAccessTimeUseCase(instance()) }
    bindProvider { UpdateThreadLastReadReplyIdUseCase(instance()) }

    // Post
    bindProvider { CreateThreadUseCase(instance()) }
    bindProvider { CreateReplyUseCase(instance()) }
    bindProvider { GetReferenceUseCase(instance()) }
    bindProvider { ToggleFavoriteUseCase(instance()) }

    // Bookmark
    bindProvider { GetBookmarksUseCase(instance()) }
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
    bindProvider { GetHistoryThreadsUseCase(instance()) }

    // Settings
    bindProvider { GetSettingsUseCase(instance()) }
    bindProvider { SaveSettingsUseCase(instance()) }

    // Misc
    bindProvider { GetTrendUseCase(instance()) }
    bindProvider { GetGreetImageUseCase(instance()) }
}

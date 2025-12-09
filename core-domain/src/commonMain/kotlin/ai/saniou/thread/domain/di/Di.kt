package ai.saniou.thread.domain.di

import ai.saniou.thread.domain.usecase.AddBookmarkUseCase
import ai.saniou.thread.domain.usecase.GetBookmarksUseCase
import ai.saniou.thread.domain.usecase.GetForumDetailUseCase
import ai.saniou.thread.domain.usecase.GetForumNameUseCase
import ai.saniou.thread.domain.usecase.GetForumsUseCase
import ai.saniou.thread.domain.usecase.GetForumThreadsPagingUseCase
import ai.saniou.thread.domain.usecase.GetHistoryThreadsUseCase
import ai.saniou.thread.domain.usecase.GetNmbForumPageUseCase
import ai.saniou.thread.domain.usecase.GetNoticeUseCase
import ai.saniou.thread.domain.usecase.GetPostsUseCase
import ai.saniou.thread.domain.usecase.GetReferenceUseCase
import ai.saniou.thread.domain.usecase.GetSubscriptionFeedUseCase
import ai.saniou.thread.domain.usecase.GetThreadDetailUseCase
import ai.saniou.thread.domain.usecase.GetThreadImagesUseCase
import ai.saniou.thread.domain.usecase.GetThreadRepliesPagingUseCase
import ai.saniou.thread.domain.usecase.GetTrendUseCase
import ai.saniou.thread.domain.usecase.IsBookmarkedUseCase
import ai.saniou.thread.domain.usecase.IsSubscribedUseCase
import ai.saniou.thread.domain.usecase.MarkNoticeAsReadUseCase
import ai.saniou.thread.domain.usecase.PostUseCase
import ai.saniou.thread.domain.usecase.RemoveBookmarkUseCase
import ai.saniou.thread.domain.usecase.SyncLocalSubscriptionsUseCase
import ai.saniou.thread.domain.usecase.ToggleFavoriteUseCase
import ai.saniou.thread.domain.usecase.ToggleSubscriptionUseCase
import ai.saniou.thread.domain.usecase.UserUseCase
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val domainModule = DI.Module("domainModule") {
    bindProvider { GetForumsUseCase(instance()) }
    bindProvider { GetPostsUseCase(instance()) }
    bindProvider { GetNmbForumPageUseCase(instance(), instance()) }
    bindProvider { ToggleFavoriteUseCase(instance()) }

    // Forum UseCases
    bindProvider { GetForumThreadsPagingUseCase(instance()) }
    bindProvider { GetForumNameUseCase(instance()) }
    bindProvider { GetForumDetailUseCase(instance()) }

    // Bookmark UseCases
    bindProvider { GetBookmarksUseCase(instance()) }
    bindProvider { AddBookmarkUseCase(instance()) }
    bindProvider { RemoveBookmarkUseCase(instance()) }
    bindProvider { IsBookmarkedUseCase(instance()) }

    // Subscription UseCases
    bindProvider { GetSubscriptionFeedUseCase(instance()) }
    bindProvider { ToggleSubscriptionUseCase(instance()) }
    bindProvider { IsSubscribedUseCase(instance()) }
    bindProvider { SyncLocalSubscriptionsUseCase(instance()) }

    // User UseCases
    bindProvider { UserUseCase(instance()) }

    // Notice UseCases
    bindProvider { GetNoticeUseCase(instance()) }
    bindProvider { MarkNoticeAsReadUseCase(instance()) }

    // History UseCases
    bindProvider { GetHistoryThreadsUseCase(instance()) }

    // Post UseCases
    bindProvider { PostUseCase(instance()) }

    // Trend UseCases
    bindProvider { GetTrendUseCase(instance()) }

    // Reference UseCases
    bindProvider { GetReferenceUseCase(instance()) }

    // Thread UseCases
    bindProvider { GetThreadDetailUseCase(instance()) }
    bindProvider { GetThreadRepliesPagingUseCase(instance()) }
    bindProvider { GetThreadImagesUseCase(instance()) }
}
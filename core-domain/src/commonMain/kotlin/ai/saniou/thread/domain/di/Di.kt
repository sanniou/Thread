package ai.saniou.thread.domain.di

import ai.saniou.thread.domain.usecase.AddBookmarkUseCase
import ai.saniou.thread.domain.usecase.GetBookmarksUseCase
import ai.saniou.thread.domain.usecase.GetForumsUseCase
import ai.saniou.thread.domain.usecase.GetNmbForumPageUseCase
import ai.saniou.thread.domain.usecase.GetPostsUseCase
import ai.saniou.thread.domain.usecase.IsBookmarkedUseCase
import ai.saniou.thread.domain.usecase.RemoveBookmarkUseCase
import ai.saniou.thread.domain.usecase.ToggleFavoriteUseCase
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val domainModule = DI.Module("domainModule") {
    bindProvider { GetForumsUseCase(instance()) }
    bindProvider { GetPostsUseCase(instance()) }
    bindProvider { GetNmbForumPageUseCase(instance(), instance()) }
    bindProvider { ToggleFavoriteUseCase(instance()) }

    // Bookmark UseCases
    bindProvider { GetBookmarksUseCase(instance()) }
    bindProvider { AddBookmarkUseCase(instance()) }
    bindProvider { RemoveBookmarkUseCase(instance()) }
    bindProvider { IsBookmarkedUseCase(instance()) }
}
package ai.saniou.nmb.di

import ai.saniou.thread.network.CookieProvider
import ai.saniou.nmb.data.NmbCookieProvider
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.api._NmbXdApiImpl
import ai.saniou.nmb.data.database.DriverFactory
import ai.saniou.nmb.data.database.createDatabase
import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.data.repository.BookmarkRepository
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.repository.HistoryRepository
import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.data.repository.NmbRepositoryImpl
import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.data.storage.CommonStorage
import ai.saniou.nmb.data.storage.GreetImageStorage
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.domain.AddBookmarkUseCase
import ai.saniou.nmb.domain.ForumCategoryUseCase
import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.domain.GetBookmarksUseCase
import ai.saniou.nmb.domain.GetReferenceUseCase
import ai.saniou.nmb.domain.GetThreadDetailUseCase
import ai.saniou.nmb.domain.GetThreadImagesUseCase
import ai.saniou.nmb.domain.GetThreadRepliesPagingUseCase
import ai.saniou.nmb.domain.HistoryUseCase
import ai.saniou.nmb.domain.IsBookmarkedUseCase
import ai.saniou.nmb.domain.NoticeUseCase
import ai.saniou.nmb.domain.PostUseCase
import ai.saniou.nmb.domain.RemoveBookmarkUseCase
import ai.saniou.nmb.domain.SubscriptionFeedUseCase
import ai.saniou.nmb.domain.ToggleSubscriptionUseCase
import ai.saniou.nmb.domain.TrendUseCase
import ai.saniou.nmb.domain.UserUseCase
import ai.saniou.nmb.initializer.AppInitializer
import ai.saniou.nmb.workflow.bookmark.BookmarkViewModel
import ai.saniou.nmb.workflow.forum.ForumViewModel
import ai.saniou.nmb.workflow.history.HistoryViewModel
import ai.saniou.nmb.workflow.home.ForumCategoryViewModel
import ai.saniou.nmb.workflow.home.GreetImageViewModel
import ai.saniou.nmb.workflow.home.HomeViewModel
import ai.saniou.nmb.workflow.image.nmbImagePreviewModule
import ai.saniou.nmb.workflow.post.PostViewModel
import ai.saniou.nmb.workflow.reference.ReferenceViewModel
import ai.saniou.nmb.workflow.search.SearchViewModel
import ai.saniou.nmb.workflow.subscription.SubscriptionViewModel
import ai.saniou.nmb.workflow.thread.ThreadViewModel
import ai.saniou.nmb.workflow.trend.TrendViewModel
import ai.saniou.nmb.workflow.user.UserDetailViewModel
import ai.saniou.nmb.workflow.user.UserViewModel
import ai.saniou.thread.network.SaniouKtorfit
import de.jensklingenberg.ktorfit.Ktorfit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bindConstant
import org.kodein.di.bindFactory
import org.kodein.di.bindMultiton
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.kodein.di.instanceOrNull

val coreCommon by DI.Module {

    bindMultiton<String, Ktorfit> { baseUrl ->
        SaniouKtorfit(baseUrl, instanceOrNull())
    }

//    bindSingleton<Ktorfit> {
//        ktorfit(this.instance<String>("baseUrl"))
//    }

}
val nmbdi = DI {
    import(coreCommon)
    import(nmbImagePreviewModule)

    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }

    bindSingleton<CookieProvider> { NmbCookieProvider(instance()) }

    bindSingleton<NmbXdApi> { _NmbXdApiImpl(instance(arg = instance<String>("nmbBaseUrl"))) }

    bindSingleton<ForumRepository> { ForumRepository(instance()) }

    // NMB 仓库
    bindSingleton<NmbRepository> { NmbRepositoryImpl(instance(), instance()) }
    bindSingleton<HistoryRepository> { NmbRepositoryImpl(instance(), instance()) }
    bindSingleton<BookmarkRepository> { BookmarkRepository(instance()) }

    // CDN管理器
    bindSingleton<CdnManager> { CdnManager(instance()) }

    // 应用初始化器
    bindSingleton<AppInitializer> { AppInitializer(instance()) }


    // 数据存储相关
    bindSingleton {
        CategoryStorage(
            scope = CoroutineScope(Dispatchers.Default)
        )
    }

    // 欢迎图片存储
    bindSingleton {
        GreetImageStorage(
            scope = CoroutineScope(Dispatchers.Default)
        )
    }

    // 订阅存储
    bindSingleton {
        SubscriptionStorage(
            scope = CoroutineScope(Dispatchers.Default)
        )
    }

    // 欢迎图片ViewModel
    bindProvider<GreetImageViewModel> { GreetImageViewModel(instance()) }

    // 论坛分类相关
    bindProvider<ForumCategoryUseCase> { ForumCategoryUseCase(instance(), instance()) }
    bindProvider<ForumCategoryViewModel> {
        ForumCategoryViewModel(
            instance(),
            instance(),
            instance()
        )
    }

    // 论坛相关
    bindProvider { ForumUseCase(instance(), instance()) }
    bindFactory<Pair<Long, Long>, ForumViewModel> { params ->
        ForumViewModel(instance(), params.first, params.second)
    }

    // 帖子相关
    bindProvider { GetThreadDetailUseCase(instance(), instance()) }
    bindProvider { GetThreadRepliesPagingUseCase(instance(), instance()) }
    bindProvider { GetReferenceUseCase(instance(), instance()) }
    bindProvider { GetThreadImagesUseCase(instance()) }
    bindFactory<Long, ThreadViewModel> { threadId ->
        ThreadViewModel(
            threadId = threadId,
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance()
        )
    }
    bindProvider { ToggleSubscriptionUseCase(instance(), instance(), instance()) }

    // 发帖和回复相关
    bindProvider { PostUseCase(instance()) }
    bindFactory<Triple<Int?, Int?, String?>, PostViewModel> { params ->
        PostViewModel(instance(), instance(), params.first, params.second, params.third)
    }

    // 用户认证相关
    bindProvider { UserUseCase(instance()) }
    bindProvider { UserViewModel(instance()) }

    // 引用 ViewModel
    bindProvider { ReferenceViewModel(instance()) }

    // 订阅相关
    bindProvider { SubscriptionViewModel(instance(), instance()) }

    // 历史相关
    bindProvider { HistoryUseCase(instance()) }
    bindProvider { HistoryViewModel(instance()) }

    // 搜索相关
    bindProvider { SearchViewModel(instance()) }
    bindFactory<String, UserDetailViewModel> { userHash ->
        UserDetailViewModel(
            userHash = userHash,
            instance()
        )
    }

    // 收藏相关
    bindProvider { GetBookmarksUseCase(instance()) }
    bindProvider { AddBookmarkUseCase(instance()) }
    bindProvider { RemoveBookmarkUseCase(instance()) }
    bindProvider { IsBookmarkedUseCase(instance()) }
    bindProvider { BookmarkViewModel(instance(), instance()) }

    bindSingleton {
        createDatabase(DriverFactory())
    }

    bindProvider { SubscriptionFeedUseCase(instance(), instance()) }

    bindProvider { NoticeUseCase(instance(), instance(), instance()) }
    bindProvider { HomeViewModel(instance()) }
    bindProvider { TrendUseCase(instance()) }
    bindProvider { TrendViewModel(instance()) }
    bindSingleton {
        CommonStorage(scope = CoroutineScope(Dispatchers.Default))
    }
}

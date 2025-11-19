package ai.saniou.nmb.di

import ai.saniou.corecommon.data.di.coreCommon
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.api._NmbXdApiImpl
import ai.saniou.nmb.data.database.DriverFactory
import ai.saniou.nmb.data.database.createDatabase
import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.repository.HistoryRepository
import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.data.repository.NmbRepositoryImpl
import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.data.storage.CommonStorage
import ai.saniou.nmb.data.storage.GreetImageStorage
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.domain.ForumCategoryUseCase
import ai.saniou.nmb.domain.ForumUseCase
import ai.saniou.nmb.domain.GetReferenceUseCase
import ai.saniou.nmb.domain.GetThreadDetailUseCase
import ai.saniou.nmb.domain.GetThreadRepliesPagingUseCase
import ai.saniou.nmb.domain.HistoryUseCase
import ai.saniou.nmb.domain.NoticeUseCase
import ai.saniou.nmb.domain.PostUseCase
import ai.saniou.nmb.domain.SubscriptionFeedUseCase
import ai.saniou.nmb.domain.ToggleSubscriptionUseCase
import ai.saniou.nmb.domain.UserUseCase
import ai.saniou.nmb.initializer.AppInitializer
import ai.saniou.nmb.workflow.forum.ForumViewModel
import ai.saniou.nmb.workflow.history.HistoryViewModel
import ai.saniou.nmb.workflow.home.ForumCategoryViewModel
import ai.saniou.nmb.workflow.home.GreetImageViewModel
import ai.saniou.nmb.workflow.home.HomeViewModel
import ai.saniou.nmb.workflow.post.PostViewModel
import ai.saniou.nmb.workflow.reference.ReferenceViewModel
import ai.saniou.nmb.workflow.subscription.SubscriptionViewModel
import ai.saniou.nmb.workflow.thread.ThreadViewModel
import ai.saniou.nmb.workflow.user.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bindConstant
import org.kodein.di.bindFactory
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance


val nmbdi = DI {
    import(coreCommon)

    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }

    bindSingleton<NmbXdApi> { _NmbXdApiImpl(instance(arg = instance<String>("nmbBaseUrl"))) }

    bindSingleton<ForumRepository> { ForumRepository(instance()) }

    // NMB 仓库
    bindSingleton<NmbRepository> { NmbRepositoryImpl(instance(), instance()) }
    bindSingleton<HistoryRepository> { NmbRepositoryImpl(instance(), instance()) }

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
    bindProvider { GetThreadDetailUseCase(instance()) }
    bindProvider { GetThreadRepliesPagingUseCase(instance(), instance()) }
    bindProvider { GetReferenceUseCase(instance(), instance()) }
    bindFactory<Long, ThreadViewModel> { threadId ->
        ThreadViewModel(
            threadId = threadId,
            getThreadDetailUseCase = instance(),
            getThreadRepliesPagingUseCase = instance(),
            forumUseCase = instance(),
            nmbRepository = instance(),
            toggleSubscriptionUseCase = instance(),
            db = instance(),
            forumRepository = instance()
        )
    }
    bindProvider { ToggleSubscriptionUseCase(instance(), instance()) }

    // 发帖和回复相关
    bindProvider { PostUseCase(instance()) }
    bindProvider { PostViewModel(instance()) }

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

    bindSingleton {
        createDatabase(DriverFactory())
    }

    bindProvider { SubscriptionFeedUseCase(instance(), instance()) }

    bindProvider { NoticeUseCase(instance(), instance(), instance()) }
    bindProvider { HomeViewModel(instance()) }
    bindSingleton {
        CommonStorage(scope = CoroutineScope(Dispatchers.Default))
    }
}



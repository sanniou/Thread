package ai.saniou.nmb.di

import ai.saniou.corecommon.data.di.coreCommon
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.api._NmbXdApiImpl
import ai.saniou.nmb.data.manager.CdnManager
import ai.saniou.nmb.data.repository.ForumRepository
import ai.saniou.nmb.data.repository.NmbRepository
import ai.saniou.nmb.data.repository.NmbRepositoryImpl
import ai.saniou.nmb.data.source.ForumPagingSource
import ai.saniou.nmb.data.storage.CategoryStorage
import ai.saniou.nmb.data.storage.GreetImageStorage
import ai.saniou.nmb.data.storage.SubscriptionStorage
import ai.saniou.nmb.data.usecase.ReferenceUseCase
import ai.saniou.nmb.domain.ForumCategoryUserCase
import ai.saniou.nmb.domain.ForumUserCase
import ai.saniou.nmb.domain.PostUseCase
import ai.saniou.nmb.domain.ThreadUseCase
import ai.saniou.nmb.domain.UserUseCase
import ai.saniou.nmb.initializer.AppInitializer
import ai.saniou.nmb.workflow.forum.ForumViewModel
import ai.saniou.nmb.workflow.home.ForumCategoryViewModel
import ai.saniou.nmb.workflow.home.GreetImageViewModel
import ai.saniou.nmb.workflow.image.ImagePreviewViewModel
import ai.saniou.nmb.workflow.post.PostViewModel
import ai.saniou.nmb.workflow.reference.ReferenceViewModel
import ai.saniou.nmb.workflow.subscription.SubscriptionViewModel
import ai.saniou.nmb.workflow.thread.ThreadViewModel
import ai.saniou.nmb.workflow.user.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bindConstant
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance


val nmbdi = DI {
    import(coreCommon)

    bindConstant<String>(tag = "nmbBaseUrl") { "https://api.nmb.best/api/" }

    bindSingleton<NmbXdApi> { _NmbXdApiImpl(instance(arg = instance<String>("nmbBaseUrl"))) }

    bindSingleton<ForumRepository> { ForumRepository(instance()) }

    // NMB 仓库
    bindSingleton<NmbRepository> { NmbRepositoryImpl(instance()) }

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
    bindProvider<ForumCategoryUserCase> { ForumCategoryUserCase(instance()) }
    bindProvider<ForumCategoryViewModel> {
        ForumCategoryViewModel(
            instance(),
            instance(),
            instance()
        )
    }

    // 论坛相关
    bindProvider { ForumPagingSource(instance()) }
    bindProvider { ForumUserCase(instance()) }
    bindProvider { ForumViewModel(instance()) }

    // 帖子相关
    bindProvider { ThreadUseCase(instance()) }
    bindProvider { ThreadViewModel(instance(), instance(), instance()) }

    // 发帖和回复相关
    bindProvider { PostUseCase(instance()) }
    bindProvider { PostViewModel(instance()) }

    // 用户认证相关
    bindProvider { UserUseCase(instance()) }
    bindProvider { UserViewModel(instance()) }

    // 图片预览相关
    bindProvider { ImagePreviewViewModel(di) }

    // 引用 UseCase
    bindProvider<ReferenceUseCase> { ReferenceUseCase(instance()) }
    // 引用 ViewModel
    bindProvider<ReferenceViewModel> { ReferenceViewModel(instance()) }

    // 订阅相关
    bindProvider<SubscriptionViewModel> { SubscriptionViewModel(instance(), instance()) }
}



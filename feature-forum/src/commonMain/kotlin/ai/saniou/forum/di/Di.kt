package ai.saniou.forum.di

import ai.saniou.forum.initializer.AppInitializer
import ai.saniou.forum.workflow.topic.TopicViewModel
import ai.saniou.forum.workflow.home.ChannelViewModel
import ai.saniou.forum.workflow.home.GreetImageViewModel
import ai.saniou.forum.workflow.image.nmbImagePreviewModule
import ai.saniou.forum.workflow.init.SourceInitViewModel
import ai.saniou.forum.workflow.login.TiebaLoginViewModel
import ai.saniou.forum.workflow.post.PostViewModel
import ai.saniou.forum.workflow.reference.ReferenceViewModel
import ai.saniou.forum.workflow.search.SearchViewModel
import ai.saniou.forum.workflow.subscription.SubscriptionViewModel
import ai.saniou.forum.workflow.topicdetail.TopicDetailViewModel
import ai.saniou.forum.workflow.topicdetail.TopicDetailViewModelParams
import ai.saniou.forum.workflow.trend.TrendViewModel
import ai.saniou.forum.workflow.user.UserDetailViewModel
import ai.saniou.forum.workflow.user.UserViewModel
import ai.saniou.thread.data.di.dataModule
import ai.saniou.thread.domain.di.domainModule
import ai.saniou.thread.network.SaniouKtorfit
import de.jensklingenberg.ktorfit.Ktorfit
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindMultiton
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val coreCommon by DI.Module {

    bindMultiton<String, Ktorfit> { baseUrl ->
        SaniouKtorfit(baseUrl)
    }

//    bindSingleton<Ktorfit> {
//        ktorfit(this.instance<String>("baseUrl"))
//    }

}

/**
 * NMB 功能模块的 DI 定义，只负责 UI (ViewModel) 层的依赖注入。
 * 所有业务逻辑和数据层的依赖都通过构造函数从外部传入。
 */
val nmbFeatureModule = DI.Module("nmbFeatureModule") {

    // 应用初始化器
    bindSingleton<AppInitializer> { AppInitializer(instance()) }

    // 欢迎图片ViewModel
    bindProvider<GreetImageViewModel> { GreetImageViewModel(instance()) }

    // 论坛分类相关
    bindProvider<ChannelViewModel> {
        ChannelViewModel(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance()
        )
    }

    // 论坛相关
    bindFactory<Pair<Long, Long>, TopicViewModel> { params ->
        // Compatibility for old factory
        TopicViewModel(
            instance(),
            instance(),
            instance(),
            "nmb",
            params.first.toString(),
            params.second.toString()
        )
    }

    // New factory supporting SourceId
    bindFactory<Triple<String, String, String>, TopicViewModel> { params ->
        TopicViewModel(
            instance(),
            instance(),
            instance(),
            sourceId = params.first,
            channelId = params.second,
            channelCategoryId = params.third
        )
    }

    // 帖子相关
    bindFactory<TopicDetailViewModelParams, TopicDetailViewModel> { params ->
        TopicDetailViewModel(
            params = params,
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance()
        )
    }


    // 发帖和回复相关
    bindFactory<Triple<Int?, Int?, String?>, PostViewModel> { params ->
        PostViewModel(instance(), instance(), params.first, params.second, params.third)
    }

    // 用户认证相关
    bindProvider {
        UserViewModel(
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance()
        )
    }

    // 引用 ViewModel
    bindProvider { ReferenceViewModel(instance()) }

    // 订阅相关
    bindProvider {
        SubscriptionViewModel(
            getSubscriptionFeedUseCase = instance(),
            toggleSubscriptionUseCase = instance(),
            syncLocalSubscriptionsUseCase = instance(),
            observeActiveSubscriptionKeyUseCase = instance(),
            saveSubscriptionKeyUseCase = instance(),
            generateRandomSubscriptionIdUseCase = instance()
        )
    }
    // 搜索相关
    bindProvider { SearchViewModel(instance()) }
    bindFactory<String, UserDetailViewModel> { userHash ->
        UserDetailViewModel(
            userHash = userHash,
            nmbRepository = instance()
        )
    }

    bindFactory<String, TrendViewModel> { sourceId ->
        TrendViewModel(
            initialSourceId = sourceId,
            getTrendUseCase = instance(),
            settingsRepository = instance(),
            sourceRepository = instance(),
            getChannelTopicsPagingUseCase = instance()
        )
    }

    bindFactory<String, SourceInitViewModel> { sourceId ->
        SourceInitViewModel(
            sourceId = sourceId,
            settingsRepository = instance(),
            subscriptionRepository = instance(),
            nmbSource = instance()
        )
    }

    // Tieba Login
    bindProvider { TiebaLoginViewModel(instance()) }
}

val nmbdi = DI {
    import(coreCommon)
    import(domainModule)
    import(dataModule)
    import(nmbImagePreviewModule)
    import(nmbFeatureModule)
}

package ai.saniou.forum.di

import ai.saniou.forum.initializer.AppInitializer
import ai.saniou.forum.workflow.topic.TopicViewModel
import ai.saniou.forum.workflow.home.ChannelViewModel
import ai.saniou.forum.workflow.init.SourceInitViewModel
import ai.saniou.forum.workflow.post.PostViewModel
import ai.saniou.forum.workflow.post.PostViewModelParams
import ai.saniou.forum.workflow.reference.ReferenceViewModel
import ai.saniou.forum.workflow.search.SearchViewModel
import ai.saniou.forum.workflow.subscription.SubscriptionViewModel
import ai.saniou.forum.workflow.topicdetail.TopicDetailViewModel
import ai.saniou.forum.workflow.topicdetail.TopicDetailViewModelParams
import ai.saniou.forum.workflow.trend.TrendViewModel
import ai.saniou.thread.domain.usecase.post.SubmitNotInterestedUseCase
import ai.saniou.forum.workflow.user.UserDetailViewModel
import ai.saniou.forum.workflow.user.UserViewModel
import ai.saniou.forum.workflow.source.SourceManagerViewModel
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance

/**
 * Forum 功能模块的 DI 定义，只负责 UI (ViewModel) 层的依赖注入。
 * 所有业务逻辑和数据层的依赖都通过构造函数从外部传入。
 */
val forumFeatureModule = DI.Module("forumFeatureModule") {

    // 应用初始化器
    bindSingleton<AppInitializer> { AppInitializer(instance()) }

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
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
            instance(),
        )
    }

    bindProvider {
        SourceManagerViewModel(
            instance(),
            instance(),
            instance(),
            instance(),
        )
    }

    // Source-aware forum topic factory.
    bindFactory<Triple<String, String, String>, TopicViewModel> { params ->
        TopicViewModel(
            instance(),
            instance(),
            instance(),
            instance(),
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
            instance(),
            instance(),
            instance(),
            instance()
        )
    }


    // 发帖和回复相关
    bindFactory<PostViewModelParams, PostViewModel> { params ->
        PostViewModel(instance(), instance(), instance(), instance(), instance(), params)
    }

    // 用户认证相关
    bindProvider {
        UserViewModel(
            instance(),
            instance(),
            instance(),
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
    bindFactory<String, SearchViewModel> { sourceId -> SearchViewModel(sourceId, instance()) }
    bindFactory<Pair<String, String>, UserDetailViewModel> { params ->
        UserDetailViewModel(
            sourceId = params.first,
            userHash = params.second,
            userContentRepository = instance(),
            getUserRelationProfileUseCase = instance(),
            followUserUseCase = instance(),
            unfollowUserUseCase = instance(),
            sourceRepository = instance(),
        )
    }

    bindFactory<String, TrendViewModel> { sourceId ->
        TrendViewModel(
            initialSourceId = sourceId,
            trendRepository = instance(),
            submitNotInterestedUseCase = instance(),
        )
    }

    bindFactory<String, SourceInitViewModel> { sourceId ->
        SourceInitViewModel(
            sourceId = sourceId,
            sourceRepository = instance(),
            settingsRepository = instance(),
            subscriptionRepository = instance(),
        )
    }
}

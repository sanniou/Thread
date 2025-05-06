package ai.saniou.nmb.workflow.subscription

import ai.saniou.nmb.data.NmbScreen
import ai.saniou.nmb.workflow.home.NavigationDestination

/**
 * 订阅列表页面导航目标
 */
object SubscriptionNavigationDestination : NavigationDestination {
    override val route = NmbScreen.Subscription.name
}

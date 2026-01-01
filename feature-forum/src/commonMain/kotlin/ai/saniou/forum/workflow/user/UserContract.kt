package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.model.forum.Account

/**
 * 定义用户中心页面的状态、事件和副作用
 */
interface UserContract {
    /**
     * UI 状态
     * @param isLoading 是否正在加载
     * @param cookies 饼干列表
     * @param error 错误信息
     */
    data class State(
        val isLoading: Boolean = true,
        val cookies: List<Account> = emptyList(),
        val error: String? = null,
    )

    /**
     * UI 事件
     */
    sealed interface Event {
        /**
         * 重新加载饼干
         */
        data object LoadCookies : Event

        /**
         * 添加一个饼干
         * @param name 别名
         * @param value 饼干值
         */
        data class AddCookie(val name: String, val value: String) : Event

        /**
         * 删除一个饼干
         * @param account 要删除的饼干
         */
        data class DeleteCookie(val account: Account) : Event

        /**
         * 更新饼干排序
         * @param accounts 排序后的饼干列表
         */
        data class UpdateCookieOrder(val accounts: List<Account>) : Event
    }

    /**
     * 副作用，用于处理单次事件，如显示 Toast
     */
    sealed interface Effect {
        /**
         * 显示错误信息
         * @param message 错误信息
         */
        data class ShowError(val message: String) : Effect
    }
}

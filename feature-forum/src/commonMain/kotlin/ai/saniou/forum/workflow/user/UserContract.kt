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
        val sourceId: String = "",
        val loginStrategy: ai.saniou.thread.domain.model.user.LoginStrategy? = null,
    )

    /**
     * UI 事件
     */
    sealed interface Event {
        /**
         * 加载数据
         */
        data class LoadData(val sourceId: String) : Event

        /**
         * 添加一个账号
         * @param inputs 输入参数 (key-value map)
         */
        data class AddAccount(val inputs: Map<String, String>) : Event

        /**
         * 删除一个账号
         * @param account 要删除的账号
         */
        data class DeleteAccount(val account: Account) : Event

        /**
         * 更新账号排序
         * @param accounts 排序后的账号列表
         */
        data class UpdateAccountOrder(val accounts: List<Account>) : Event
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

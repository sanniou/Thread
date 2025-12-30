package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Topic
import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow

/**
 * 板块相关的仓库接口，定义了板块数据的契约
 */
interface ChannelRepository {

    /**
     * 从指定的信息源获取板块列表
     * @param sourceId 信息源ID
     */
    fun getChannels(sourceId: String): Flow<List<Channel>>

    /**
     * 刷新板块列表
     * @param sourceId 信息源ID
     */
    suspend fun fetchChannels(sourceId: String): Result<Unit>

    /**
     * 获取板块下的帖子分页数据
     *
     * @param fid 板块ID
     * @param isTimeline 是否为时间线模式
     * @param initialPage 初始页码
     * @return 包含帖子分页数据的 Flow
     */
    fun getChannelTopicsPaging(
        sourceId: String,
        fid: String,
        isTimeline: Boolean,
        initialPage: Int = 1
    ): Flow<PagingData<Topic>>

    /**
     * 获取板块名称
     *
     * @param sourceId 来源ID
     * @param fid 板块ID
     * @return 包含板块名称的 Flow, 如果找不到则为 null
     */
    fun getChannelName(sourceId: String, fid: String): Flow<String?>

    /**
     * 获取板块详情
     *
     * @param sourceId 来源ID
     * @param fid 板块ID
     * @return 包含板块详情的 Flow
     */
    fun getChannelDetail(sourceId: String, fid: String): Flow<Channel?>

    /**
     * 保存最后打开的板块
     * @param forum 要保存的板块，如果为null则清除记录
     */
    suspend fun saveLastOpenedChannel(forum: Channel?)

    /**
     * 获取最后打开的板块
     * @return 最后打开的板块，如果不存在则为null
     */
    suspend fun getLastOpenedChannel(): Channel?
}

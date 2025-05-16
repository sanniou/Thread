package ai.saniou.nmb.data.repository

import ai.saniou.corecommon.data.SaniouResponse
import ai.saniou.nmb.data.api.NmbXdApi
import ai.saniou.nmb.data.entity.Reply

/**
 * NMB 仓库实现类
 */
class NmbRepositoryImpl(
    private val nmbXdApi: NmbXdApi
) : NmbRepository {

    /**
     * 获取引用的回复内容
     */
    override suspend fun getReference(refId: Long): Reply? {
        return try {
            val response = nmbXdApi.ref(refId)
            if (response is SaniouResponse.Success) {
                val reference = response.data
                // 将 NmbReference 转换为 Reply
                Reply(
                    id = reference.id,
                    fid = 0, // 引用 API 不返回 fid
                    replyCount = 0, // 引用 API 不返回回复数量
                    img = reference.img,
                    ext = reference.ext,
                    now = reference.now,
                    userHash = reference.userHash,
                    name = reference.name,
                    title = reference.title,
                    content = reference.content,
                    sage = reference.sage,
                    admin = 0, // 引用 API 不返回 admin
                    hide = reference.hide
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

package ai.saniou.nmb.data.repository

import ai.saniou.nmb.data.entity.Reply

/**
 * NMB 仓库接口
 */
interface NmbRepository {

   /**
    * 获取引用的回复内容
    */
   suspend fun getReference(refId: Long): Reply?

   /**
    * 更新帖子的最后访问时间
    */
   suspend fun updateThreadLastAccessTime(threadId: Long, time: Long)

   /**
    * 更新帖子的最后阅读回复ID
    */
   suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: Long)
}

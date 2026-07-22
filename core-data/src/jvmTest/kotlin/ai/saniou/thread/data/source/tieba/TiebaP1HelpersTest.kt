package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.MessageListBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TiebaP1HelpersTest {
    @Test
    fun parseTopicBookmarkId_extractsThreadId() {
        assertEquals("12345", parseTiebaTopicBookmarkId("tieba.Topic.12345"))
        assertNull(parseTiebaTopicBookmarkId("tieba.Comment.9"))
        assertNull(parseTiebaTopicBookmarkId("nmb.Topic.1"))
        assertNull(parseTiebaTopicBookmarkId("tieba.Topic."))
    }

    @Test
    fun messageListEnsureOk_throwsOnErrorCode() {
        assertFailsWith<IllegalStateException> {
            MessageListBean(errorCode = "110001").ensureOk("回复我的")
        }
        MessageListBean(errorCode = "0").ensureOk("回复我的")
        MessageListBean(errorCode = null).ensureOk("回复我的")
    }

    @Test
    fun userLikeForumBean_toChannel_mapsCoreFields() {
        val channel = ai.saniou.thread.data.source.tieba.model.UserLikeForumBean.ForumBean(
            id = "42",
            name = "linux",
            levelId = "8",
            levelName = "资深吧友",
            avatar = "https://img.example/a.png",
            slogan = "hello",
        ).toChannel()
        requireNotNull(channel)
        assertEquals("42", channel.id)
        assertEquals("linux", channel.name)
        assertEquals("tieba", channel.sourceId)
        assertEquals("tieba_fav", channel.groupId)
        assertEquals("https://img.example/a.png", channel.logoUrl)
    }
}

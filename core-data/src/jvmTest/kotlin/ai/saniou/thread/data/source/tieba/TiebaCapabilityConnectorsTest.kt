package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.UserPostBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TiebaCapabilityConnectorsTest {
    private val post = UserPostBean.PostBean(
        forumId = "42",
        forumName = "测试吧",
        threadId = "100",
        postId = "101",
        createTime = "1700000000",
        title = "主题",
        userId = "7",
        nameShow = "测试用户",
        userPortrait = "portrait",
        replyNum = "3",
        agree = UserPostBean.AgreeBean(agreeNum = "2"),
        abstracts = listOf(UserPostBean.PostContentBean(type = "0", text = "正文")),
    )

    @Test
    fun mapsUserTopicWithoutLeakingRemoteModel() {
        val topic = post.toTopic()

        assertEquals("100", topic.id)
        assertEquals("42", topic.channelId)
        assertEquals("测试吧", topic.channelName)
        assertEquals("正文", topic.content)
        assertEquals(3, topic.commentCount)
        assertTrue(topic.author.avatar.orEmpty().endsWith("portrait"))
    }

    @Test
    fun mapsUserReplyToCommonComment() {
        val comment = post.toComment()

        assertEquals("101", comment.id)
        assertEquals("100", comment.topicId)
        assertEquals("正文", comment.content)
        assertEquals(2, comment.agreeCount)
    }
}

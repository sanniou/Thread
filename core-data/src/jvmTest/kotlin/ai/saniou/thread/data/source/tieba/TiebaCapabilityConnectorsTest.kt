package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.UserPostBean
import ai.saniou.thread.data.source.tieba.model.ThreadContentBean
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

    @Test
    fun mapsInlineSubPostsToCommonPreview() {
        val child = ThreadContentBean.PostListItemBean(
            id = "child",
            floor = "2",
            time = "1700000001",
            author = ThreadContentBean.UserInfoBean(id = "8", nameShow = "楼中楼"),
            content = listOf(ThreadContentBean.ContentBean(type = "0", text = "子回复")),
        )
        val parent = ThreadContentBean.PostListItemBean(
            id = "parent",
            floor = "2",
            time = "1700000000",
            author = ThreadContentBean.UserInfoBean(id = "7", nameShow = "层主"),
            content = listOf(ThreadContentBean.ContentBean(type = "0", text = "主回复")),
            subPostNumber = "1",
            subPostList = ThreadContentBean.SubPostListBean(subPostList = listOf(child)),
        )

        val mapped = TiebaMapper.mapThreadContentToComments(
            ThreadContentBean(postList = listOf(parent)),
            topicId = "topic",
        ).single()

        assertEquals(1, mapped.subCommentCount)
        assertEquals("parent", mapped.subCommentsPreview.single().replyToId)
        assertEquals("子回复", mapped.subCommentsPreview.single().content)
    }
}

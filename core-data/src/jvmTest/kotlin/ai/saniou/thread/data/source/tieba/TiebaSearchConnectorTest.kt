package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.SearchThreadBean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TiebaSearchConnectorTest {
    private val hit = SearchThreadBean.ThreadInfoBean(
        tid = "200",
        pid = "201",
        title = "搜索标题",
        content = "搜索正文",
        time = "1700000000",
        modifiedTime = 1700000001,
        postNum = "9",
        likeNum = "4",
        shareNum = "1",
        forumId = "55",
        forumName = "测试吧",
        user = SearchThreadBean.UserInfoBean(
            userName = "u",
            showNickname = "昵称",
            userId = "88",
            portrait = "p88",
        ),
        type = 1,
        forumInfo = SearchThreadBean.ForumInfo(forumName = "测试吧", avatar = ""),
    )

    @Test
    fun mapsSearchHitToTopicWithoutLeakingRemoteModel() {
        val topic = hit.toTopic()

        assertEquals("200", topic.id)
        assertEquals("55", topic.channelId)
        assertEquals("测试吧", topic.channelName)
        assertEquals("搜索正文", topic.content)
        assertEquals(9, topic.commentCount)
        assertEquals(4, topic.agreeCount)
        assertEquals("昵称", topic.author.name)
        assertTrue(topic.author.avatar.orEmpty().endsWith("p88"))
        assertEquals(TiebaMapper.SOURCE_ID, topic.sourceId)
    }

    @Test
    fun mapsSearchHitToCommentUsingPid() {
        val comment = hit.toComment()

        assertEquals("201", comment.id)
        assertEquals("200", comment.topicId)
        assertEquals("搜索正文", comment.content)
        assertEquals(4, comment.agreeCount)
    }

    @Test
    fun ensureOkRejectsNonZeroErrorCode() {
        val bean = SearchThreadBean(
            errorCode = 1,
            errorMsg = "失败",
            data = SearchThreadBean.DataBean(hasMore = 0, currentPage = 1),
        )
        val error = assertFailsWith<IllegalStateException> { bean.ensureOk() }
        assertEquals("失败", error.message)
    }

    @Test
    fun hybridRefererIsUrlEncoded() {
        val referer = hybridSearchReferer("kotlin multiplatform")
        assertTrue(referer.contains("hybrid"))
        assertTrue(referer.contains("%"))
    }
}

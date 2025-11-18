package ai.saniou.nmb.ui.components.previews

import ai.saniou.nmb.data.entity.IBaseThread
import ai.saniou.nmb.data.entity.IBaseThreadReply
import ai.saniou.nmb.data.entity.Reply
import ai.saniou.nmb.ui.components.ThreadCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun PreviewThreadCard() {
    val previewThread = object : IBaseThread, IBaseThreadReply {
        override val id: Long = 12345L
        override val replyCount: Long = 99L
        override val img: String = "/img/123.jpg"
        override val ext: String = ".jpg"
        override val now: String = "2023-10-27 10:00:00"
        override val userHash: String = "abcdef"
        override val name: String = "User Name"
        override val title: String = "这是一个非常长的标题，用于测试省略号的效果"
        override val content: String =
            "这是帖子的内容，也会很长很长很长，长到需要被截断并显示省略号...".repeat(5)
        override val sage: Long = 1
        override val fid: Long = 123L
        override val admin: Long = 1L
        override val hide: Long = 0L
        override val remainReplies: Long? = 5
        override val replies: List<Reply> = listOf(
            Reply(
                id = 1L,
                userHash = "fedcba",
                name = "ReplyUser1",
                content = "这是第一条回复。",
                now = "2023-10-27 10:05:00",
                fid = 1,
                replyCount = 99L,
                img = "/img/456.jpg",
                ext = ".jpg",
                title = "回复标题",
                sage = 0,
                admin = 0,
                hide = 0,
            ),
            Reply(
                id = 2L,
                userHash = "ghijkl",
                name = "ReplyUser2",
                content = "这是第二条回复，内容也可能很长。",
                now = "2023-10-27 10:10:00",
                fid = 1,
                replyCount = 99L,
                img = "/img/456.jpg",
                ext = ".jpg",
                title = "回复标题",
                sage = 0,
                admin = 0,
                hide = 0,
            )
        )
    }
    MaterialTheme {
        ThreadCard(thread = previewThread, onClick = {})
    }
}

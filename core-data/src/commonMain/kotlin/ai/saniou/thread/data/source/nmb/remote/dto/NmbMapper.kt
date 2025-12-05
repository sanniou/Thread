package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

fun Forum.toDomain(): Post {
    return Post(
        id = "nmb:${this.id}",
        title = this.title,
        content = this.content,
        author = this.userHash,
        createdAt = parseNmbTime(this.now),
        sourceName = "NMB",
        sourceUrl = "https://www.nmbxd1.com/t/${this.id}",
        forumName = "" // NMB API不直接提供板块名
    )
}

fun ForumDetail.toDomain(): Forum {
    return Forum(
        id = this.id.toString(),
        name = this.showName.ifEmpty { this.name },
        sourceName = "NMB"
    )
}

private fun parseNmbTime(timeString: String): Instant {
    // NMB 时间格式: 2022-06-18(六)05:10:29
    // Ktor aarch64 不支持 java.time
    // 这是一个简化的实现，仅用于演示
    return Clock.System.now()
}
package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.thread.domain.model.Forum
import ai.saniou.thread.domain.model.Post
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun Forum.toDomain(): Post {
    return Post(
        id = "nmb:${this.id}",
        title = TODO(),
        content = TODO(),
        author = TODO(),
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
        sourceName = "NMB",
        now = TODO()
    )
}

@OptIn(ExperimentalTime::class)
private fun parseNmbTime(timeString: String): Instant {
    // NMB 时间格式: 2022-06-18(六)05:10:29
    // Ktor aarch64 不支持 java.time
    // 这是一个简化的实现，仅用于演示
    return Clock.System.now()
}

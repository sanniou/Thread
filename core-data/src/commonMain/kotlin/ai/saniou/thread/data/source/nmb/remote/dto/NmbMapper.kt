package ai.saniou.thread.data.source.nmb.remote.dto

import ai.saniou.nmb.db.table.Forum
import ai.saniou.nmb.db.table.TimeLine
import ai.saniou.thread.domain.model.Forum as DomainForum

fun Forum.toDomain(): DomainForum = DomainForum(
    id = id.toString(),
    name = name,
    sourceName = "nmb",
    tag = null,
    showName = showName,
    msg = msg,
    groupId = fGroup.toString(),
    groupName = "", // This will be populated by the Source
    threadCount = threadCount,
    autoDelete = autoDelete,
)

fun TimeLine.toDomain(): DomainForum = DomainForum(
    id = id.toString(),
    name = name,
    sourceName = "nmb",
    tag = "timeline",
    showName = displayName,
    msg = notice,
    groupId = "-1",
    groupName = "TimeLine",
    threadCount = maxPage?.let { it * 20 }, // Approximate thread count
    autoDelete = null,
)


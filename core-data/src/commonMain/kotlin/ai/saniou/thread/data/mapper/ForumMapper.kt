package ai.saniou.thread.data.mapper

import ai.saniou.thread.domain.model.forum.Forum

fun ai.saniou.thread.db.table.forum.Forum.toDomain(): Forum {
    return Forum(
        id = id,
        name = name,
        showName = showName,
        msg = msg,
        groupId = fGroup,
        groupName = "", // Database doesn't store group name directly in Forum table, might need join or separate fetch
        sourceName = sourceId,
        tag = null, // Not in DB
        threadCount = threadCount,
        autoDelete = autoDelete,
        interval = interval,
        safeMode = safeMode
    )
}

fun Forum.toEntity(): ai.saniou.thread.db.table.forum.Forum {
    return ai.saniou.thread.db.table.forum.Forum(
        id = id,
        sourceId = sourceName,
        fGroup = groupId,
        sort = null, // Not in Domain
        name = name,
        showName = showName,
        msg = msg,
        interval = interval,
        safeMode = safeMode,
        autoDelete = autoDelete,
        threadCount = threadCount,
        permissionLevel = null, // Not in Domain
        forumFuseId = null, // Not in Domain
        status = "active" // Default
    )
}
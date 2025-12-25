package ai.saniou.thread.data.mapper

import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.db.table.forum.Channel as EntityChannel

fun EntityChannel.toDomain(): Channel {
    return Channel(
        id = id,
        name = name,
        displayName = displayName,
        description = description,
        groupId = fGroup,
        groupName = "", // Database doesn't store group name directly in Channel table
        sourceName = sourceId,
        tag = null, // Not in DB
        topicCount = topicCount,
        autoDelete = autoDelete,
        interval = interval,
        safeMode = safeMode
    )
}

fun Channel.toEntity(): EntityChannel {
    return EntityChannel(
        id = id,
        sourceId = sourceName,
        fGroup = groupId,
        sort = null, // Not in Domain
        name = name,
        displayName = displayName,
        description = description,
        interval = interval,
        safeMode = safeMode,
        autoDelete = autoDelete,
        topicCount = topicCount,
        permissionLevel = null, // Not in Domain
        forumFuseId = null, // Not in Domain
        status = "active" // Default
    )
}
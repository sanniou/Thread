package ai.saniou.thread.data.mapper

import ai.saniou.thread.data.source.discourse.remote.dto.DiscourseCategory
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.db.table.forum.Channel as EntityChannel

fun EntityChannel.toDomain(): Channel {
    return Channel(
        id = id,
        name = name,
        displayName = displayName,
        description = description,
        descriptionText = descriptionText,
        groupId = fGroup,
        groupName = "", // Database doesn't store group name directly in Channel table
        sourceName = sourceId,
        sort = sort,
        tag = null, // Not in DB
        topicCount = topicCount,
        postCount = postCount,
        autoDelete = autoDelete,
        interval = interval,
        safeMode = safeMode,
        parentId = parentId,
        color = color,
        textColor = textColor,
        icon = icon,
        emoji = emoji,
        styleType = styleType,
        listViewStyle = listViewStyle,
        logoUrl = logoUrl,
        bannerUrl = bannerUrl,
        slug = slug,
        canCreateTopic = canCreateTopic?.let { it == 1L }, // Correct mapping
        children = emptyList() // Database entities are flat
    )
}

fun Channel.toEntity(): EntityChannel {
    return EntityChannel(
        id = id,
        sourceId = sourceName,
        fGroup = groupId,
        sort = sort,
        name = name,
        displayName = displayName,
        description = description,
        descriptionText = descriptionText,
        interval = interval,
        safeMode = safeMode,
        autoDelete = autoDelete,
        topicCount = topicCount,
        postCount = postCount,
        permissionLevel = null, // Not in Domain
        forumFuseId = null, // Not in Domain
        status = "active", // Default
        parentId = parentId,
        color = color,
        textColor = textColor,
        icon = icon,
        emoji = emoji,
        styleType = styleType,
        listViewStyle = listViewStyle,
        logoUrl = logoUrl,
        bannerUrl = bannerUrl,
        slug = slug,
        canCreateTopic = if (canCreateTopic == true) 1L else 0L
    )
}

fun DiscourseCategory.toDomainTree(sourceName: String): Channel {
    val children = subcategoryList?.map { it.toDomainTree(sourceName) } ?: emptyList()

    return Channel(
        id = id.toString(),
        name = name,
        displayName = name,
        description = description ?: "",
        descriptionText = descriptionText,
        groupId = "discourse_group", // Unified group ID for Discourse
        groupName = "Discourse",
        sourceName = sourceName,
        sort = position?.toLong(),
        tag = null,
        topicCount = topicCount.toLong(),
        postCount = postCount.toLong(),
        autoDelete = null,
        interval = null,
        safeMode = if (readRestricted) "restricted" else "public",
        parentId = parentCategoryId?.toString(),
        color = color,
        textColor = textColor,
        icon = icon,
        emoji = emoji,
        styleType = styleType,
        listViewStyle = subcategoryListStyle,
        logoUrl = uploadedLogo?.url ?: uploadedLogoDark?.url,
        bannerUrl = uploadedBackground?.url ?: uploadedBackgroundDark?.url,
        slug = slug,
        canCreateTopic = null, // From API response (needed to pass down?)
        children = children
    )
}

fun Channel.flatten(): List<Channel> {
    return listOf(this) + children.flatMap { it.flatten() }
}

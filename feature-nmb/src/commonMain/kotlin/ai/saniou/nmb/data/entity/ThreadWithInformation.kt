package ai.saniou.nmb.data.entity

import ai.saniou.nmb.db.table.GetHistoryThreads

data class ThreadWithInformation(
    val thread: Thread,
    val remainReplies: Long?,
    val lastKey: Long,
    val last_access_time: Long,
    val last_read_reply_id: Long,
)

fun GetHistoryThreads.toThreadWithInformation() = ThreadWithInformation(
    thread = Thread(
        id = id,
        fid = fid,
        replyCount = replyCount,
        img = img,
        ext = ext,
        now = now,
        userHash = userHash,
        name = name,
        title = title,
        content = content,
        sage = sage,
        admin = admin,
        hide = hide,
        replies = emptyList()
    ),
    remainReplies = remainReplies,
    lastKey = lastKey,
    last_access_time = last_access_time,
    last_read_reply_id = last_read_reply_id
)
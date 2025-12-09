import ai.saniou.nmb.db.table.ThreadReply

//package ai.saniou.thread.data.mapper
//
//import ai.saniou.nmb.db.table.Forum
//import ai.saniou.nmb.db.table.Thread
//import ai.saniou.nmb.db.table.ThreadReply
//import ai.saniou.thread.domain.model.ForumDetail
//import ai.saniou.thread.domain.model.ThreadDetail
//import ai.saniou.thread.domain.model.ThreadPost
//
///**
// * 将数据库实体 [Thread] 转换为领域模型 [ThreadDetail]
// */
//fun Thread.toDomain(): ThreadDetail {
//    return ThreadDetail(
//        thread = ThreadPost(
//            id = id,
//            fid = fid,
//            replyCount = replyCount,
//            img = img,
//            ext = ext,
//            now = now,
//            userHash = userHash,
//            name = name,
//            title = title,
//            content = content,
//            sage = sage,
//            admin = admin,
//            hide = hide,
//        ),
//        lastReadReplyId = last_read_reply_id ?: 0
//    )
//}
//
///**
// * 将API数据传输对象 [ai.saniou.thread.data.source.nmb.remote.dto.Thread] 转换为数据库实体 [Thread]
// */
//fun ai.saniou.thread.data.source.nmb.remote.dto.Thread.toTable(page: Int): Thread {
//    return Thread(
//        id = id.toLong(),
//        fid = fid.toLong(),
//        replyCount = replyCount,
//        img = img,
//        ext = ext,
//        now = now,
//        userHash = userHash,
//        name = name,
//        title = title,
//        content = content,
//        sage = sage,
//        admin = admin,
//        page = page.toLong(),
//        last_read_reply_id = null, // 这个值由其他逻辑更新
//        hide = false // 默认不隐藏
//    )
//}
//
/**
 * 将API数据传输对象 [ai.saniou.thread.data.source.nmb.remote.dto.Thread] 转换为回复的数据库实体列表
 */
fun ai.saniou.thread.data.source.nmb.remote.dto.Thread.toTableThreadReply(page: Int): List<ThreadReply> {
    return replies.map { reply ->
        ThreadReply(
            id = reply.id.toLong(),
            userHash = reply.userHash,
            title = reply.title,
            name = reply.name,
            now = reply.now,
            content = reply.content,
            admin = reply.admin,
            img = reply.img,
            ext = reply.ext,
            page = page.toLong(),
            threadId = id
        )
    }
}
//
///**
// * 将API数据传输对象 [ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply] 转换为领域模型 [ThreadPost]
// */
//fun ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply.toDomain(): ThreadPost {
//    return ThreadPost(
//        id = id.toLong(),
//        fid = 0, // 回复中没有板块信息
//        replyCount = "0", // 回复中没有回复数
//        img = img,
//        ext = ext,
//        now = now,
//        userHash = userHash,
//        name = name,
//        title = title,
//        content = content,
//        sage = sage,
//        admin = admin,
//        hide = false // 回复默认不隐藏
//    )
//}
//
///**
// * 将数据库实体 [Forum] 转换为领域模型 [ForumDetail]
// */
//fun Forum.toDomain(): ForumDetail {
//    return ForumDetail(
//        id = id,
//        name = name,
//        showName = showName,
//        msg = msg,
//        updateAt = updateAt,
//        threadsCount = threadsCount,
//        postsCount = postsCount
//    )
//}
//
///**
// * 将数据库实体 [ThreadReply] 转换为领域模型 [ThreadPost]
// */
//fun ThreadReply.toDomain(): ThreadPost {
//    return ThreadPost(
//        id = id,
//        fid = 0, // 回复中没有板块信息
//        replyCount = "0", // 回复中没有回复数
//        img = img,
//        ext = ext,
//        now = now,
//        userHash = userHash,
//        name = name,
//        title = title,
//        content = content,
//        sage = 0, // TODO
//        admin = admin,
//        hide = false // 回复默认不隐藏
//    )
//}

package ai.saniou.thread.data.source.tieba

import ai.saniou.thread.data.source.tieba.model.ForumPageBean
import ai.saniou.thread.data.source.tieba.model.ForumRecommend
import ai.saniou.thread.data.source.tieba.model.SubFloorListBean
import ai.saniou.thread.data.source.tieba.model.ThreadContentBean
import ai.saniou.thread.domain.model.forum.Author
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.Image
import ai.saniou.thread.domain.model.forum.Topic
import com.huanchengfly.tieba.post.api.models.protos.frsPage.FrsPageResponse
import com.huanchengfly.tieba.post.api.models.protos.hotThreadList.HotThreadListResponse
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.personalized.PersonalizedResponse
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListResponse
import com.huanchengfly.tieba.post.api.models.protos.userLike.UserLikeResponse
import kotlinx.datetime.Instant

object TiebaMapper {

    const val SOURCE_ID = "tieba"
    private const val SOURCE_NAME = "Tieba"
    private const val BASE_URL = "https://tieba.baidu.com"

    fun mapForumRecommendToChannels(response: ForumRecommend): List<Channel> {
        return response.likeForum.map { forum ->
            Channel(
                id = forum.forumId,
                name = forum.forumName,
                displayName = forum.forumName,
                description = "",
                descriptionText = null,
                groupId = "tieba_fav",
                groupName = "关注的吧",
                sourceName = SOURCE_NAME,
                sort = 0,
                topicCount = null,
                postCount = null,
                autoDelete = null,
                logoUrl = forum.avatar,
                icon = "font-awesome:fa-comments"
            )
        }
    }

    fun mapForumRecommendResponseToChannels(response: com.huanchengfly.tieba.post.api.models.protos.forumRecommend.ForumRecommendResponse): List<Channel> {
        val likeForum = response.data_?.like_forum ?: emptyList()
        return likeForum.map { forum ->
            Channel(
                id = forum.forum_id.toString(),
                name = forum.forum_name,
                displayName = forum.forum_name,
                description = "",
                descriptionText = null,
                groupId = "tieba_fav",
                groupName = "关注的吧",
                sourceName = SOURCE_NAME,
                sort = 0,
                topicCount = null,
                postCount = null,
                autoDelete = null,
                logoUrl = forum.avatar,
                icon = "font-awesome:fa-comments"
            )
        }
    }

    fun mapEntityToChannel(entity: ai.saniou.thread.db.table.forum.Channel): Channel {
        return Channel(
            id = entity.id,
            name = entity.name,
            displayName = entity.displayName,
            description = entity.description,
            descriptionText = entity.descriptionText,
            groupId = entity.fGroup,
            groupName = "", // TODO: Join with ChannelCategory if needed
            sourceName = SOURCE_NAME,
            sort = entity.sort,
            topicCount = entity.topicCount,
            postCount = entity.postCount,
            autoDelete = entity.autoDelete,
            interval = entity.interval,
            safeMode = entity.safeMode,
            parentId = entity.parentId,
            color = entity.color,
            textColor = entity.textColor,
            icon = entity.icon,
            emoji = entity.emoji,
            styleType = entity.styleType,
            listViewStyle = entity.listViewStyle,
            logoUrl = entity.logoUrl,
            bannerUrl = entity.bannerUrl,
            slug = entity.slug,
            canCreateTopic = entity.canCreateTopic == 1L
        )
    }

    fun mapChannelToEntity(channel: Channel): ai.saniou.thread.db.table.forum.Channel {
        return ai.saniou.thread.db.table.forum.Channel(
            id = channel.id,
            sourceId = SOURCE_ID,
            fGroup = channel.groupId,
            sort = channel.sort,
            name = channel.name,
            displayName = channel.displayName,
            description = channel.description,
            descriptionText = channel.descriptionText,
            interval = channel.interval,
            safeMode = channel.safeMode,
            autoDelete = channel.autoDelete,
            topicCount = channel.topicCount,
            postCount = channel.postCount,
            permissionLevel = null,
            forumFuseId = null,
            status = null,
            parentId = channel.parentId,
            color = channel.color,
            textColor = channel.textColor,
            icon = channel.icon,
            emoji = channel.emoji,
            styleType = channel.styleType,
            listViewStyle = channel.listViewStyle,
            logoUrl = channel.logoUrl,
            bannerUrl = channel.bannerUrl,
            slug = channel.slug,
            canCreateTopic = if (channel.canCreateTopic == true) 1L else 0L
        )
    }

    fun mapForumPageToTopics(response: ForumPageBean): List<Topic> {
        val forumName = response.forum?.name ?: ""
        val forumId = response.forum?.id ?: ""

        return response.threadList?.mapNotNull { thread ->
            if (thread.tid == null) return@mapNotNull null

            val author = Author(
                id = thread.authorId ?: "0",
                name = response.userList?.find { it.id == thread.authorId }?.nameShow
                    ?: response.userList?.find { it.id == thread.authorId }?.name
                    ?: "Unknown",
                avatar = response.userList?.find { it.id == thread.authorId }?.portrait?.let {
                    "http://tb.himg.baidu.com/sys/portrait/item/$it"
                },
                sourceName = SOURCE_NAME
            )

            val images = thread.media?.mapNotNull { media ->
                if (media.type == "3") { // Image type
                    Image(
                        originalUrl = media.bigPic ?: media.originPic ?: media.srcPic ?: "",
                        thumbnailUrl = media.srcPic ?: media.bigPic ?: "",
                        width = null, // Width/Height not available in MediaInfoBean
                        height = null
                    )
                } else null
            } ?: emptyList()

            Topic(
                id = thread.tid,
                channelId = forumId,
                channelName = forumName,
                title = thread.title,
                content = thread.abstractBeans?.joinToString("\n") { it?.text ?: "" } ?: "",
                summary = thread.abstractBeans?.joinToString("") { it?.text ?: "" },
                author = author,
                createdAt = thread.createTime?.toLongOrNull()?.let { Instant.fromEpochSeconds(it) }
                    ?: Instant.fromEpochSeconds(0),
                commentCount = thread.replyNum?.toLongOrNull() ?: 0,
                images = images,
                isSage = false,
                isAdmin = false, // Cannot determine easily from list
                isHidden = false,
                isLocal = false,
                sourceName = SOURCE_NAME,
                sourceId = SOURCE_ID,
                sourceUrl = "$BASE_URL/p/${thread.tid}"
            )
        } ?: emptyList()
    }

    fun mapThreadContentToTopic(response: ThreadContentBean, threadId: String): Topic {
        val forumId = response.forum?.id ?: response.displayForum?.id ?: ""
        val forumName = response.forum?.name ?: response.displayForum?.name ?: ""

        val mainPost = response.postList?.firstOrNull { it.floor == "1" }
            ?: response.postList?.firstOrNull()

        val authorBean = mainPost?.author ?: response.thread?.author
        val author = Author(
            id = authorBean?.id ?: "0",
            name = authorBean?.nameShow ?: authorBean?.name ?: "Unknown",
            avatar = authorBean?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
            sourceName = SOURCE_NAME
        )

        val threadTitle = response.thread?.title ?: mainPost?.title ?: ""

        // Extract content from first post
        val content =
            mainPost?.content?.joinToString("") { contentBean: ThreadContentBean.ContentBean ->
                when (contentBean.type) {
                    "0" -> contentBean.text ?: "" // Text
                    "1" -> "<a href=\"${contentBean.link}\">${contentBean.text}</a>" // Link
                    "2" -> if (contentBean.text == "#") "" else contentBean.text
                        ?: "" // Emoji/Smiley (simplified)
                    "3" -> "<img src=\"${contentBean.bigCdnSrc ?: contentBean.cdnSrc ?: contentBean.src}\" />" // Image
                    else -> contentBean.text ?: ""
                }
            } ?: ""

        val images = mainPost?.content?.filter { it.type == "3" }
            ?.map { contentBean: ThreadContentBean.ContentBean ->
                Image(
                    originalUrl = contentBean.bigCdnSrc ?: contentBean.cdnSrc ?: contentBean.src
                    ?: "",
                    thumbnailUrl = contentBean.cdnSrc ?: contentBean.src ?: "",
                    width = contentBean.width?.toIntOrNull(),
                    height = contentBean.height?.toIntOrNull()
                )
            } ?: emptyList()

        return Topic(
            id = threadId,
            channelId = forumId,
            channelName = forumName,
            title = threadTitle,
            content = content,
            summary = null,
            author = author,
            createdAt = mainPost?.time?.toLongOrNull()?.let { Instant.fromEpochSeconds(it) }
                ?: Instant.fromEpochSeconds(0),
            commentCount = response.thread?.replyNum?.toLongOrNull() ?: 0,
            images = images,
            isSage = false,
            isAdmin = authorBean?.isBawu == "1",
            isHidden = false,
            sourceName = SOURCE_NAME,
            sourceId = SOURCE_ID,
            sourceUrl = "$BASE_URL/p/$threadId"
        )
    }

    fun mapThreadContentToComments(response: ThreadContentBean, topicId: String): List<Comment> {
        // Filter out the first post (floor 1) as it is the topic content, unless we are paging and this page doesn't contain floor 1
        // But typically floor 1 is handled in Topic.
        // Let's include all posts for now, maybe filtered by caller or UI.

        return response.postList?.map { post: ThreadContentBean.PostListItemBean ->
            val author = Author(
                id = post.authorId ?: post.author?.id ?: "0",
                name = post.author?.nameShow ?: post.author?.name ?: "Unknown",
                avatar = post.author?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
                sourceName = SOURCE_NAME
            )

            val content =
                post.content?.joinToString("") { contentBean: ThreadContentBean.ContentBean ->
                    when (contentBean.type) {
                        "0" -> contentBean.text ?: ""
                        "1" -> "<a href=\"${contentBean.link}\">${contentBean.text}</a>"
                        "2" -> if (contentBean.text == "#") "" else contentBean.text ?: ""
                        "3" -> "<img src=\"${contentBean.bigCdnSrc ?: contentBean.cdnSrc ?: contentBean.src}\" />"
                        else -> contentBean.text ?: ""
                    }
                } ?: ""

            val images = post.content?.filter { it.type == "3" }
                ?.map { contentBean: ThreadContentBean.ContentBean ->
                    Image(
                        originalUrl = contentBean.bigCdnSrc ?: contentBean.cdnSrc ?: contentBean.src
                        ?: "",
                        thumbnailUrl = contentBean.cdnSrc ?: contentBean.src ?: "",
                        width = contentBean.width?.toIntOrNull(),
                        height = contentBean.height?.toIntOrNull()
                    )
                } ?: emptyList()

            Comment(
                id = post.id ?: "",
                topicId = topicId,
                author = author,
                createdAt = post.time?.toLongOrNull()?.let { Instant.fromEpochSeconds(it) }
                    ?: Instant.fromEpochSeconds(0),
                title = post.title,
                content = content,
                images = images,
                isAdmin = post.author?.isBawu == "1",
                floor = post.floor?.toIntOrNull(),
                replyToId = null // Tieba main floor replies don't have direct parent usually, unless sub-post
            )
        } ?: emptyList()
    }

    fun mapSubFloorListToComments(response: SubFloorListBean, topicId: String): List<Comment> {
        return response.subPostList?.map { post: SubFloorListBean.PostInfo ->
            val author = Author(
                id = post.author.id ?: "0",
                name = post.author.nameShow ?: post.author.name ?: "Unknown",
                avatar = post.author.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
                sourceName = SOURCE_NAME
            )

            val content =
                post.content.joinToString("") { contentBean: ThreadContentBean.ContentBean ->
                    when (contentBean.type) {
                        "0" -> contentBean.text ?: ""
                        "1" -> "<a href=\"${contentBean.link}\">${contentBean.text}</a>"
                        "2" -> if (contentBean.text == "#") "" else contentBean.text ?: ""
                        "3" -> "<img src=\"${contentBean.bigCdnSrc ?: contentBean.cdnSrc ?: contentBean.src}\" />"
                        else -> contentBean.text ?: ""
                    }
                }

            Comment(
                id = post.id,
                topicId = topicId,
                author = author,
                createdAt = post.time.toLongOrNull()?.let { Instant.fromEpochSeconds(it) }
                    ?: Instant.fromEpochSeconds(0),
                title = post.title,
                content = content,
                images = emptyList(), // Subposts rarely have images in this list view? Need check
                isAdmin = post.author.isBawu == "1",
                floor = null, // Subposts are within a floor
                replyToId = null // Could infer from content if it starts with "Reply to..."
            )
        } ?: emptyList()
    }

    fun mapFrsPageResponseToTopics(
        response: FrsPageResponse,
        defaultForumName: String,
        defaultForumId: String,
    ): List<Topic> {
        val data = response.data_ ?: return emptyList()
        val forumName = data.forum?.name ?: defaultForumName
        val forumId = data.forum?.id?.toString() ?: defaultForumId

        return data.thread_list.mapNotNull { thread ->
            val tid = thread.id.toString()

            val authorUser = data.user_list.find { it.id == thread.authorId }
            val author = Author(
                id = thread.authorId.toString(),
                name = authorUser?.nameShow ?: authorUser?.name ?: "Unknown",
                avatar = authorUser?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
                sourceName = SOURCE_NAME
            )

            val images = thread.media.mapNotNull { media ->
                if (media.type == 3) {
                    Image(
                        originalUrl = media.bigPic,
                        thumbnailUrl = media.srcPic,
                        width = media.width,
                        height = media.height
                    )
                } else null
            }

            Topic(
                id = tid,
                channelId = forumId,
                channelName = forumName,
                title = thread.title,
                content = thread._abstract.joinToString("\n") { it.text },
                summary = thread._abstract.joinToString("") { it.text },
                author = author,
                createdAt = thread.createTime.toLong().let { Instant.fromEpochSeconds(it) },
                commentCount = thread.replyNum.toLong(),
                images = images,
                isSage = false,
                isAdmin = false,
                isHidden = false,
                isLocal = false,
                sourceName = SOURCE_NAME,
                sourceId = SOURCE_ID,
                sourceUrl = "$BASE_URL/p/$tid"
            )
        }
    }

    fun mapPersonalizedResponseToTopics(response: PersonalizedResponse): List<Topic> {
        val data = response.data_ ?: return emptyList()
        return data.thread_list.mapNotNull { thread ->
            val tid = thread.id.toString()
            val author = Author(
                id = thread.authorId.toString(),
                name = thread.author?.nameShow ?: thread.author?.name ?: "Unknown",
                avatar = thread.author?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
                sourceName = SOURCE_NAME
            )

            val images = thread.media.mapNotNull { media ->
                if (media.type == 3) {
                    Image(
                        originalUrl = media.bigPic,
                        thumbnailUrl = media.srcPic,
                        width = media.width,
                        height = media.height
                    )
                } else null
            }

            Topic(
                id = tid,
                channelId = thread.forumId.toString(),
                channelName = thread.forumName,
                title = thread.title,
                content = thread._abstract.joinToString("\n") { it.text },
                summary = thread._abstract.joinToString("") { it.text },
                author = author,
                createdAt = thread.createTime.toLong().let { Instant.fromEpochSeconds(it) },
                commentCount = thread.replyNum.toLong(),
                images = images,
                isSage = false,
                isAdmin = false,
                isHidden = false,
                isLocal = false,
                sourceName = SOURCE_NAME,
                sourceId = SOURCE_ID,
                sourceUrl = "$BASE_URL/p/$tid"
            )
        }
    }

    fun mapUserLikeResponseToTopics(response: UserLikeResponse): List<Topic> {
        val data = response.data_ ?: return emptyList()
        // In Proto: repeated ConcernData threadInfo = 1;
        // ConcernData has ThreadInfo threadList = 1;
        return data.threadInfo.mapNotNull { concernData ->
            val thread = concernData.threadList ?: return@mapNotNull null
            val tid = thread.id.toString()
            val author = Author(
                id = thread.authorId.toString(),
                name = thread.author?.nameShow ?: thread.author?.name ?: "Unknown",
                avatar = thread.author?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
                sourceName = SOURCE_NAME
            )

            val images = thread.media.mapNotNull { media ->
                if (media.type == 3) {
                    Image(
                        originalUrl = media.bigPic,
                        thumbnailUrl = media.srcPic,
                        width = media.width,
                        height = media.height
                    )
                } else null
            }

            Topic(
                id = tid,
                channelId = thread.forumId.toString(),
                channelName = thread.forumName,
                title = thread.title,
                content = thread._abstract.joinToString("\n") { it.text },
                summary = thread._abstract.joinToString("") { it.text },
                author = author,
                createdAt = thread.createTime.toLong().let { Instant.fromEpochSeconds(it) },
                commentCount = thread.replyNum.toLong(),
                images = images,
                isSage = false,
                isAdmin = false,
                isHidden = false,
                isLocal = false,
                sourceName = SOURCE_NAME,
                sourceId = SOURCE_ID,
                sourceUrl = "$BASE_URL/p/$tid"
            )
        }
    }

    fun mapHotThreadListResponseToTopics(response: HotThreadListResponse): List<Topic> {
        val data = response.data_ ?: return emptyList()
        
        val threads = data.threadInfo.mapNotNull { thread ->
            val tid = thread.id.toString()
            val author = Author(
                id = thread.authorId.toString(),
                name = thread.author?.nameShow ?: thread.author?.name ?: "Unknown",
                avatar = thread.author?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
                sourceName = SOURCE_NAME
            )

            val images = thread.media.mapNotNull { media ->
                if (media.type == 3) {
                    Image(
                        originalUrl = media.bigPic,
                        thumbnailUrl = media.srcPic,
                        width = media.width,
                        height = media.height
                    )
                } else null
            }

            Topic(
                id = tid,
                channelId = thread.forumId.toString(),
                channelName = thread.forumName,
                title = thread.title,
                content = thread._abstract.joinToString("\n") { it.text },
                summary = thread._abstract.joinToString("") { it.text },
                author = author,
                createdAt = thread.createTime.toLong().let { Instant.fromEpochSeconds(it) },
                commentCount = thread.replyNum.toLong(),
                images = images,
                isSage = false,
                isAdmin = false,
                isHidden = false,
                isLocal = false,
                sourceName = SOURCE_NAME,
                sourceId = SOURCE_ID,
                sourceUrl = "$BASE_URL/p/$tid"
            )
        }

        val topics = data.topicList.map { topic ->
             Topic(
                id = topic.topicId.toString(),
                channelId = "",
                channelName = "话题",
                title = topic.topicName,
                content = topic.topicDesc,
                summary = topic.topicDesc,
                author = Author(id = "0", name = "话题", avatar = null, sourceName = SOURCE_NAME),
                createdAt = Instant.fromEpochSeconds(0), // No time in RecommendTopicList
                commentCount = topic.discussNum.toLong(),
                images = if (topic.topicPic.isNotEmpty()) listOf(Image(topic.topicPic, topic.topicPic, null, null)) else emptyList(),
                isSage = false,
                isAdmin = false,
                isHidden = false,
                isLocal = false,
                sourceName = SOURCE_NAME,
                sourceId = SOURCE_ID,
                sourceUrl = ""
            )
        }

        return threads + topics
    }

    fun mapTopicListResponseToTopics(response: TopicListResponse): List<Topic> {
        val data = response.data_ ?: return emptyList()
        return data.topic_list.map { topic ->
             Topic(
                id = topic.topic_id.toString(),
                channelId = "",
                channelName = "话题",
                title = topic.topic_name,
                content = topic.topic_desc,
                summary = topic.topic_desc,
                author = Author(id = "0", name = "话题", avatar = null, sourceName = SOURCE_NAME),
                createdAt = Instant.fromEpochSeconds(0),
                commentCount = topic.discuss_num.toLong(),
                images = if (topic.topic_image.isNotEmpty()) listOf(Image(topic.topic_image, topic.topic_image, null, null)) else emptyList(),
                isSage = false,
                isAdmin = false,
                isHidden = false,
                isLocal = false,
                sourceName = SOURCE_NAME,
                sourceId = SOURCE_ID,
                sourceUrl = ""
            )
        }
    }

    fun mapPbPageResponseToTopic(response: PbPageResponse, threadId: String): Topic? {
        val data = response.data_ ?: return null
        val thread = data.thread ?: return null
        val forum = data.forum

        val forumId = forum?.id?.toString() ?: ""
        val forumName = forum?.name ?: ""

        val mainPost = data.post_list.firstOrNull { it.floor == 1 } ?: data.post_list.firstOrNull()

        val authorUser = data.user_list.find { it.id == mainPost?.author_id }
        val author = Author(
            id = mainPost?.author_id?.toString() ?: thread.authorId.toString(),
            name = authorUser?.nameShow ?: authorUser?.name ?: "Unknown",
            avatar = authorUser?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
            sourceName = SOURCE_NAME
        )

        val content = mainPost?.content?.joinToString("") { content ->
            when (content.type) {
                0 -> content.text ?: ""
                1 -> "<a href=\"${content.link}\">${content.text}</a>"
                2 -> if (content.text == "#") "" else content.text ?: ""
                3 -> "<img src=\"${content.bigCdnSrc}\" />"
                else -> content.text ?: ""
            }
        } ?: ""

        val images = mainPost?.content?.filter { it.type == 3 }?.map { content ->
            Image(
                originalUrl = content.bigCdnSrc,
                thumbnailUrl = content.cdnSrc,
                width = content.width,
                height = content.height
            )
        } ?: emptyList()

        return Topic(
            id = threadId,
            channelId = forumId,
            channelName = forumName,
            title = thread.title,
            content = content,
            summary = null,
            author = author,
            createdAt = mainPost?.time?.toLong()?.let { Instant.fromEpochSeconds(it) }
                ?: Instant.fromEpochSeconds(0),
            commentCount = thread.replyNum.toLong(),
            images = images,
            isSage = false,
            isAdmin = false,
            isHidden = false,
            sourceName = SOURCE_NAME,
            sourceId = SOURCE_ID,
            sourceUrl = "$BASE_URL/p/$threadId"
        )
    }

    fun mapPbPageResponseToComments(response: PbPageResponse, topicId: String): List<Comment> {
        val data = response.data_ ?: return emptyList()

        return data.post_list.map { post ->
            val authorUser = data.user_list.find { it.id == post.author_id }
            val author = Author(
                id = post.author_id.toString(),
                name = authorUser?.nameShow ?: authorUser?.name ?: "Unknown",
                avatar = authorUser?.portrait?.let { "http://tb.himg.baidu.com/sys/portrait/item/$it" },
                sourceName = SOURCE_NAME
            )

            val content = post.content.joinToString("") { content ->
                when (content.type) {
                    0 -> content.text ?: ""
                    1 -> "<a href=\"${content.link}\">${content.text}</a>"
                    2 -> if (content.text == "#") "" else content.text ?: ""
                    3 -> "<img src=\"${content.bigCdnSrc}\" />"
                    else -> content.text ?: ""
                }
            }

            val images = post.content.filter { it.type == 3 }.map { content ->
                Image(
                    originalUrl = content.bigCdnSrc,
                    thumbnailUrl = content.cdnSrc,
                    width = content.width,
                    height = content.height
                )
            }

            Comment(
                id = post.id.toString(),
                topicId = topicId,
                author = author,
                createdAt = post.time.toLong().let { Instant.fromEpochSeconds(it) },
                title = post.title,
                content = content,
                images = images,
                isAdmin = false, // Check bawu/manager info if needed
                floor = post.floor,
                replyToId = null
            )
        }
    }
}
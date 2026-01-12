package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.cache.CacheStrategy
import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.mapper.toMetadata
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.Reply
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.toCommentEntity
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toDomainComment
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.forum.Image
import ai.saniou.thread.domain.model.forum.Account
import ai.saniou.thread.domain.model.forum.Channel
import ai.saniou.thread.domain.model.forum.Comment
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.model.forum.TopicMetadata
import ai.saniou.thread.domain.model.user.LoginField
import ai.saniou.thread.domain.model.user.LoginStrategy
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceCapabilities
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.network.SaniouResult
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime


const val NMBSourceId = "nmb"
const val NMBSourceName = "A岛"

class NmbSource(
    private val nmbXdApi: NmbXdApi,
    private val db: Database,
    private val settingsRepository: SettingsRepository,
    private val cdnManager: CdnManager,
) : Source {
    override val id: String = NMBSourceId
    override val name: String = NMBSourceName

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsPagination = true
    )

    override val trendSource by lazy { NmbTrendSource(this) }

    override val loginStrategy: LoginStrategy = LoginStrategy.Manual(
        title = "A岛匿名版 - 饼干导入",
        description = "请输入您在 A岛匿名版 获取的饼干 (Cookie)，通常是一串随机字符。",
        fields = listOf(
            LoginField(
                key = "cookie",
                label = "饼干 (Cookie)",
                hint = "例如：H4k...z9a",
                isMultiline = true
            )
        )
    )

    override val isInitialized: Flow<Boolean> =
        settingsRepository.observeValue<Boolean>("nmb_initialized")
            .map { it == true }

    override fun observeChannels(): Flow<List<Channel>> {
        val channelFlow =
            db.channelQueries.getChannelsBySource(id).asFlow().mapToList(Dispatchers.Default)
        val timelinesFlow =
            db.timeLineQueries.getAllTimeLines().asFlow().mapToList(Dispatchers.Default)
        val categoriesFlow = db.channelQueries.getChannelCategoriesBySource(id).asFlow()
            .mapToList(Dispatchers.Default)

        return kotlinx.coroutines.flow.combine(
            channelFlow,
            timelinesFlow,
            categoriesFlow
        ) { channelFromDb, timelines, categoriesList ->
            val categories = categoriesList.associateBy { it.id }
            val channels = channelFromDb.map { channel ->
                channel.toDomain(db.channelQueries).copy(
                    groupName = categories[channel.fGroup]?.name ?: "未知分类"
                )
            }
            buildList {
                addAll(channels)
                addAll(timelines.map { it.toDomain() })
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun fetchChannels(): Result<Unit> {
        // 1. Check cache policy
        val needUpdate = CacheStrategy.shouldFetch(
            db = db,
            keyType = RemoteKeyType.FORUM_CATEGORY,
            keyId = RemoteKeyType.FORUM_CATEGORY.name,
            expiration = 1.days
        )

        if (!needUpdate) {
            return Result.success(Unit)
        }

        // 2. Fetch from remote and update database
        val remoteResult = fetchAndStoreRemoteForums()
        if (remoteResult.isFailure) {
            return Result.failure(remoteResult.exceptionOrNull()!!)
        }

        CacheStrategy.updateLastFetchTime(
            db = db,
            keyType = RemoteKeyType.FORUM_CATEGORY,
            keyId = RemoteKeyType.FORUM_CATEGORY.name
        )
        return Result.success(Unit)
    }

    private suspend fun fetchAndStoreRemoteForums(): Result<Unit> {
        // Fetch forums
        when (val forumListResponse = nmbXdApi.getForumList()) {
            is SaniouResult.Success -> {
                val forumCategories = forumListResponse.data.map { category ->
                    category.copy(forums = category.forums.filter { it.id > 0 })
                }
                forumCategories.forEach { forumCategory ->
                    db.channelQueries.insertChannelCategory(forumCategory.toTable())
                    forumCategory.forums.forEach { forumDetail ->
                        db.channelQueries.insertChannel(forumDetail.toTable())
                    }
                }
            }

            is SaniouResult.Error -> return Result.failure(forumListResponse.ex)
        }

        // Fetch timelines
        when (val timelineListResponse = nmbXdApi.getTimelineList()) {
            is SaniouResult.Success -> {
                timelineListResponse.data.forEach { timeLine ->
                    db.timeLineQueries.insertTimeLine(timeLine.toTable())
                }
            }

            is SaniouResult.Error -> return Result.failure(timelineListResponse.ex)
        }
        return Result.success(Unit)
    }

    override suspend fun getChannelTopics(
        channelId: String,
        page: Int,
        isTimeline: Boolean,
    ): Result<List<Topic>> {
        val fid = channelId.toLongOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid NMB channel ID"))

        return try {
            val response = if (isTimeline) {
                nmbXdApi.timeline(fid, page.toLong())
            } else {
                nmbXdApi.showf(fid, page.toLong())
            }

            if (response is SaniouResult.Success) {
                val topics = response.data.map {
                    it.toDomain(cdnManager.currentCdnUrl.value)
                }
                Result.success(topics)
            } else {
                Result.failure((response as SaniouResult.Error).ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getTopicDetail(threadId: String, page: Int): Result<Topic> {
        return try {
            val tid = threadId.toLongOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid NMB thread ID"))

            val response = nmbXdApi.thread(tid, page.toLong())
            if (response is SaniouResult.Success) {
                Result.success(response.data.toDomain(cdnManager.currentCdnUrl.value))
            } else {
                Result.failure((response as SaniouResult.Error).ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopicMetadata(threadId: String, page: Int): Result<TopicMetadata> {
        return getTopicDetail(threadId, page).map { topic ->
            topic.toMetadata().copy(
                totalPages = (topic.commentCount / 19).toInt() + if (topic.commentCount % 19 > 0) 1 else 0
            )
        }
    }

    override fun getChannel(channelId: String): Flow<Channel?> {
        return db.channelQueries.getChannel(id = channelId, sourceId = id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain(db.channelQueries) }
    }

    suspend fun updateThreadLastAccessTime(threadId: Long, time: Long) {
        db.topicQueries.updateTopicLastAccessTime(time, id, threadId.toString())
    }

    suspend fun updateThreadLastReadReplyId(threadId: Long, replyId: String) {
        db.topicQueries.updateTopicLastReadCommentId(replyId, id, threadId.toString())
    }

    fun observeIsSubscribed(subscriptionKey: String, threadId: Long): Flow<Boolean> {
        return db.subscriptionQueries.isSubscribed(subscriptionKey, id, threadId.toString())
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun addSubscription(subscriptionKey: String, thread: Thread) {
        db.subscriptionQueries.insertSubscription(
            subscriptionKey = subscriptionKey,
            sourceId = id,
            topicId = thread.id.toString(),
            page = 1L,
            subscriptionTime = Clock.System.now().toEpochMilliseconds(),
            isLocal = 1L
        )
    }

    suspend fun deleteSubscription(subscriptionKey: String, threadId: Long) {
        db.subscriptionQueries.deleteSubscription(subscriptionKey, id, threadId.toString())
    }

    suspend fun getSortedAccounts(): List<Account> {
        return db.accountQueries.getSortedAccounts().asFlow().mapToList(Dispatchers.Default).first()
            .toDomain()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun insertAccount(alias: String, cookie: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val count =
            db.accountQueries.countAccounts().asFlow().mapToList(Dispatchers.Default).first().size
        db.accountQueries.insertAccount(
            id = cookie,
            source_id = "nmb",
            account = cookie,
            uid = null,
            alias = alias,
            avatar = null,
            extra_data = null,
            sort = count.toLong(),
            is_current = 0L,
            createdAt = now,
            lastUsedAt = now
        )
    }

    data class TrendAnchor(
        val page: Int,
        val start: String,
        val end: String,
        val count: Int,
    )

    suspend fun getTrendAnchor(): TrendAnchor? {
        return db.keyValueQueries.getKeyValue("trend_anchor")
            .executeAsOneOrNull()
            ?.content
            ?.split("|")
            ?.let {
                if (it.size >= 3) {
                    val page = it[0].toInt()
                    val start = it[1]
                    val end = it[2]
                    // 兼容旧数据，默认 19 (满页)
                    val count = if (it.size >= 4) it[3].toInt() else 19
                    TrendAnchor(page, start, end, count)
                } else null
            }
    }

    suspend fun setTrendAnchor(page: Int, startNow: String, endNow: String, count: Int) {
        db.keyValueQueries.insertKeyValue("trend_anchor", "$page|$startNow|$endNow|$count")
    }

    /**
     * 获取引用的回复内容
     */
    suspend fun getReference(refId: Long): Reply? {
        return try {
            val response = nmbXdApi.ref(refId)
            if (response is SaniouResult.Success) {
                val reference = response.data
                // 将 NmbReference 转换为 Reply
                Reply(
                    id = reference.id,
                    fid = 0, // 引用 API 不返回 fid
                    replyCount = 0, // 引用 API 不返回回复数量
                    img = reference.img,
                    ext = reference.ext,
                    now = reference.now,
                    userHash = reference.userHash,
                    name = reference.name,
                    title = reference.title,
                    content = reference.content,
                    sage = reference.sage,
                    admin = 0, // 引用 API 不返回 admin
                    hide = reference.hide
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }


    override suspend fun getTopicComments(
        threadId: String,
        page: Int,
        isPoOnly: Boolean,
    ): Result<List<Comment>> {
        val tid = threadId.toLongOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid NMB thread ID"))

        return try {
            val apiCall = if (isPoOnly) {
                nmbXdApi.po(tid, page.toLong())
            } else {
                nmbXdApi.thread(tid, page.toLong())
            }

            when (apiCall) {
                is SaniouResult.Success -> {
                    val thread = apiCall.data
                    val comments = mutableListOf<Comment>()

                    if (page == 1) {
                        // 将 Thread (主楼) 作为 Comment 存入
                        // 主楼也视为一个 Comment
                        comments.add(thread.toDomainComment(sourceId = id, cdnUrl = cdnManager.currentCdnUrl.value))
                    }

                    // 添加回复
                    comments.addAll(
                        thread.replies
                            //"id":9999999,"user_hash":"Tips"
                            .filter { it.id != 9999999L }
                            .map { it.toDomain(threadId, cdnManager.currentCdnUrl.value) })

                    Result.success(comments)
                }

                is SaniouResult.Error -> {
                    Result.failure(apiCall.ex)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendThread(page: Int): Result<Thread> {
        return try {
            when (val response = nmbXdApi.getTrendThread(page = page.toLong())) {
                is SaniouResult.Success -> {
                    val thread = response.data
                    // 保存到数据库
                    db.topicQueries.transaction {
                        val topic = thread.toTable(id, page = page.toLong())
                        db.topicQueries.upsertTopic(topic)
                        // 保存 Topic 图片
                        saveNmbImage(
                            db = db,
                            cdnManager = cdnManager,
                            sourceId = id,
                            parentId = thread.id.toString(),
                            parentType = ImageType.Topic,
                            img = thread.img,
                            ext = thread.ext
                        )

                        // 将 Thread (主楼) 作为 Comment 存入
                        db.commentQueries.upsertComment(
                            thread.toCommentEntity(sourceId = id)
                        )
                        // 保存 Comment 图片
                        saveNmbImage(
                            db = db,
                            cdnManager = cdnManager,
                            sourceId = id,
                            parentId = thread.id.toString(),
                            parentType = ImageType.Comment,
                            img = thread.img,
                            ext = thread.ext
                        )

                        thread.replies.forEach { reply ->
                            db.commentQueries.upsertComment(
                                reply.toTableReply(
                                    sourceId = id,
                                    threadId = thread.id,
                                    page = page.toLong()
                                )
                            )
                            // 保存 Comment 图片
                            saveNmbImage(
                                db = db,
                                cdnManager = cdnManager,
                                sourceId = id,
                                parentId = reply.id.toString(),
                                parentType = ImageType.Comment,
                                img = reply.img,
                                ext = reply.ext
                            )
                        }
                    }
                    Result.success(thread)
                }

                is SaniouResult.Error -> Result.failure(response.ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据日期获取 Trend 回复
     * @param targetDate 目标日期
     * @return 目标日期的第一个回复（通常是当天的 Trend 发布内容）
     */
    suspend fun getTrendReplyByDate(targetDate: kotlinx.datetime.LocalDate): Result<ThreadReply?> {
        val anchor = getTrendAnchor()
        var predictedPage = 0

        // 1. 尝试通过 Anchor 预测页面
        if (anchor != null) {
            try {
                val (cachedPage, cachedStart, cachedEnd, cachedCount) = anchor
                val startDate =
                    ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(cachedStart)
                val endDate =
                    ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(cachedEnd)

                if (startDate != null && endDate != null) {
                    if (targetDate in startDate..endDate) {
                        predictedPage = cachedPage
                    } else if (targetDate > endDate) {
                        // 目标日期 > 缓存结束日期 (需要往后翻)
                        if (cachedCount < 19) {
                            // 若缓存页未满，必定在当前页(可能刚更新)或下一页，优先查当前页
                            predictedPage = cachedPage
                        } else {
                            // 缓存页已满，估算页码偏移
                            val daysDiff = endDate.daysUntil(targetDate)
                            // 粗略估算：假设每天1条，每页19条。
                            val pageOffset = (daysDiff / 19).coerceAtLeast(1)
                            predictedPage = cachedPage + pageOffset
                        }
                    } else { // targetDate < startDate
                        // 往前找
                        val daysDiff = targetDate.daysUntil(startDate)
                        val pageOffset = (daysDiff / 19).coerceAtLeast(1)
                        predictedPage = (cachedPage - pageOffset).coerceAtLeast(1)
                    }
                }
            } catch (e: Exception) {
                // 预测失败，忽略
            }
        }

        // 2. 如果没有预测或预测页面无效，获取最后一页作为起点 (通常查Trend都是查最近的)
        if (predictedPage <= 0) {
            val firstPageThread = getTrendThread(page = 1).getOrElse { return Result.failure(it) }
            val replyCount = firstPageThread.replyCount
            val lastPage = ((replyCount - 1) / 19) + 1
            predictedPage = lastPage.toInt()
        }

        // 3. 搜索循环 (最多尝试 3 次跳页，解决预测偏差)
        var currentPage = predictedPage
        var steps = 0
        val maxSteps = 3

        while (steps < maxSteps) {
            val threadResult = getTrendThread(currentPage)
            val thread = threadResult.getOrElse { return Result.failure(it) }
            val replies = thread.replies

            if (replies.isEmpty()) {
                // 如果当前页为空，停止搜索 (可能是页码超出)
                return Result.success(null)
            }

            // 更新 Anchor
            val firstNow = replies.first().now
            val lastNow = replies.last().now
            setTrendAnchor(currentPage, firstNow, lastNow, replies.size)

            val pageStart =
                ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(firstNow)
            val pageEnd =
                ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(lastNow)

            if (pageStart == null || pageEnd == null) {
                return Result.failure(IllegalStateException("Date parsing failed for page $currentPage"))
            }

            // 检查命中
            if (targetDate in pageStart..pageEnd) {
                val targetReply = replies.find {
                    ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(it.now) == targetDate
                }
                return Result.success(targetReply)
            }

            // 未命中，决定方向
            if (targetDate > pageEnd) {
                // 目标在未来
                if (replies.size < 19) {
                    // 当前页未满且目标更晚 -> 目标尚未生成
                    return Result.success(null)
                }
                currentPage++
            } else {
                // 目标在过去 (targetDate < pageStart)
                if (currentPage <= 1) {
                    return Result.success(null)
                }
                currentPage--
            }
            steps++
        }

        return Result.success(null)
    }

    suspend fun getLocalLatestReply(threadId: Long): Comment? {
        return db.commentQueries.getLastFiveComments(id, threadId.toString())
            .executeAsList()
            .maxByOrNull { it.id.toLong() }
            ?.toDomain(db.imageQueries)
    }

    suspend fun getLocalReplyByDate(
        threadId: Long,
        targetDate: kotlinx.datetime.LocalDate,
    ): Comment? {
        val startOfDay =
            targetDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val endOfDay = targetDate.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        return db.commentQueries.getCommentByTopicIdAndDateRange(
            sourceId = id,
            topicId = threadId.toString(),
            start = startOfDay,
            end = endOfDay
        ).executeAsOneOrNull()?.toDomain(db.imageQueries)
    }

    fun searchTopicsPager(query: String): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.topicQueries,
                    context = Dispatchers.Default,
                    countQuery = db.topicQueries.countSearchTopics(query),
                    queryProvider = { limit, offset ->
                        db.topicQueries.searchTopics(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.commentQueries, db.imageQueries) }
        }
    }

    fun searchCommentsPager(query: String): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.commentQueries,
                    context = Dispatchers.Default,
                    countQuery = db.commentQueries.countSearchComments(query),
                    queryProvider = { limit, offset ->
                        db.commentQueries.searchComments(query, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
    }

    fun getUserTopicsPager(userHash: String): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.topicQueries,
                    context = Dispatchers.Default,
                    countQuery = db.topicQueries.countTopicsByUserHash(userHash),
                    queryProvider = { limit, offset ->
                        db.topicQueries.getTopicsByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.commentQueries, db.imageQueries) }
        }
    }

    fun getUserCommentsPager(userHash: String): Flow<PagingData<Comment>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.commentQueries,
                    context = Dispatchers.Default,
                    countQuery = db.commentQueries.countCommentsByUserHashNoTopic(userHash),
                    queryProvider = { limit, offset ->
                        db.commentQueries.getCommentsByUserHashOffset(userHash, limit, offset)
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
    }
}

internal suspend fun saveNmbImage(
    db: Database,
    cdnManager: CdnManager,
    sourceId: String,
    parentId: String,
    parentType: ImageType,
    img: String?,
    ext: String?,
) {
    if (!img.isNullOrEmpty() && !ext.isNullOrEmpty()) {
        val originalUrl = cdnManager.buildImageUrl(img, ext, false)
        val thumbUrl = cdnManager.buildImageUrl(img, ext, true)
        val path = "$img$ext"
        db.imageQueries.upsertImage(
            Image(
                id = path, // Using relative path as ID
                sourceId = sourceId,
                parentId = parentId,
                parentType = parentType,
                originalUrl = originalUrl,
                thumbnailUrl = thumbUrl,
                name = null,
                extension = ext,
                path = path,
                width = null,
                height = null,
                sortOrder = 0
            )
        )
    }
}

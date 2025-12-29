package ai.saniou.thread.data.source.nmb

import ai.saniou.thread.data.manager.CdnManager
import ai.saniou.thread.data.mapper.toDomain
import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.Forum
import ai.saniou.thread.data.source.nmb.remote.dto.RemoteKeyType
import ai.saniou.thread.data.source.nmb.remote.dto.Reply
import ai.saniou.thread.data.source.nmb.remote.dto.Thread
import ai.saniou.thread.data.source.nmb.remote.dto.ThreadReply
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.data.source.nmb.remote.dto.toTable
import ai.saniou.thread.data.source.nmb.remote.dto.toTableReply
import ai.saniou.thread.db.Database
import ai.saniou.thread.db.table.Cookie
import ai.saniou.thread.db.table.forum.Image
import ai.saniou.thread.domain.model.forum.ImageType
import ai.saniou.thread.domain.repository.SettingsRepository
import ai.saniou.thread.domain.repository.Source
import ai.saniou.thread.domain.repository.SourceCapabilities
import ai.saniou.thread.domain.repository.observeValue
import ai.saniou.thread.network.SaniouResponse
import app.cash.paging.ExperimentalPagingApi
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
import kotlin.time.Instant
import ai.saniou.thread.db.table.forum.GetTopicsInChannelOffset as GetThreadsInForumOffset
import ai.saniou.thread.domain.model.forum.Channel as DomainForum
import ai.saniou.thread.domain.model.forum.Comment as DomainComment
import ai.saniou.thread.domain.model.forum.Topic as Post

class NmbSource(
    private val nmbXdApi: NmbXdApi,
    private val db: Database,
    private val settingsRepository: SettingsRepository,
    private val cdnManager: CdnManager,
) : Source {
    override val id: String = "nmb"
    override val name: String = "A岛"

    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsTrend = true,
        supportsTrendHistory = true,
        supportsPagination = true
    )

    override val isInitialized: Flow<Boolean> =
        settingsRepository.observeValue<Boolean>("nmb_initialized")
            .map { it == true }

    @OptIn(ExperimentalTime::class)
    override suspend fun getForums(): Result<List<DomainForum>> {
        // 1. Check cache policy
        val now = Clock.System.now()
        val lastQueryTime =
            db.remoteKeyQueries.getRemoteKeyById(
                RemoteKeyType.FORUM_CATEGORY,
                RemoteKeyType.FORUM_CATEGORY.name
            ).executeAsOneOrNull()?.updateAt ?: 0L

        val lastUpdateInstant = Instant.fromEpochMilliseconds(lastQueryTime)
        val needUpdate = now - lastUpdateInstant >= 1.days

        // 2. If cache is outdated, fetch from remote and update database
        if (needUpdate) {
            val remoteResult = fetchAndStoreRemoteForums()
            if (remoteResult.isFailure) {
                return Result.failure(remoteResult.exceptionOrNull()!!)
            }
            db.remoteKeyQueries.insertKey(
                type = RemoteKeyType.FORUM_CATEGORY,
                id = RemoteKeyType.FORUM_CATEGORY.name,
                nextKey = null,
                currKey = Long.MIN_VALUE,
                prevKey = null,
                updateAt = now.toEpochMilliseconds(),
            )
        }

        // 3. Query from database and return
        val forumsFromDb = db.channelQueries.getChannelsBySource(id).executeAsList()
        val timelines = db.timeLineQueries.getAllTimeLines().executeAsList()
        val categories =
            db.channelQueries.getChannelCategoriesBySource(id).executeAsList().associateBy { it.id }

        val forums = forumsFromDb.map { forum ->
            forum.toDomain().copy(
                groupName = categories[forum.fGroup]?.name ?: "未知分类"
            )
        }

        val combined = buildList {
            addAll(forums)
            addAll(timelines.map { it.toDomain() })
        }
        return Result.success(combined)
    }

    private suspend fun fetchAndStoreRemoteForums(): Result<Unit> {
        // Fetch forums
        when (val forumListResponse = nmbXdApi.getForumList()) {
            is SaniouResponse.Success -> {
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

            is SaniouResponse.Error -> return Result.failure(forumListResponse.ex)
        }

        // Fetch timelines
        when (val timelineListResponse = nmbXdApi.getTimelineList()) {
            is SaniouResponse.Success -> {
                timelineListResponse.data.forEach { timeLine ->
                    db.timeLineQueries.insertTimeLine(timeLine.toTable())
                }
            }

            is SaniouResponse.Error -> return Result.failure(timelineListResponse.ex)
        }
        return Result.success(Unit)
    }


    override suspend fun getPosts(forumId: String, page: Int): Result<List<Post>> {
        TODO("Not yet implemented")
    }

    override fun getThreadsPager(
        forumId: String,
        isTimeline: Boolean,
        initialPage: Int,
    ): Flow<PagingData<Post>> {
        val fidLong =
            forumId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())
        val pagerFlow = if (isTimeline) {
            getTimelinePager(fidLong, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
        } else {
            getShowfPager(fidLong, DataPolicy.NETWORK_ELSE_CACHE, initialPage)
        }
        return pagerFlow
    }

    override suspend fun getThreadDetail(threadId: String, page: Int): Result<Post> {
        return try {
            val tid = threadId.toLongOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid NMB thread ID"))

            val response = nmbXdApi.thread(tid, page.toLong())
            if (response is SaniouResponse.Success) {
                val thread = response.data
                // Use default emptyList for images from remote since we don't have ImageQueries here for remote data yet
                // Or map remote images if available in DTO
                Result.success(thread.toDomain())
            } else {
                Result.failure((response as SaniouResponse.Error).ex)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getThreadRepliesPager(
        threadId: String,
        initialPage: Int,
        isPoOnly: Boolean,
    ): Flow<PagingData<DomainComment>> {
        val tid =
            threadId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(PagingData.empty())
        return getThreadRepliesPager(
            tid,
            null,
            DataPolicy.NETWORK_ELSE_CACHE,
            initialPage,
            isPoOnly
        )
    }

    override fun getForum(forumId: String): Flow<DomainForum?> {
        return db.channelQueries.getChannel(id = forumId, sourceId = id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }
    }

    fun getTimelinePager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<Post>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.timeline(fid, page.toLong()) }
        ).flow.map { pagingData ->
            pagingData.map {
                it.toDomain(
                    db.commentQueries,
                    db.imageQueries
                )
            }
        }
    }

    fun getShowfPager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int = 1,
    ): Flow<PagingData<Post>> {
        return createPager(
            fid = fid,
            policy = policy,
            initialPage = initialPage,
            fetcher = { page -> nmbXdApi.showf(fid, page.toLong()) }
        ).flow.map { pagingData ->
            pagingData.map {
                it.toDomain(
                    db.commentQueries,
                    db.imageQueries
                )
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    fun getThreadRepliesPager(
        threadId: Long,
        poUserHash: String?,
        policy: DataPolicy,
        initialPage: Int = 1,
        isPoOnly: Boolean = false,
    ): Flow<PagingData<DomainComment>> {
        val pageSize = 19
        return Pager(
            config = PagingConfig(pageSize = pageSize), // 每页19个回复
            initialKey = ((initialPage - 1) * pageSize),
            remoteMediator = ThreadRemoteMediator(
                sourceId = id,
                threadId = threadId.toString(),
                db = db,
                dataPolicy = policy,
                initialPage = initialPage,
                isPoOnly = isPoOnly,
                cdnManager = cdnManager,
                fetcher = { page ->
                    if (isPoOnly) {
                        nmbXdApi.po(threadId, page.toLong())
                    } else {
                        nmbXdApi.thread(threadId, page.toLong())
                    }
                }
            ),
            pagingSourceFactory = {
                if (isPoOnly) {
                    QueryPagingSource(
                        transacter = db.commentQueries,
                        context = Dispatchers.Default,
                        countQuery = db.commentQueries.countCommentsByTopicIdPoMode(
                            sourceId = id,
                            topicId = threadId.toString()
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsByTopicIdPoModeOffset(
                                sourceId = id,
                                topicId = threadId.toString(),
                                limit = limit,
                                offset = offset,
                            )
                        }
                    )
                } else if (poUserHash != null) {
                    // 只看指定PO (Old logic, maybe deprecated or specific use case)
                    QueryPagingSource(
                        transacter = db.commentQueries,
                        context = Dispatchers.Default,
                        countQuery =
                            db.commentQueries.countCommentsByTopicIdAndUserHash(
                                id,
                                threadId.toString(),
                                poUserHash
                            ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsByTopicIdAndUserHashOffset(
                                sourceId = id,
                                topicId = threadId.toString(),
                                userHash = poUserHash,
                                limit = limit,
                                offset = offset,
                            )
                        }
                    )
                } else {
                    // 查看全部
                    QueryPagingSource(
                        transacter = db.commentQueries,
                        context = Dispatchers.Default,
                        countQuery = db.commentQueries.countCommentsByTopicId(
                            id,
                            threadId.toString()
                        ),
                        queryProvider = { limit, offset ->
                            db.commentQueries.getCommentsByTopicIdOffset(
                                sourceId = id,
                                topicId = threadId.toString(),
                                limit = limit,
                                offset = offset
                            )
                        }
                    )
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain(db.imageQueries) }
        }
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

    suspend fun getSortedCookies(): List<Cookie> {
        return db.cookieQueries.getSortedCookies().asFlow().mapToList(Dispatchers.Default).first()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun insertCookie(alias: String, cookie: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val count =
            db.cookieQueries.countCookies().asFlow().mapToList(Dispatchers.Default).first().size
        db.cookieQueries.insertCookie(
            cookie = cookie,
            alias = alias,
            sort = count.toLong(),
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
            if (response is SaniouResponse.Success) {
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

    @OptIn(ExperimentalPagingApi::class)
    private fun createPager(
        fid: Long,
        policy: DataPolicy,
        initialPage: Int,
        fetcher: suspend (page: Int) -> SaniouResponse<List<Forum>>,
    ): Pager<Int, GetThreadsInForumOffset> {
        val pageSize = 20
        return Pager(
            config = PagingConfig(pageSize = pageSize),
            initialKey = initialPage,
            remoteMediator = ForumRemoteMediator(
                sourceId = id,
                fid = fid.toString(),
                db = db,
                dataPolicy = policy,
                initialPage = initialPage,
                cdnManager = cdnManager,
                fetcher = fetcher
            ),
            pagingSourceFactory = {
                QueryPagingSource(
                    transacter = db.topicQueries,
                    context = Dispatchers.Default,
                    countQuery = db.topicQueries.countTopicsByChannel(id, fid.toString()),
                    queryProvider = { limit, offset ->
                        db.topicQueries.getTopicsInChannelOffset(id, fid.toString(), limit, offset)
                    },
                )
            }
        )
    }

    suspend fun getThreadRepliesByPage(
        threadId: Long,
        page: Int,
    ): Result<List<ThreadReply>> {
        return try {
            when (val response = nmbXdApi.thread(threadId, page.toLong())) {
                is SaniouResponse.Success -> {
                    val thread = response.data
                    // 更新数据库
                    db.topicQueries.transaction {
                        db.topicQueries.upsertTopic(thread.toTable(id, page = page.toLong()))
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
                    Result.success(thread.replies)
                }

                is SaniouResponse.Error -> {
                    Result.failure(response.ex)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendThread(page: Int): Result<Thread> {
        return try {
            when (val response = nmbXdApi.getTrendThread(page = page.toLong())) {
                is SaniouResponse.Success -> {
                    val thread = response.data
                    // 保存到数据库
                    db.topicQueries.transaction {
                        db.topicQueries.upsertTopic(thread.toTable(id, page = page.toLong()))
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

                is SaniouResponse.Error -> Result.failure(response.ex)
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
        var predictedPage: Int? = null

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
                        // 如果缓存页已经不满 19 条，说明是最后一页，且数据还没生成
                        if (cachedCount < 19) {
                            return Result.success(null)
                        }

                        // 往后找，按每天 1 条估算
                        val daysNeeded = endDate.daysUntil(targetDate)
                        // 缓存页已经有的天数 (近似)
                        val countOnPage = startDate.daysUntil(endDate) + 1
                        val remainingSlots = 19 - countOnPage

                        if (daysNeeded <= remainingSlots) {
                            predictedPage = cachedPage
                        } else {
                            val remainingDays = daysNeeded - remainingSlots
                            val additionalPages = (remainingDays - 1) / 19 + 1
                            predictedPage = cachedPage + additionalPages
                        }
                    } else { // targetDate < startDate
                        // 往前找
                        val daysDiff = targetDate.daysUntil(startDate)
                        val pageOffset = (daysDiff - 1) / 19 + 1
                        predictedPage = cachedPage - pageOffset
                    }
                }
            } catch (e: Exception) {
                // 预测失败，回退
            }
        }

        // 2. 如果没有预测或预测页面无效，计算最后一页
        var currentPage = predictedPage ?: 0

        if (currentPage <= 0) {
            // 获取第一页以拿到总页数
            val firstPageThread = getTrendThread(page = 1).getOrElse { return Result.failure(it) }
            val replyCount = firstPageThread.replyCount
            val lastPage = ((replyCount - 1) / 19) + 1
            currentPage = lastPage.toInt()
        }

        // 3. 验证并精确查找 (无循环重试，仅做一次邻近跳转)
        val threadResult = getTrendThread(currentPage)
        val thread = threadResult.getOrElse { return Result.failure(it) }
        val replies = thread.replies

        // 空页异常处理
        if (replies.isEmpty()) {
            return Result.success(null)
        }

        // 解析当前页时间范围
        val firstReplyDate =
            ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(replies.first().now)
        val lastReplyDate =
            ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(replies.last().now)

        if (firstReplyDate == null || lastReplyDate == null) {
            return Result.failure(IllegalStateException("Date parsing failed for page $currentPage"))
        }

        // 更新 Anchor
        setTrendAnchor(currentPage, replies.first().now, replies.last().now, replies.size)

        // 检查是否命中当前页
        val targetReply = replies.find {
            ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(it.now) == targetDate
        }
        if (targetReply != null) {
            return Result.success(targetReply)
        }

        // 没命中，判断是向前还是向后
        if (targetDate > lastReplyDate) {
            // 目标日期在当前页之后
            if (replies.size < 19) {
                // 当前页未满，说明没有更新的数据了
                return Result.success(null)
            }
            // 尝试下一页
            val nextPage = currentPage + 1
            val nextThread = getTrendThread(nextPage).getOrElse { return Result.failure(it) }
            val nextReplies = nextThread.replies

            if (nextReplies.isNotEmpty()) {
                setTrendAnchor(
                    nextPage,
                    nextReplies.first().now,
                    nextReplies.last().now,
                    nextReplies.size
                )
                val found = nextReplies.find {
                    ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(it.now) == targetDate
                }
                return Result.success(found)
            }
        } else if (targetDate < firstReplyDate) {
            // 目标日期在当前页之前
            if (currentPage > 1) {
                val prevPage = currentPage - 1
                val prevThread = getTrendThread(prevPage).getOrElse { return Result.failure(it) }
                val prevReplies = prevThread.replies

                if (prevReplies.isNotEmpty()) {
                    setTrendAnchor(
                        prevPage,
                        prevReplies.first().now,
                        prevReplies.last().now,
                        prevReplies.size
                    )
                    val found = prevReplies.find {
                        ai.saniou.corecommon.utils.DateParser.parseDateFromNowString(it.now) == targetDate
                    }
                    return Result.success(found)
                }
            }
        }

        return Result.success(null)
    }

    suspend fun getLocalLatestReply(threadId: Long): DomainComment? {
        return db.commentQueries.getLastFiveComments(id, threadId.toString())
            .executeAsList()
            .maxByOrNull { it.id.toLong() }
            ?.toDomain(db.imageQueries)
    }

    suspend fun getLocalReplyByDate(
        threadId: Long,
        targetDate: kotlinx.datetime.LocalDate,
    ): DomainComment? {
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

    fun searchThreadsPager(query: String): Flow<PagingData<Post>> {
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

    fun searchRepliesPager(query: String): Flow<PagingData<DomainComment>> {
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

    fun getUserThreadsPager(userHash: String): Flow<PagingData<Post>> {
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

    fun getUserRepliesPager(userHash: String): Flow<PagingData<DomainComment>> {
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

internal fun saveNmbImage(
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

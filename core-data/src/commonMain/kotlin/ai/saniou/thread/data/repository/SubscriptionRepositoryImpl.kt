package ai.saniou.thread.data.repository

import ai.saniou.nmb.db.Database
import ai.saniou.thread.data.source.nmb.SqlDelightPagingSource
import ai.saniou.thread.data.source.nmb.SubscriptionRemoteMediator
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.domain.model.forum.Post
import ai.saniou.thread.domain.repository.SubscriptionRepository
import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SubscriptionRepositoryImpl(
    private val nmbXdApi: NmbXdApi,
    private val db: Database,
) : SubscriptionRepository {

    override suspend fun getActiveSubscriptionKey(): String? {
        return withContext(Dispatchers.IO) {
            db.keyValueQueries.getKeyValue("active_subscription_key").executeAsOneOrNull()?.value_
        }
    }

    override fun observeActiveSubscriptionKey(): Flow<String?> {
        return db.keyValueQueries.getKeyValue("active_subscription_key")
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.value_ }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getSubscriptionFeed(subscriptionKey: String): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = SubscriptionRemoteMediator(subscriptionKey, nmbXdApi, db),
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    countQueryProvider = {
                        db.subscriptionQueries.countSubscriptionsBySubscriptionKey(subscriptionKey)
                    },
                    transacter = db.subscriptionQueries,
                    context = Dispatchers.IO,
                    pageQueryProvider = { page ->
                        db.subscriptionQueries.selectSubscriptionThread(
                            subscriptionKey = subscriptionKey,
                            page = page.toLong()
                        )
                    }
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleSubscription(
        subscriptionKey: String,
        subscriptionId: String,
        isSubscribed: Boolean,
    ): Result<String> {
        return try {
            val message: String
            if (!isSubscribed) {
                message = nmbXdApi.addFeed(subscriptionKey, subscriptionId.toLong())
                db.subscriptionQueries.insertSubscription(
                    subscriptionKey = subscriptionKey,
                    threadId = subscriptionId.toLong(),
                    page = 1L,
                    subscriptionTime = Clock.System.now().epochSeconds,
                    isLocal = 1L
                )
            } else {
                message = nmbXdApi.delFeed(subscriptionKey, subscriptionId.toLong())
                db.subscriptionQueries.deleteSubscription(subscriptionKey, subscriptionId.toLong())
            }
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isSubscribed(subscriptionKey: String, postId: String): Flow<Boolean> {
        return db.subscriptionQueries.isSubscribed(subscriptionKey, postId.toLong())
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    override suspend fun syncLocalSubscriptions(subscriptionKey: String) {
        withContext(Dispatchers.IO) {
            val localSubscriptions =
                db.subscriptionQueries.getLocalSubscriptions(subscriptionKey).executeAsList()
            localSubscriptions.forEach {
                nmbXdApi.addFeed(subscriptionKey, it.threadId)
                db.subscriptionQueries.updateLocalFlag(subscriptionKey, it.threadId)
            }
        }
    }

    override suspend fun hasLocalSubscriptions(subscriptionKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            db.subscriptionQueries.countLocalSubscriptions(subscriptionKey).executeAsOne() > 0
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun addSubscriptionKey(key: String) {
        withContext(Dispatchers.IO) {
            db.subscriptionKeyQueries.insertSubscriptionKey(key, Clock.System.now().epochSeconds)
            setActiveSubscriptionKey(key)
        }
    }

    override fun getSubscriptionKeys(): Flow<List<String>> {
        return db.subscriptionKeyQueries.getAllSubscriptionKeys()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.key } }
    }

    override suspend fun setActiveSubscriptionKey(key: String) {
        withContext(Dispatchers.IO) {
            db.keyValueQueries.insertKeyValue("active_subscription_key", key)
        }
    }

    override fun generateRandomSubscriptionId(): String {
        return Uuid.random().toString()
    }
}

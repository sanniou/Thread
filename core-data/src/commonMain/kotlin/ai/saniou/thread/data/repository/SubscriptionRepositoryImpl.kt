package ai.saniou.thread.data.repository

import ai.saniou.thread.data.paging.DataPolicy
import ai.saniou.thread.data.paging.SqlDelightPagingSource
import ai.saniou.thread.data.source.nmb.SubscriptionRemoteMediator
import ai.saniou.thread.data.source.nmb.remote.NmbXdApi
import ai.saniou.thread.data.source.nmb.remote.dto.toDomain
import ai.saniou.thread.db.Database
import ai.saniou.thread.domain.model.forum.Topic
import ai.saniou.thread.domain.repository.SubscriptionRepository
import androidx.paging.*
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
            db.keyValueQueries.getKeyValue("active_subscription_key").executeAsOneOrNull()?.content
        }
    }

    override fun observeActiveSubscriptionKey(): Flow<String?> {
        return db.keyValueQueries.getKeyValue("active_subscription_key")
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.content }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getSubscriptionFeed(subscriptionKey: String): Flow<PagingData<Topic>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            remoteMediator = SubscriptionRemoteMediator(
                subscriptionKey,
                nmbXdApi,
                db,
                DataPolicy.CACHE_ELSE_NETWORK
            ),
            pagingSourceFactory = {
                SqlDelightPagingSource(
                    countQueryProvider = {
                        db.subscriptionQueries.countSubscriptionsBySubscriptionKey(subscriptionKey)
                    },
                    transacter = db.subscriptionQueries,
                    context = Dispatchers.IO,
                    pageQueryProvider = { page ->
                        db.subscriptionQueries.selectSubscriptionTopic(
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
                    sourceId = "nmb",
                    topicId = subscriptionId,
                    page = 1L,
                    subscriptionTime = Clock.System.now().toEpochMilliseconds(),
                    isLocal = 1L
                )
            } else {
                message = nmbXdApi.delFeed(subscriptionKey, subscriptionId.toLong())
                db.subscriptionQueries.deleteSubscription(subscriptionKey, "nmb", subscriptionId)
            }
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isSubscribed(subscriptionKey: String, postId: String): Flow<Boolean> {
        return db.subscriptionQueries.isSubscribed(subscriptionKey, "nmb", postId)
            .asFlow()
            .mapToOne(Dispatchers.Default)
    }

    override suspend fun syncLocalSubscriptions(subscriptionKey: String) {
        withContext(Dispatchers.IO) {
            val localSubscriptions =
                db.subscriptionQueries.getLocalSubscriptions(subscriptionKey).executeAsList()
            localSubscriptions.forEach {
                nmbXdApi.addFeed(subscriptionKey, it.topicId.toLong())
                db.subscriptionQueries.updateLocalFlag(subscriptionKey, "nmb", it.topicId)
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
            db.subscriptionKeyQueries.insertSubscriptionKey(key, Clock.System.now().toEpochMilliseconds())
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

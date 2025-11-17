package ai.saniou.nmb.domain

import ai.saniou.nmb.data.entity.Thread
import ai.saniou.nmb.data.entity.toThread
import ai.saniou.nmb.db.Database
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class GetThreadDetailUseCase(
    private val db: Database,
) {
    /**
     * 从数据库中获取一个帖子的信息流。
     * 当 RemoteMediator 更新数据库时，这个 Flow 会自动发射最新的数据。
     * @param id 帖子 ID
     * @return 包含帖子信息的 Flow
     */
    operator fun invoke(id: Long): Flow<Thread> =
        db.threadQueries.getThread(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .mapNotNull { it?.toThread() }
}

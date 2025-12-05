package ai.saniou.thread.data.sync.local

import ai.saniou.thread.domain.repository.SyncProvider

/**
 * 本地同步服务的骨架实现
 *
 * TODO: 实现本地文件的读写
 */
class LocalSyncProvider : SyncProvider {
    override val id: String = "local"

    override suspend fun export(data: String): Result<Unit> {
        println("Local export: $data")
        return Result.success(Unit)
    }

    override suspend fun import(): Result<String> {
        println("Local import")
        return Result.success("data from local file")
    }
}
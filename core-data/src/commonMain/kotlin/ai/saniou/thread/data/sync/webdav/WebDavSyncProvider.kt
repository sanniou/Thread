package ai.saniou.thread.data.sync.webdav

import ai.saniou.thread.domain.repository.SyncProvider

/**
 * WebDAV 同步服务的骨架实现
 *
 * TODO: 对接真实的 WebDAV 服务
 */
class WebDavSyncProvider : SyncProvider {
    override val id: String = "webdav"

    override suspend fun export(data: String): Result<Unit> {
        println("WebDAV export: $data")
        return Result.success(Unit)
    }

    override suspend fun import(): Result<String> {
        println("WebDAV import")
        return Result.success("data from webdav")
    }
}
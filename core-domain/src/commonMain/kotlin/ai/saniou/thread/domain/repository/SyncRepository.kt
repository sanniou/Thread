package ai.saniou.thread.domain.repository

/**
 * 同步服务的标准接口
 */
interface SyncProvider {
    /**
     * 同步服务的唯一标识，如 "webdav", "local"
     */
    val id: String

    /**
     * 导出数据
     */
    suspend fun export(data: String): Result<Unit>

    /**
     * 导入数据
     */
    suspend fun import(): Result<String>
}

/**
 * 同步仓库接口
 */
interface SyncRepository {
    /**
     * 使用指定的同步服务导出数据
     * @param providerId 同步服务ID
     * @param data 要导出的数据
     */
    suspend fun export(providerId: String, data: String): Result<Unit>

    /**
     * 使用指定的同步服务导入数据
     * @param providerId 同步服务ID
     */
    suspend fun import(providerId: String): Result<String>
}
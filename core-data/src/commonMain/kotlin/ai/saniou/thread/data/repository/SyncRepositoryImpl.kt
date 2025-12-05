package ai.saniou.thread.data.repository

import ai.saniou.thread.domain.repository.SyncProvider
import ai.saniou.thread.domain.repository.SyncRepository

class SyncRepositoryImpl(
    private val providers: Set<SyncProvider>
) : SyncRepository {

    private val providerMap by lazy { providers.associateBy { it.id } }

    override suspend fun export(providerId: String, data: String): Result<Unit> {
        val provider = providerMap[providerId] ?: return Result.failure(IllegalArgumentException("Provider not found: $providerId"))
        return provider.export(data)
    }

    override suspend fun import(providerId: String): Result<String> {
        val provider = providerMap[providerId] ?: return Result.failure(IllegalArgumentException("Provider not found: $providerId"))
        return provider.import()
    }
}
package ai.saniou.thread.domain.repository

import ai.saniou.thread.domain.model.content.ContentEdge
import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.RelatedContent
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface ContentGraphRepository {
    suspend fun upsert(edge: ContentEdge)
    suspend fun removeFor(reference: ContentReference)
    fun getRelated(reference: ContentReference): Flow<PagingData<RelatedContent>>
    suspend fun rebuild(reference: ContentReference): Int
}

package ai.saniou.thread.domain.usecase

import ai.saniou.thread.domain.model.Notice
import ai.saniou.thread.domain.repository.NoticeRepository
import kotlinx.coroutines.flow.Flow

class GetNoticeUseCase(
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(): Flow<Notice?> {
        noticeRepository.fetchAndCacheNotice()
        return noticeRepository.getLatestNotice()
    }
}

class MarkNoticeAsReadUseCase(
    private val noticeRepository: NoticeRepository
) {
    operator fun invoke(id: String) {
        noticeRepository.markAsRead(id)
    }
}

package ai.saniou.thread.domain.usecase.activity

import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.repository.ActivityCenterRepository
import ai.saniou.thread.domain.repository.ProductActionExecutor

class ObserveActivityCenterUseCase(private val repository: ActivityCenterRepository) {
    operator fun invoke() = repository.observe()
}

class ExecuteProductActionUseCase(private val executor: ProductActionExecutor) {
    val runningConflictKeys get() = executor.runningConflictKeys

    suspend operator fun invoke(request: ProductActionRequest) = executor.execute(request)
}

class ClearCompletedActivitiesUseCase(private val repository: ActivityCenterRepository) {
    suspend operator fun invoke() = repository.clearCompletedActions()
}

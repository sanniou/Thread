package ai.saniou.thread.feature.activity

import ai.saniou.thread.domain.model.activity.ProductActionDanger
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.usecase.activity.ClearCompletedActivitiesUseCase
import ai.saniou.thread.domain.usecase.activity.ExecuteProductActionUseCase
import ai.saniou.thread.domain.usecase.activity.ObserveActivityCenterUseCase
import ai.saniou.thread.feature.activity.ActivityCenterContract.Event
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivityCenterViewModel(
    observeActivityCenter: ObserveActivityCenterUseCase,
    private val executeProductAction: ExecuteProductActionUseCase,
    private val clearCompletedActivities: ClearCompletedActivitiesUseCase,
) : ScreenModel {
    private val mutableState = MutableStateFlow(ActivityCenterContract.State())
    val state = mutableState.asStateFlow()

    init {
        screenModelScope.launch {
            observeActivityCenter().collect { snapshot -> mutableState.update { it.copy(snapshot = snapshot) } }
        }
        screenModelScope.launch {
            executeProductAction.runningConflictKeys.collect { keys ->
                mutableState.update { it.copy(runningConflictKeys = keys) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.FilterChanged -> mutableState.update { it.copy(filter = event.value) }
            is Event.QueryChanged -> mutableState.update { it.copy(query = event.value.take(160)) }
            is Event.Execute -> {
                if (event.request.danger == ProductActionDanger.NORMAL) execute(event.request)
                else mutableState.update { it.copy(pendingDangerAction = event.request) }
            }
            Event.ConfirmDangerAction -> mutableState.value.pendingDangerAction?.let(::execute)
            Event.DismissDangerAction -> mutableState.update { it.copy(pendingDangerAction = null) }
            Event.ClearCompleted -> screenModelScope.launch {
                clearCompletedActivities()
                mutableState.update { it.copy(message = "已清理完成记录") }
            }
            Event.DismissOutput -> mutableState.update { it.copy(outputTitle = null, outputPayload = null) }
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun execute(request: ProductActionRequest) {
        if (request.conflictKey in mutableState.value.runningConflictKeys) return
        mutableState.update { it.copy(pendingDangerAction = null) }
        screenModelScope.launch {
            executeProductAction(request).fold(
                onSuccess = { result ->
                    mutableState.update {
                        it.copy(
                            message = result.message,
                            outputTitle = result.output?.let { request.type.name.replace('_', ' ') },
                            outputPayload = result.output,
                        )
                    }
                },
                onFailure = { error ->
                    mutableState.update { it.copy(message = error.message ?: "动作执行失败") }
                },
            )
        }
    }
}

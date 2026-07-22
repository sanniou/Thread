package ai.saniou.thread.feature.operations

import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.usecase.activity.ExecuteProductActionUseCase
import ai.saniou.thread.domain.usecase.operations.ObserveOperationsUseCase
import ai.saniou.thread.feature.operations.OperationsContract.Event
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_2f0f04da11
import thread.composeapp.generated.resources.error_action_exec_failed

class OperationsViewModel(
    observeOperations: ObserveOperationsUseCase,
    private val executeProductAction: ExecuteProductActionUseCase,
) : ScreenModel {
    private val mutableState = MutableStateFlow(OperationsContract.State())
    val state = mutableState.asStateFlow()

    init {
        screenModelScope.launch {
            observeOperations().collect { snapshot ->
                mutableState.update { it.copy(snapshot = snapshot) }
            }
        }
        screenModelScope.launch {
            executeProductAction.runningConflictKeys.collect { keys ->
                mutableState.update { state ->
                    state.copy(
                        workingSourceIds = state.snapshot.sources
                            .filter { "source:${it.id}" in keys }
                            .mapTo(mutableSetOf(), SourceHealth::id)
                    )
                }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.FilterChanged -> mutableState.update { it.copy(filter = event.filter) }
            is Event.Retry -> retry(event.source)
            is Event.ClearDiagnostic -> execute(
                ProductActionRequest(ProductActionType.CLEAR_SOURCE_DIAGNOSTIC, sourceId = event.sourceId)
            )
            Event.ExportDiagnostic -> exportDiagnostic()
            Event.DiagnosticDismissed -> mutableState.update { it.copy(diagnosticPayload = null) }
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun exportDiagnostic() {
        if (mutableState.value.isExportingDiagnostic) return
        screenModelScope.launch {
            mutableState.update { it.copy(isExportingDiagnostic = true) }
            executeProductAction(ProductActionRequest(ProductActionType.EXPORT_DIAGNOSTIC))
                .onSuccess { result ->
                    mutableState.update {
                        it.copy(
                            diagnosticPayload = result.output,
                            message = result.message,
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.message ?: getString(Res.string.s_2f0f04da11)) }
                }
            mutableState.update { it.copy(isExportingDiagnostic = false) }
        }
    }

    private fun retry(source: SourceHealth) {
        execute(
            ProductActionRequest(
                ProductActionType.REFRESH_SOURCE,
                sourceId = source.id,
                sourceKind = source.kind,
            )
        )
    }

    private fun execute(request: ProductActionRequest) {
        screenModelScope.launch {
            executeProductAction(request).fold(
                onSuccess = { result -> mutableState.update { it.copy(message = result.message) } },
                onFailure = { error -> mutableState.update { it.copy(message = error.message ?: getString(Res.string.error_action_exec_failed)) } },
            )
        }
    }
}

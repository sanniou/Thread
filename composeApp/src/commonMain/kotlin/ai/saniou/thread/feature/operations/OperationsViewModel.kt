package ai.saniou.thread.feature.operations

import ai.saniou.thread.domain.model.operations.ContentSourceKind
import ai.saniou.thread.domain.model.operations.SourceHealth
import ai.saniou.thread.domain.usecase.channel.FetchChannelsUseCase
import ai.saniou.thread.domain.usecase.operations.ClearSourceDiagnosticUseCase
import ai.saniou.thread.domain.usecase.operations.ObserveOperationsUseCase
import ai.saniou.thread.domain.usecase.operations.ExportDiagnosticUseCase
import ai.saniou.thread.domain.usecase.reader.RefreshFeedSourceUseCase
import ai.saniou.thread.feature.operations.OperationsContract.Event
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OperationsViewModel(
    observeOperations: ObserveOperationsUseCase,
    private val refreshFeedSource: RefreshFeedSourceUseCase,
    private val fetchChannels: FetchChannelsUseCase,
    private val clearSourceDiagnostic: ClearSourceDiagnosticUseCase,
    private val exportDiagnostic: ExportDiagnosticUseCase,
) : ScreenModel {
    private val mutableState = MutableStateFlow(OperationsContract.State())
    val state = mutableState.asStateFlow()

    init {
        screenModelScope.launch {
            observeOperations().collect { snapshot ->
                mutableState.update { it.copy(snapshot = snapshot) }
            }
        }
    }

    fun onEvent(event: Event) {
        when (event) {
            is Event.FilterChanged -> mutableState.update { it.copy(filter = event.filter) }
            is Event.Retry -> retry(event.source)
            is Event.ClearDiagnostic -> screenModelScope.launch { clearSourceDiagnostic(event.sourceId) }
            Event.ExportDiagnostic -> exportDiagnostic()
            Event.DiagnosticDismissed -> mutableState.update { it.copy(diagnosticPayload = null) }
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
        }
    }

    private fun exportDiagnostic() {
        if (mutableState.value.isExportingDiagnostic) return
        screenModelScope.launch {
            mutableState.update { it.copy(isExportingDiagnostic = true) }
            runCatching { exportDiagnostic.invoke() }
                .onSuccess { export ->
                    mutableState.update {
                        it.copy(
                            diagnosticPayload = export.payload,
                            message = "已生成 ${export.sourceCount} 个来源的脱敏诊断",
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.message ?: "诊断生成失败") }
                }
            mutableState.update { it.copy(isExportingDiagnostic = false) }
        }
    }

    private fun retry(source: SourceHealth) {
        if (source.id in mutableState.value.workingSourceIds) return
        screenModelScope.launch {
            mutableState.update { it.copy(workingSourceIds = it.workingSourceIds + source.id) }
            val result = when (source.kind) {
                ContentSourceKind.FORUM -> fetchChannels(source.id, forceRefresh = true)
                ContentSourceKind.READER -> refreshFeedSource(source.id, forceRefresh = true)
            }
            result.fold(
                onSuccess = {
                    mutableState.update { it.copy(message = "${source.name} 已刷新") }
                },
                onFailure = { error ->
                    mutableState.update { it.copy(message = error.message ?: "${source.name} 刷新失败") }
                },
            )
            mutableState.update { it.copy(workingSourceIds = it.workingSourceIds - source.id) }
        }
    }
}

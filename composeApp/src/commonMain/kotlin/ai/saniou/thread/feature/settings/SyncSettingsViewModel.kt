package ai.saniou.thread.feature.settings

import ai.saniou.thread.domain.model.sync.WebDavConfig
import ai.saniou.thread.domain.model.activity.ProductActionRequest
import ai.saniou.thread.domain.model.activity.ProductActionType
import ai.saniou.thread.domain.usecase.activity.ExecuteProductActionUseCase
import ai.saniou.thread.domain.refresh.RefreshStatus
import ai.saniou.thread.domain.usecase.reader.ObserveReaderSchedulerUseCase
import ai.saniou.thread.domain.usecase.refresh.ObserveRefreshDiagnosticsUseCase
import ai.saniou.thread.domain.usecase.sync.ObserveWebDavConfigUseCase
import ai.saniou.thread.domain.usecase.sync.SaveWebDavConfigUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncSettingsViewModel(
    private val executeProductAction: ExecuteProductActionUseCase,
    private val observeWebDavConfig: ObserveWebDavConfigUseCase,
    private val saveWebDavConfig: SaveWebDavConfigUseCase,
    private val observeReaderScheduler: ObserveReaderSchedulerUseCase,
    private val observeRefreshDiagnostics: ObserveRefreshDiagnosticsUseCase,
) : ScreenModel {
    private val _state = MutableStateFlow(SyncSettingsContract.State())
    val state: StateFlow<SyncSettingsContract.State> = _state.asStateFlow()

    init {
        screenModelScope.launch {
            observeWebDavConfig().collect { config ->
                _state.update {
                    it.copy(
                        endpoint = config?.endpoint.orEmpty(),
                        username = config?.username.orEmpty(),
                        password = config?.password.orEmpty(),
                    )
                }
            }
        }
        screenModelScope.launch {
            observeReaderScheduler().collect { scheduler -> _state.update { it.copy(scheduler = scheduler) } }
        }
        screenModelScope.launch {
            observeRefreshDiagnostics().collect { tasks ->
                _state.update {
                    it.copy(
                        activeRefreshCount = tasks.values.count { task -> task.status == RefreshStatus.RUNNING },
                        failedRefreshCount = tasks.values.count { task -> task.status == RefreshStatus.FAILED },
                    )
                }
            }
        }
    }

    fun onEvent(event: SyncSettingsContract.Event) {
        when (event) {
            is SyncSettingsContract.Event.EndpointChanged -> _state.update { it.copy(endpoint = event.value) }
            is SyncSettingsContract.Event.UsernameChanged -> _state.update { it.copy(username = event.value) }
            is SyncSettingsContract.Event.PasswordChanged -> _state.update { it.copy(password = event.value) }
            SyncSettingsContract.Event.SaveWebDav -> saveConfig()
            SyncSettingsContract.Event.ClearWebDav -> clearConfig()
            SyncSettingsContract.Event.ExportLocal -> exportLocal()
            SyncSettingsContract.Event.ShowImportLocal -> _state.update { it.copy(dialog = UserDataDialog(true)) }
            is SyncSettingsContract.Event.ImportLocal -> importLocal(event.payload)
            SyncSettingsContract.Event.BackupWebDav -> backup()
            SyncSettingsContract.Event.RestoreWebDav -> restore()
            SyncSettingsContract.Event.DismissDialog -> _state.update { it.copy(dialog = null) }
            SyncSettingsContract.Event.MessageShown -> _state.update { it.copy(message = null) }
        }
    }

    private fun saveConfig() = launchWork("WebDAV 配置已保存") {
        val current = _state.value
        saveWebDavConfig(WebDavConfig(current.endpoint.trim(), current.username, current.password))
    }

    private fun clearConfig() = launchWork("WebDAV 配置已清除") { saveWebDavConfig(null) }

    private fun exportLocal() {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.EXPORT_USER_DATA)).fold(
                onSuccess = { result ->
                    _state.update { it.copy(isWorking = false, dialog = UserDataDialog(false, result.output.orEmpty())) }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun importLocal(payload: String) {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.IMPORT_USER_DATA, payload = payload)).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(
                            isWorking = false,
                            dialog = null,
                            message = result.message,
                        )
                    }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun backup() {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.BACKUP_TO_WEBDAV)).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(isWorking = false, message = result.message)
                    }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun restore() {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            executeProductAction(ProductActionRequest(ProductActionType.RESTORE_FROM_WEBDAV)).fold(
                onSuccess = { result ->
                    _state.update {
                        it.copy(isWorking = false, message = result.message)
                    }
                },
                onFailure = ::showFailure,
            )
        }
    }

    private fun launchWork(successMessage: String, block: suspend () -> Unit) {
        screenModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            runCatching { block() }.fold(
                onSuccess = { _state.update { it.copy(isWorking = false, message = successMessage) } },
                onFailure = ::showFailure,
            )
        }
    }

    private fun showFailure(error: Throwable) {
        _state.update { it.copy(isWorking = false, message = error.message ?: "操作失败") }
    }
}

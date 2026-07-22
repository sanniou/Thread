package ai.saniou.forum.workflow.user

import ai.saniou.thread.domain.repository.AccountRepository
import ai.saniou.thread.domain.repository.SourceRepository
import ai.saniou.thread.domain.repository.UserContentRepository
import ai.saniou.thread.domain.source.ProfileEditRequest
import ai.saniou.thread.domain.usecase.user.FollowUserUseCase
import ai.saniou.thread.domain.usecase.user.GetUserRelationProfileUseCase
import ai.saniou.thread.domain.usecase.user.UnfollowUserUseCase
import ai.saniou.thread.domain.usecase.user.UpdateUserProfileUseCase
import ai.saniou.thread.domain.usecase.user.UploadUserPortraitUseCase
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserDetailViewModel(
    private val sourceId: String,
    private val userHash: String,
    private val userContentRepository: UserContentRepository,
    private val getUserRelationProfileUseCase: GetUserRelationProfileUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val uploadUserPortraitUseCase: UploadUserPortraitUseCase,
    private val sourceRepository: SourceRepository,
    private val accountRepository: AccountRepository,
) : ScreenModel {

    private val _state = MutableStateFlow(
        UserDetailContract.State(
            userHash = userHash
        )
    )
    val state = _state.asStateFlow()

    private val _effect = Channel<UserDetailContract.Effect>()
    val effect = _effect.receiveAsFlow()

    private var pendingPortraitBytes: ByteArray? = null
    private var pendingPortraitContentType: String = "application/octet-stream"

    init {
        loadData()
        loadRelation()
    }

    private fun loadData() {
        val threads = userContentRepository.getUserTopics(sourceId, userHash)
        val replies = userContentRepository.getUserComments(sourceId, userHash)
        _state.update {
            it.copy(
                topics = threads,
                comments = replies
            )
        }
    }

    private fun loadRelation() {
        screenModelScope.launch {
            val caps = sourceRepository.getSource(sourceId)?.capabilities
            val supportsFollow = caps?.supportsUserFollow == true
            val supportsEdit = caps?.supportsProfileEdit == true
            val currentUid = accountRepository.getCurrentAccount(sourceId).first()?.uid.orEmpty()
            val isSelf = currentUid.isNotBlank() && (
                currentUid == userHash ||
                    userHash.equals(currentUid, ignoreCase = true)
                )
            _state.update {
                it.copy(
                    supportsUserFollow = supportsFollow,
                    supportsProfileEdit = supportsEdit,
                    isSelf = isSelf,
                )
            }
            if (!supportsFollow && !supportsEdit) return@launch
            _state.update { it.copy(isProfileLoading = true) }
            getUserRelationProfileUseCase(sourceId, userHash)
                .onSuccess { profile ->
                    val selfByProfile = currentUid.isNotBlank() && profile.userId == currentUid
                    _state.update {
                        it.copy(
                            profile = profile,
                            isProfileLoading = false,
                            isSelf = it.isSelf || selfByProfile,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isProfileLoading = false,
                            actionMessage = error.message,
                        )
                    }
                }
        }
    }

    fun handleEvent(event: UserDetailContract.Event) {
        when (event) {
            is UserDetailContract.Event.SwitchTab -> {
                _state.update { it.copy(currentTab = event.tab) }
            }

            UserDetailContract.Event.Back -> {
                screenModelScope.launch {
                    _effect.send(UserDetailContract.Effect.NavigateBack)
                }
            }

            UserDetailContract.Event.ToggleFollow -> toggleFollow()
            UserDetailContract.Event.OpenEditProfile -> openEdit()
            UserDetailContract.Event.DismissEditProfile -> {
                clearPendingPortrait()
                _state.update {
                    it.copy(
                        isEditDialogOpen = false,
                        isSavingProfile = false,
                        isUploadingPortrait = false,
                    )
                }
            }
            is UserDetailContract.Event.EditNickNameChanged -> {
                _state.update { it.copy(editNickName = event.value) }
            }
            is UserDetailContract.Event.EditIntroChanged -> {
                _state.update { it.copy(editIntro = event.value) }
            }
            is UserDetailContract.Event.EditSexChanged -> {
                _state.update { it.copy(editSex = event.value) }
            }
            UserDetailContract.Event.SubmitEditProfile -> submitEdit()
            UserDetailContract.Event.PickPortrait -> {
                if (!canEditSelf() || state.value.isUploadingPortrait || state.value.isSavingProfile) return
                screenModelScope.launch {
                    _effect.send(UserDetailContract.Effect.RequestPortraitPicker)
                }
            }
            is UserDetailContract.Event.PortraitPicked -> {
                if (!canEditSelf()) return
                if (event.bytes.isEmpty()) {
                    _state.update { it.copy(actionMessage = "头像图片不能为空") }
                    return
                }
                if (event.bytes.size > MAX_PORTRAIT_BYTES) {
                    _state.update { it.copy(actionMessage = "头像不能超过 5 MB") }
                    return
                }
                pendingPortraitBytes = event.bytes
                pendingPortraitContentType = event.contentType
                _state.update {
                    it.copy(pendingPortraitFileName = event.fileName)
                }
            }
            UserDetailContract.Event.UploadPortrait -> uploadPortrait()
            UserDetailContract.Event.ClearPendingPortrait -> clearPendingPortrait()
            UserDetailContract.Event.ConsumeActionMessage -> {
                _state.update { it.copy(actionMessage = null) }
            }
        }
    }

    private fun openEdit() {
        val current = state.value
        if (!current.supportsProfileEdit || !current.isSelf) return
        val profile = current.profile
        clearPendingPortrait()
        _state.update {
            it.copy(
                isEditDialogOpen = true,
                editNickName = profile?.name.orEmpty(),
                editIntro = profile?.intro.orEmpty(),
                editSex = profile?.sex ?: 0,
            )
        }
    }

    private fun submitEdit() {
        val current = state.value
        if (!current.supportsProfileEdit || !current.isSelf || current.isSavingProfile) return
        val nick = current.editNickName.trim()
        if (nick.isBlank()) {
            _state.update { it.copy(actionMessage = "昵称不能为空") }
            return
        }
        screenModelScope.launch {
            _state.update { it.copy(isSavingProfile = true) }
            val request = ProfileEditRequest(
                nickName = nick,
                intro = current.editIntro.trim().take(500),
                sex = current.editSex.coerceIn(0, 2),
                birthdayTimeSec = current.profile?.birthdayTimeSec ?: 0L,
                birthdayShowStatus = current.profile?.birthdayShowStatus ?: false,
            )
            updateUserProfileUseCase(sourceId, request)
                .onSuccess { message ->
                    _state.update {
                        it.copy(
                            isSavingProfile = false,
                            isEditDialogOpen = false,
                            actionMessage = message,
                            profile = it.profile?.copy(
                                name = nick,
                                intro = request.intro.ifBlank { null },
                                sex = request.sex,
                            ),
                        )
                    }
                    clearPendingPortrait()
                    refreshProfile()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSavingProfile = false,
                            actionMessage = error.message,
                        )
                    }
                }
        }
    }

    private fun uploadPortrait() {
        val current = state.value
        if (!canEditSelf() || current.isUploadingPortrait) return
        val bytes = pendingPortraitBytes
        val fileName = current.pendingPortraitFileName
        if (bytes == null || fileName.isNullOrBlank()) {
            _state.update { it.copy(actionMessage = "请先选择头像图片") }
            return
        }
        screenModelScope.launch {
            _state.update { it.copy(isUploadingPortrait = true) }
            uploadUserPortraitUseCase(
                sourceId = sourceId,
                fileName = fileName,
                bytes = bytes,
                contentType = pendingPortraitContentType,
            ).onSuccess { message ->
                clearPendingPortrait()
                _state.update {
                    it.copy(
                        isUploadingPortrait = false,
                        actionMessage = message,
                    )
                }
                refreshProfile()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isUploadingPortrait = false,
                        actionMessage = error.message,
                    )
                }
            }
        }
    }

    private fun refreshProfile() {
        screenModelScope.launch {
            getUserRelationProfileUseCase(sourceId, userHash)
                .onSuccess { profile ->
                    _state.update { it.copy(profile = profile) }
                }
        }
    }

    private fun toggleFollow() {
        val current = state.value
        if (!current.supportsUserFollow || current.isFollowBusy || current.isSelf) return
        screenModelScope.launch {
            _state.update { it.copy(isFollowBusy = true) }
            val following = current.profile?.isFollowing == true
            val result = if (following) {
                unfollowUserUseCase(sourceId, userHash)
            } else {
                followUserUseCase(sourceId, userHash)
            }
            result
                .onSuccess { message ->
                    _state.update {
                        val profile = it.profile?.copy(isFollowing = !following)
                        it.copy(
                            profile = profile,
                            isFollowBusy = false,
                            actionMessage = message,
                        )
                    }
                    refreshProfile()
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isFollowBusy = false,
                            actionMessage = error.message,
                        )
                    }
                }
        }
    }

    private fun canEditSelf(): Boolean {
        val current = state.value
        return current.supportsProfileEdit && current.isSelf
    }

    private fun clearPendingPortrait() {
        pendingPortraitBytes = null
        pendingPortraitContentType = "application/octet-stream"
        _state.update { it.copy(pendingPortraitFileName = null) }
    }

    private companion object {
        const val MAX_PORTRAIT_BYTES = 5 * 1024 * 1024
    }
}

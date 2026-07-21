package ai.saniou.thread.feature.social

import ai.saniou.thread.domain.model.content.ContentReference
import ai.saniou.thread.domain.model.content.ContentReferenceKind
import ai.saniou.thread.domain.model.social.SocialInteraction
import ai.saniou.thread.domain.model.social.SocialPost
import ai.saniou.thread.domain.repository.ContentGraphRepository
import ai.saniou.thread.domain.usecase.social.GetSocialPostUseCase
import ai.saniou.thread.domain.usecase.social.InteractWithSocialPostUseCase
import ai.saniou.thread.feature.social.SocialDetailContract.Event
import ai.saniou.thread.feature.social.SocialDetailContract.State
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import thread.composeapp.generated.resources.Res
import thread.composeapp.generated.resources.s_51884bb3cd
import thread.composeapp.generated.resources.s_acba1d0374
import thread.composeapp.generated.resources.s_f6eafd8b6d

class SocialDetailViewModel(
    private val sourceId: String,
    private val postId: String,
    private val getSocialPost: GetSocialPostUseCase,
    private val interactWithSocialPost: InteractWithSocialPostUseCase,
    private val contentGraphRepository: ContentGraphRepository,
) : ScreenModel {
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: Event) {
        when (event) {
            Event.Retry -> load()
            Event.MessageShown -> mutableState.update { it.copy(message = null) }
            is Event.Interact -> interact(event.interaction, event.enabled)
        }
    }

    private fun load() {
        screenModelScope.launch {
            mutableState.update { it.copy(isLoading = true, error = null) }
            runCatching { getSocialPost(sourceId, postId) }.fold(
                onSuccess = { post ->
                    if (post == null) {
                        mutableState.update {
                            it.copy(isLoading = false, error = getString(Res.string.s_acba1d0374))
                        }
                    } else {
                        mutableState.update { it.copy(isLoading = false, post = post, error = null) }
                        runCatching {
                            contentGraphRepository.rebuild(
                                ContentReference(ContentReferenceKind.SOCIAL_POST, postId, sourceId),
                            )
                        }
                    }
                },
                onFailure = { error ->
                    mutableState.update {
                        it.copy(isLoading = false, error = error.message ?: getString(Res.string.s_f6eafd8b6d))
                    }
                },
            )
        }
    }

    private fun interact(interaction: SocialInteraction, enabled: Boolean) {
        val current = mutableState.value.post ?: return
        screenModelScope.launch {
            interactWithSocialPost(current, interaction, enabled).fold(
                onSuccess = { updated ->
                    mutableState.update { it.copy(post = updated, message = null) }
                },
                onFailure = { error ->
                    mutableState.update {
                        it.copy(message = error.message ?: getString(Res.string.s_51884bb3cd))
                    }
                },
            )
        }
    }
}

interface SocialDetailContract {
    data class State(
        val isLoading: Boolean = true,
        val post: SocialPost? = null,
        val error: String? = null,
        val message: String? = null,
    )

    sealed interface Event {
        data object Retry : Event
        data object MessageShown : Event
        data class Interact(val interaction: SocialInteraction, val enabled: Boolean) : Event
    }
}

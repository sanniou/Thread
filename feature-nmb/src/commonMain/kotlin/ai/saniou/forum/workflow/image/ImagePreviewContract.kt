package ai.saniou.forum.workflow.image

interface ImagePreviewContract {
    data class State(
        val images: List<ImageInfo> = emptyList(),
        val initialIndex: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val saveError: String? = null,
        val endReached: Boolean = false,
    )

    sealed interface Event {
        data object LoadMore : Event
        data class SaveImage(val index: Int) : Event
    }

    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
    }
}

val nmbImagePreviewModule = org.kodein.di.DI.Module("NmbImagePreviewModule") {
    import(imagePreviewViewModelModule)
}

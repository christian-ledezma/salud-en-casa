package bo.saludencasa.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ProfessionalServicesResult
import bo.saludencasa.features.catalog.domain.usecase.GetProfessionalServicesUseCase
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetPublicProfileUseCase
import bo.saludencasa.features.reviews.domain.model.ProfessionalReview
import bo.saludencasa.features.reviews.domain.model.ProfessionalReviewsPageResult
import bo.saludencasa.features.reviews.domain.model.ReviewError
import bo.saludencasa.features.reviews.domain.usecase.GetProfessionalReviewsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ServicesState {
    data object Loading : ServicesState

    data object Empty : ServicesState

    data class Content(
        val services: List<ProfessionalService>,
    ) : ServicesState

    data class Failed(
        val error: CatalogError,
    ) : ServicesState
}

sealed interface ReviewsState {
    data object Loading : ReviewsState

    data object Empty : ReviewsState

    data class Content(
        val reviews: List<ProfessionalReview>,
        val loadingMore: Boolean,
        val endReached: Boolean,
        val notice: ReviewError?,
    ) : ReviewsState

    data class Failed(
        val error: ReviewError,
    ) : ReviewsState
}

sealed interface PublicProfileUiState {
    data object Loading : PublicProfileUiState

    data object NotPublished : PublicProfileUiState

    // The two sections live inside Content and not beside it because neither
    // means anything without a published profile: the directory is what says
    // this person exists publicly at all.
    data class Content(
        val profile: PublicProfile,
        val services: ServicesState = ServicesState.Loading,
        val reviews: ReviewsState = ReviewsState.Loading,
    ) : PublicProfileUiState

    data class Failed(
        val error: ProfileError,
    ) : PublicProfileUiState
}

class PublicProfileViewModel(
    private val professionalId: String,
    private val getPublicProfile: GetPublicProfileUseCase,
    private val getProfessionalServices: GetProfessionalServicesUseCase,
    private val getProfessionalReviews: GetProfessionalReviewsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow<PublicProfileUiState>(PublicProfileUiState.Loading)

    val uiState: StateFlow<PublicProfileUiState> = state.asStateFlow()

    private var loadMoreJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadMoreJob?.cancel()
        state.value = PublicProfileUiState.Loading
        viewModelScope.launch {
            val result = getPublicProfile(professionalId)
            state.value =
                when (result) {
                    is PublicProfileResult.Success -> PublicProfileUiState.Content(result.profile)
                    PublicProfileResult.NotPublished -> PublicProfileUiState.NotPublished
                    is PublicProfileResult.Failure -> PublicProfileUiState.Failed(result.error)
                }

            // The two sections are asked for only once the directory answered:
            // a profile that is not published has no services and no comments
            // to show, and asking anyway would spend two requests to draw
            // nothing. Side by side and not one after the other, because
            // neither read depends on the other and the services one already
            // costs two round trips of its own.
            if (result is PublicProfileResult.Success) {
                coroutineScope {
                    launch { loadServices() }
                    launch { loadFirstReviewsPage() }
                }
            }
        }
    }

    fun onRetryServices() {
        viewModelScope.launch { loadServices() }
    }

    fun onRetryReviews() {
        viewModelScope.launch { loadFirstReviewsPage() }
    }

    fun onReachedEnd() {
        val content = state.value as? PublicProfileUiState.Content ?: return
        val reviews = content.reviews as? ReviewsState.Content ?: return
        if (reviews.endReached || reviews.loadingMore) return

        // The notice is cleared on the way in: this is also the retry of the
        // page that failed, and the list only asks for the next page by itself
        // while there is no notice to read.
        updateReviews { it.copy(loadingMore = true, notice = null) }
        loadMoreJob =
            viewModelScope.launch {
                val result = getProfessionalReviews(professionalId, offset = reviews.reviews.size)
                updateReviews { current ->
                    when (result) {
                        is ProfessionalReviewsPageResult.Loaded -> {
                            current.copy(
                                // A repeated key throws out of a LazyColumn
                                // and takes the screen down, and a tie in the
                                // instant can bring a row back on the next
                                // offset.
                                reviews =
                                    (current.reviews + result.reviews)
                                        .distinctBy(ProfessionalReview::id),
                                loadingMore = false,
                                endReached = result.endReached,
                                notice = null,
                            )
                        }

                        is ProfessionalReviewsPageResult.Failure -> {
                            current.copy(loadingMore = false, notice = result.error)
                        }
                    }
                }
            }
    }

    private suspend fun loadServices() {
        updateContent { it.copy(services = ServicesState.Loading) }
        val services =
            when (val result = getProfessionalServices(professionalId)) {
                is ProfessionalServicesResult.Loaded -> {
                    if (result.services.isEmpty()) {
                        ServicesState.Empty
                    } else {
                        ServicesState.Content(result.services)
                    }
                }

                is ProfessionalServicesResult.Failure -> {
                    ServicesState.Failed(result.error)
                }
            }
        updateContent { it.copy(services = services) }
    }

    private suspend fun loadFirstReviewsPage() {
        updateContent { it.copy(reviews = ReviewsState.Loading) }
        val reviews =
            when (val result = getProfessionalReviews(professionalId, offset = 0)) {
                is ProfessionalReviewsPageResult.Loaded -> {
                    if (result.reviews.isEmpty()) {
                        ReviewsState.Empty
                    } else {
                        ReviewsState.Content(
                            reviews = result.reviews,
                            loadingMore = false,
                            endReached = result.endReached,
                            notice = null,
                        )
                    }
                }

                is ProfessionalReviewsPageResult.Failure -> {
                    ReviewsState.Failed(result.error)
                }
            }
        updateContent { it.copy(reviews = reviews) }
    }

    private fun updateContent(change: (PublicProfileUiState.Content) -> PublicProfileUiState.Content) {
        state.update { current ->
            if (current is PublicProfileUiState.Content) change(current) else current
        }
    }

    private fun updateReviews(change: (ReviewsState.Content) -> ReviewsState.Content) {
        updateContent { content ->
            val reviews = content.reviews
            if (reviews is ReviewsState.Content) content.copy(reviews = change(reviews)) else content
        }
    }
}

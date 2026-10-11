package bo.saludencasa.features.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.domain.model.ServiceTypesResult
import bo.saludencasa.features.catalog.domain.usecase.GetServiceTypeCatalogUseCase
import bo.saludencasa.features.search.domain.model.NearbyProfessional
import bo.saludencasa.features.search.domain.model.NearbyProfessionalsPageResult
import bo.saludencasa.features.search.domain.model.SearchCriteria
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.model.SearchOrigin
import bo.saludencasa.features.search.domain.model.SearchOriginResult
import bo.saludencasa.features.search.domain.model.SearchRadius
import bo.saludencasa.features.search.domain.usecase.GetSearchOriginUseCase
import bo.saludencasa.features.search.domain.usecase.SearchNearbyProfessionalsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ResultsMode {
    MAP,
    LIST,
}

sealed interface SearchResultsState {
    data object Searching : SearchResultsState

    // The first criterion of HU-11 begins with a registered address. Without
    // one there is nothing to search around, and the only way forward is to go
    // and register one.
    data object NoPrimaryAddress : SearchResultsState

    data object Empty : SearchResultsState

    data class Content(
        val professionals: List<NearbyProfessional>,
        val loadingMore: Boolean,
        val endReached: Boolean,
        val notice: SearchError?,
    ) : SearchResultsState

    data class Failed(
        val error: SearchError,
    ) : SearchResultsState
}

// The criteria and the mode live beside the results and not inside each case,
// the shape DocumentReviewQueueUiState established: the chips stay on screen
// while the search runs, comes back empty or fails.
data class ProfessionalSearchUiState(
    val criteria: SearchCriteria = SearchCriteria(),
    val mode: ResultsMode = ResultsMode.MAP,
    val origin: SearchOrigin? = null,
    val serviceTypes: List<ServiceType> = emptyList(),
    val selectedId: String? = null,
    // The twelve service types are a list that opens, not a row of chips: a
    // chip cannot show a name of thirty-four characters at a 200 % font scale
    // (docs/decisions.md, 2026-10-09). It lives in the state so the preview and
    // the layout test can open it.
    val typeFilterExpanded: Boolean = false,
    val results: SearchResultsState = SearchResultsState.Searching,
)

class ProfessionalSearchViewModel(
    private val getSearchOrigin: GetSearchOriginUseCase,
    private val searchNearbyProfessionals: SearchNearbyProfessionalsUseCase,
    private val getServiceTypeCatalog: GetServiceTypeCatalogUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(ProfessionalSearchUiState())
    val uiState: StateFlow<ProfessionalSearchUiState> = state.asStateFlow()

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var catalogLoaded = false

    // Called on every resume rather than from init, because the origin is
    // edited on another screen: coming back from registering an address has to
    // search again, and coming back from a professional's profile must not
    // blank the list that was already there.
    fun refresh() {
        searchJob?.cancel()
        // The page in flight is NOT cancelled here. A resume that finds the same
        // origin changes nothing, and cancelling that page without putting its
        // flag down leaves the footer spinning for ever, because the list only
        // asks again while the flag is down. It is cancelled below, where the
        // list it would land on is actually being replaced.
        searchJob =
            viewModelScope.launch {
                loadServiceTypes()

                when (val result = getSearchOrigin()) {
                    is SearchOriginResult.Found -> {
                        val previous = state.value.origin
                        state.update { it.copy(origin = result.origin) }
                        if (previous != result.origin || state.value.results !is SearchResultsState.Content) {
                            loadMoreJob?.cancel()
                            runSearch(reset = true)
                        }
                    }

                    is SearchOriginResult.Failure -> {
                        loadMoreJob?.cancel()
                        state.update {
                            it.copy(
                                origin = null,
                                selectedId = null,
                                results =
                                    if (result.error == SearchError.NoPrimaryAddress) {
                                        SearchResultsState.NoPrimaryAddress
                                    } else {
                                        SearchResultsState.Failed(result.error)
                                    },
                            )
                        }
                    }
                }
            }
    }

    fun onRadiusChange(radius: SearchRadius) {
        if (state.value.criteria.radius == radius) return
        updateCriteria { it.copy(radius = radius) }
    }

    fun onTypeFilterToggle() {
        state.update { it.copy(typeFilterExpanded = !it.typeFilterExpanded) }
    }

    fun onServiceTypeChange(serviceTypeId: String?) {
        state.update { it.copy(typeFilterExpanded = false) }
        if (state.value.criteria.serviceTypeId == serviceTypeId) return
        updateCriteria { it.copy(serviceTypeId = serviceTypeId) }
    }

    fun onAvailabilityChange(availableNowOnly: Boolean) {
        if (state.value.criteria.availableNowOnly == availableNowOnly) return
        updateCriteria { it.copy(availableNowOnly = availableNowOnly) }
    }

    fun onClearFilters() {
        updateCriteria { SearchCriteria(radius = it.radius) }
    }

    fun onWidenRadius() {
        val wider =
            SearchRadius.entries.firstOrNull { it.km > state.value.criteria.radius.km } ?: return
        onRadiusChange(wider)
    }

    // The one criterion that must not search again: both modes draw the same
    // results, so reloading here would spend a request and blink the list for
    // nothing.
    fun onModeChange(mode: ResultsMode) {
        state.update { it.copy(mode = mode) }
    }

    fun onProfessionalSelected(professionalId: String?) {
        state.update { it.copy(selectedId = professionalId) }
    }

    fun onRetry() {
        if (state.value.origin == null) refresh() else restartSearch()
    }

    fun onReachedEnd() {
        val content = state.value.results as? SearchResultsState.Content ?: return
        if (content.endReached || content.loadingMore) return
        val origin = state.value.origin ?: return

        // The notice is cleared on the way in: this is also the retry of the
        // page that failed, and the screen only asks for the next page by
        // itself while there is no notice to read.
        state.update { it.copy(results = content.copy(loadingMore = true, notice = null)) }
        loadMoreJob =
            viewModelScope.launch {
                val criteria = state.value.criteria
                val result =
                    searchNearbyProfessionals(
                        origin.coordinate,
                        criteria,
                        offset = content.professionals.size,
                    )
                val current = state.value.results as? SearchResultsState.Content ?: return@launch

                state.update {
                    it.copy(
                        results =
                            when (result) {
                                is NearbyProfessionalsPageResult.Loaded -> {
                                    current.copy(
                                        // Two rows with the same key freeze a
                                        // LazyColumn, and a tie in distance can
                                        // bring a row back on the next offset.
                                        professionals =
                                            (current.professionals + result.professionals)
                                                .distinctBy(NearbyProfessional::professionalId),
                                        loadingMore = false,
                                        endReached = result.endReached,
                                        notice = null,
                                    )
                                }

                                is NearbyProfessionalsPageResult.Failure -> {
                                    current.copy(loadingMore = false, notice = result.error)
                                }
                            },
                    )
                }
            }
    }

    private fun updateCriteria(change: (SearchCriteria) -> SearchCriteria) {
        state.update { it.copy(criteria = change(it.criteria)) }
        restartSearch()
    }

    private fun restartSearch() {
        // A criterion can change while the origin is still being resolved, and
        // cancelling that job to search without an origin leaves the screen on
        // its spinner for ever: runSearch has nothing to search around. Resolving
        // it again ends in the same search, with the criteria as they are now.
        if (state.value.origin == null) {
            refresh()
            return
        }

        searchJob?.cancel()
        loadMoreJob?.cancel()
        searchJob = viewModelScope.launch { runSearch(reset = true) }
    }

    // Both jobs are cancelled before this runs: a reply to the criteria the
    // chips no longer describe, landing last, would fill the list with rows
    // that answer a question nobody is asking any more.
    private suspend fun runSearch(reset: Boolean) {
        val origin = state.value.origin ?: return
        if (reset) {
            state.update { it.copy(selectedId = null, results = SearchResultsState.Searching) }
        }

        val criteria = state.value.criteria
        state.update {
            it.copy(
                results =
                    when (val result = searchNearbyProfessionals(origin.coordinate, criteria, offset = 0)) {
                        is NearbyProfessionalsPageResult.Loaded -> {
                            if (result.professionals.isEmpty()) {
                                SearchResultsState.Empty
                            } else {
                                SearchResultsState.Content(
                                    professionals = result.professionals,
                                    loadingMore = false,
                                    endReached = result.endReached,
                                    notice = null,
                                )
                            }
                        }

                        is NearbyProfessionalsPageResult.Failure -> {
                            SearchResultsState.Failed(result.error)
                        }
                    },
            )
        }
    }

    // The chip row is a filter and not a precondition: a catalog that cannot be
    // read leaves the search working without it, rather than taking the whole
    // screen down with it.
    private suspend fun loadServiceTypes() {
        if (catalogLoaded) return

        // An `if` and not an exhaustive `when`: the failure branch would have
        // nothing to do, and a branch that evaluates to Unit is what the
        // compiler reports as an unused expression.
        val result = getServiceTypeCatalog()
        if (result is ServiceTypesResult.Loaded) {
            catalogLoaded = true
            state.update { it.copy(serviceTypes = result.types) }
        }
    }
}

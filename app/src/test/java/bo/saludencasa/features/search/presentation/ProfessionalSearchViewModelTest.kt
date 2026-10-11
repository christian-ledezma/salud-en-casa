@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.search.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.usecase.GetServiceTypeCatalogUseCase
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.search.FakeProfessionalSearchRepository
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.model.SearchRadius
import bo.saludencasa.features.search.domain.usecase.GetSearchOriginUseCase
import bo.saludencasa.features.search.domain.usecase.SearchNearbyProfessionalsUseCase
import bo.saludencasa.features.search.nearbyProfessional
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfessionalSearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val home = address(id = "home", alias = "Casa", coordinate = coordinate(-17.3835, -66.1568))
    private val addresses = FakeAddressRepository(listResult = AddressListResult.Success(listOf(home)))
    private val search = FakeProfessionalSearchRepository()
    private val catalog =
        FakeCatalogRepository(catalog = listOf(generalConsultation, physiotherapy))

    private fun viewModel(): ProfessionalSearchViewModel =
        ProfessionalSearchViewModel(
            GetSearchOriginUseCase(GetMyAddressesUseCase(addresses)),
            SearchNearbyProfessionalsUseCase(search),
            GetServiceTypeCatalogUseCase(catalog),
        )

    private fun page(count: Int) = List(count) { index -> nearbyProfessional(id = "p$index", distanceM = 100 * index) }

    private fun TestScope.opened(): ProfessionalSearchViewModel {
        val viewModel = viewModel()
        viewModel.refresh()
        advanceUntilIdle()
        return viewModel
    }

    private fun content(viewModel: ProfessionalSearchViewModel): SearchResultsState.Content =
        viewModel.uiState.value.results as SearchResultsState.Content

    @Test
    fun searchesAroundThePrimaryAddressWithTheDefaultRadius() =
        runTest {
            search.professionals = page(3)

            val viewModel = opened()

            val request = search.requests.single()
            assertEquals(coordinate(-17.3835, -66.1568), request.origin)
            assertEquals(SearchRadius.DEFAULT, request.criteria.radius)
            assertEquals(3, content(viewModel).professionals.size)
            assertEquals(listOf(generalConsultation, physiotherapy), viewModel.uiState.value.serviceTypes)
        }

    // Searching around a fallback point would show results near a place the
    // person never declared, and they would have no way of knowing.
    @Test
    fun aPersonWithoutAPrimaryAddressIsNotSearchedForAnywhere() =
        runTest {
            addresses.listResult = AddressListResult.Success(listOf(address(isPrimary = false)))

            val viewModel = opened()

            assertEquals(SearchResultsState.NoPrimaryAddress, viewModel.uiState.value.results)
            assertTrue(search.requests.isEmpty())
        }

    // Keeping the offset across a criteria change pastes a page of the old
    // radius on top of the new one, and the nearest results of the new search
    // are the ones that get skipped.
    @Test
    fun changingTheRadiusStartsOverFromTheFirstPage() =
        runTest {
            search.professionals = page(SearchNearbyProfessionalsUseCase.PAGE_SIZE)
            val viewModel = opened()
            viewModel.onReachedEnd()
            advanceUntilIdle()

            viewModel.onRadiusChange(SearchRadius.KM_10)
            advanceUntilIdle()

            val last = search.requests.last()
            assertEquals(0, last.offset)
            assertEquals(SearchRadius.KM_10, last.criteria.radius)
        }

    // The reply to the radius that was abandoned must never land: it would fill
    // the list with rows the chips no longer describe.
    @Test
    fun aReplyToThePreviousRadiusNeverOverwritesTheCurrentOne() =
        runTest {
            search.responder = { request ->
                if (request.criteria.radius == SearchRadius.KM_2) {
                    listOf(nearbyProfessional(id = "near"))
                } else {
                    listOf(nearbyProfessional(id = "far"))
                }
            }
            val viewModel = opened()

            val gate = CompletableDeferred<Unit>()
            search.gate = gate
            viewModel.onRadiusChange(SearchRadius.KM_2)
            advanceUntilIdle()

            search.gate = null
            viewModel.onRadiusChange(SearchRadius.KM_10)
            advanceUntilIdle()

            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("far"), content(viewModel).professionals.map { it.professionalId })
            assertEquals(SearchRadius.KM_10, viewModel.uiState.value.criteria.radius)
        }

    // Two rows with the same key freeze a LazyColumn, and a tie in distance can
    // bring a row back at the next offset.
    @Test
    fun loadingTheNextPageAppendsWithoutRepeatingARow() =
        runTest {
            val first = page(SearchNearbyProfessionalsUseCase.PAGE_SIZE)
            search.professionals = first + first.last() + nearbyProfessional(id = "new")
            val viewModel = opened()

            viewModel.onReachedEnd()
            advanceUntilIdle()

            val ids = content(viewModel).professionals.map { it.professionalId }
            assertEquals(ids.distinct(), ids)
            assertTrue(ids.contains("new"))
        }

    @Test
    fun reachingTheEndTwiceWhileThePageLoadsAsksForItOnce() =
        runTest {
            search.professionals = page(SearchNearbyProfessionalsUseCase.PAGE_SIZE + 1)
            val viewModel = opened()

            viewModel.onReachedEnd()
            viewModel.onReachedEnd()
            advanceUntilIdle()

            assertEquals(2, search.requests.size)
        }

    // Dropping the rows because the last page failed throws away everything the
    // person was reading over a problem at the bottom of the list.
    @Test
    fun aFailedNextPageKeepsTheRowsAndCanBeRetried() =
        runTest {
            search.professionals = page(SearchNearbyProfessionalsUseCase.PAGE_SIZE + 1)
            val viewModel = opened()

            search.failure = SearchError.NetworkUnavailable
            viewModel.onReachedEnd()
            advanceUntilIdle()

            val failed = content(viewModel)
            assertEquals(SearchNearbyProfessionalsUseCase.PAGE_SIZE, failed.professionals.size)
            assertEquals(SearchError.NetworkUnavailable, failed.notice)
            assertFalse(failed.loadingMore)

            search.failure = null
            viewModel.onReachedEnd()
            advanceUntilIdle()

            val retried = content(viewModel)
            assertNull(retried.notice)
            assertEquals(SearchNearbyProfessionalsUseCase.PAGE_SIZE + 1, retried.professionals.size)
        }

    // Both modes draw the same results, so a request here is a request too many
    // and a blink nobody asked for.
    @Test
    fun switchingBetweenTheMapAndTheListDoesNotSearchAgain() =
        runTest {
            search.professionals = page(2)
            val viewModel = opened()

            viewModel.onModeChange(ResultsMode.LIST)
            advanceUntilIdle()

            assertEquals(1, search.requests.size)
            assertEquals(ResultsMode.LIST, viewModel.uiState.value.mode)
            assertEquals(2, content(viewModel).professionals.size)
        }

    // The chip row is a filter, not a precondition. Taking the screen down
    // because the catalog failed would deny the search over a row of chips.
    @Test
    fun aCatalogThatCannotBeReadDoesNotBlockTheSearch() =
        runTest {
            catalog.readFailure = CatalogError.NetworkUnavailable
            search.professionals = page(1)

            val viewModel = opened()

            assertTrue(
                viewModel.uiState.value.serviceTypes
                    .isEmpty(),
            )
            assertEquals(1, content(viewModel).professionals.size)
        }

    @Test
    fun choosingATypeAndAvailabilityNarrowsWhatIsAsked() =
        runTest {
            search.professionals = page(1)
            val viewModel = opened()

            viewModel.onServiceTypeChange(physiotherapy.id)
            advanceUntilIdle()
            viewModel.onAvailabilityChange(true)
            advanceUntilIdle()

            val last = search.requests.last()
            assertEquals(physiotherapy.id, last.criteria.serviceTypeId)
            assertTrue(last.criteria.availableNowOnly)
            assertTrue(viewModel.uiState.value.criteria.isNarrowed)
        }

    @Test
    fun clearingTheFiltersKeepsTheRadiusAndDropsTheRest() =
        runTest {
            search.professionals = page(1)
            val viewModel = opened()
            viewModel.onRadiusChange(SearchRadius.KM_10)
            viewModel.onServiceTypeChange(physiotherapy.id)
            viewModel.onAvailabilityChange(true)
            advanceUntilIdle()

            viewModel.onClearFilters()
            advanceUntilIdle()

            val criteria = viewModel.uiState.value.criteria
            assertEquals(SearchRadius.KM_10, criteria.radius)
            assertNull(criteria.serviceTypeId)
            assertFalse(criteria.availableNowOnly)
        }

    // The way out of an empty result, and it has to stop: offering an eleventh
    // kilometre that the chips cannot show would be a button that does nothing.
    @Test
    fun wideningTheRadiusClimbsTheThreeOptionsAndStopsAtTheLast() =
        runTest {
            val viewModel = opened()

            viewModel.onRadiusChange(SearchRadius.KM_2)
            viewModel.onWidenRadius()
            advanceUntilIdle()
            assertEquals(SearchRadius.KM_5, viewModel.uiState.value.criteria.radius)

            viewModel.onWidenRadius()
            advanceUntilIdle()
            assertEquals(SearchRadius.KM_10, viewModel.uiState.value.criteria.radius)

            val before = search.requests.size
            viewModel.onWidenRadius()
            advanceUntilIdle()
            assertEquals(SearchRadius.KM_10, viewModel.uiState.value.criteria.radius)
            assertEquals(before, search.requests.size)
        }

    // A card left open for a professional who is no longer in the results is a
    // card about nobody.
    @Test
    fun theSelectionIsClearedWhenTheResultsAreReplaced() =
        runTest {
            search.professionals = page(2)
            val viewModel = opened()
            viewModel.onProfessionalSelected("p1")

            viewModel.onRadiusChange(SearchRadius.KM_2)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.selectedId)
        }

    // Coming back after registering an address has to search again; coming back
    // from a profile must not blank the list that was already there.
    @Test
    fun comingBackWithTheSameAddressKeepsTheResultsAndWithANewOneSearchesAgain() =
        runTest {
            search.professionals = page(2)
            val viewModel = opened()

            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(1, search.requests.size)

            addresses.listResult =
                AddressListResult.Success(
                    listOf(address(id = "other", coordinate = coordinate(-17.4, -66.2))),
                )
            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(2, search.requests.size)
            assertEquals(coordinate(-17.4, -66.2), search.requests.last().origin)
        }

    @Test
    fun aSearchThatFailsOutrightOffersARetryThatSearchesAgain() =
        runTest {
            search.failure = SearchError.NetworkUnavailable
            val viewModel = opened()

            assertEquals(SearchResultsState.Failed(SearchError.NetworkUnavailable), viewModel.uiState.value.results)

            search.failure = null
            search.professionals = page(1)
            viewModel.onRetry()
            advanceUntilIdle()

            assertEquals(1, content(viewModel).professionals.size)
        }

    // Opening the list of types is not a criterion, so it must not spend a
    // request; and choosing one has to close it, or the twelve rows stay between
    // the filters and the map.
    @Test
    fun openingTheTypeListCostsNoRequestAndChoosingATypeClosesIt() =
        runTest {
            search.professionals = page(1)
            val viewModel = opened()

            viewModel.onTypeFilterToggle()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.typeFilterExpanded)
            assertEquals(1, search.requests.size)

            viewModel.onServiceTypeChange(physiotherapy.id)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.typeFilterExpanded)
            assertEquals(
                physiotherapy.id,
                search.requests
                    .last()
                    .criteria.serviceTypeId,
            )
        }

    // Touching a filter before the origin has resolved used to cancel the very
    // job that was resolving it, and the search then had nothing to search
    // around: the screen stayed on its spinner until the person left it and
    // came back.
    @Test
    fun changingTheRadiusBeforeTheOriginResolvedStillSearches() =
        runTest {
            search.professionals = page(2)
            val viewModel = viewModel()

            viewModel.refresh()
            viewModel.onRadiusChange(SearchRadius.KM_10)
            advanceUntilIdle()

            assertEquals(
                SearchRadius.KM_10,
                search.requests
                    .last()
                    .criteria.radius,
            )
            assertEquals(2, content(viewModel).professionals.size)
        }

    // The screen resolves the origin again on every resume, and a resume lands
    // on a rotation or on coming back from a profile. Cancelling the page that
    // was in flight without putting its flag down left the footer spinning for
    // ever: the list only asks for the next page when that flag is down, so
    // nothing ever asked again.
    @Test
    fun aResumeWhileAPageIsLoadingDoesNotStrandTheFooterSpinner() =
        runTest {
            search.professionals = page(SearchNearbyProfessionalsUseCase.PAGE_SIZE + 1)
            val viewModel = opened()

            val gate = CompletableDeferred<Unit>()
            search.gate = gate
            viewModel.onReachedEnd()
            advanceUntilIdle()
            assertTrue(content(viewModel).loadingMore)

            search.gate = null
            viewModel.refresh()
            advanceUntilIdle()
            gate.complete(Unit)
            advanceUntilIdle()

            assertFalse(content(viewModel).loadingMore)
            assertEquals(
                SearchNearbyProfessionalsUseCase.PAGE_SIZE + 1,
                content(viewModel).professionals.size,
            )
        }
}

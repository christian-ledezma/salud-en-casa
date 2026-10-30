@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.home.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.SetAvailabilityUseCase
import bo.saludencasa.features.profile.professionalDetails
import bo.saludencasa.features.profile.professionalProfile
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.InboxResult
import bo.saludencasa.features.requests.domain.usecase.GetInboxUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveInboxSignalsUseCase
import bo.saludencasa.features.requests.inboxRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ProfessionalHomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profiles =
        FakeProfileRepository(profileResult = ProfileResult.Success(professionalProfile()))
    private val requests = FakeRequestRepository()

    private fun viewModel(): ProfessionalHomeViewModel =
        ProfessionalHomeViewModel(
            GetProfileUseCase(profiles),
            SetAvailabilityUseCase(profiles),
            GetInboxUseCase(requests),
            ObserveInboxSignalsUseCase(requests),
        )

    private fun ProfessionalHomeViewModel.content(): ProfessionalHomeUiState.Content =
        uiState.value as ProfessionalHomeUiState.Content

    // The switch on the home screen is the profile screen's, moved: a refused
    // write that left the switch where the finger put it would tell the
    // professional they are visible while the search still hides them.
    @Test
    fun anAvailabilityTheServerRefusesReturnsToThePreviousValue() =
        runTest {
            profiles.profileResult =
                ProfileResult.Success(professionalProfile(professionalDetails(availableNow = false)))
            profiles.availabilityResult = AvailabilityResult.Failure(ProfileError.NetworkUnavailable)
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            viewModel.onAvailabilityChange(true)
            advanceUntilIdle()

            assertFalse(viewModel.content().availableNow)
            assertEquals(ProfileError.NetworkUnavailable, viewModel.content().availabilityError)
        }

    // The home screen previews the inbox; the tab is where the whole page lives.
    @Test
    fun onlyTheFirstThreeRequestsOfTheInboxReachTheHomeScreen() =
        runTest {
            requests.inboxResult = InboxResult.Loaded(List(5) { inboxRequest(id = "r$it") })
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            val nearby = viewModel.content().nearby as NearbyRequests.Some
            assertEquals(listOf("r0", "r1", "r2"), nearby.requests.map { it.id })
        }

    // The refresh runs on every resume, so a dropped read must not swap what the
    // professional was looking at for an error.
    @Test
    fun aRefreshThatFailsKeepsWhatWasOnScreen() =
        runTest {
            requests.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "r1")))
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()

            profiles.profileResult = ProfileResult.Failure(ProfileError.NetworkUnavailable)
            viewModel.refresh()
            advanceUntilIdle()

            val nearby = viewModel.content().nearby as NearbyRequests.Some
            assertEquals("r1", nearby.requests.single().id)
        }

    // RF-07.4 on the home screen: a request published in range appears without
    // the professional reloading, driven by the inbox's own broadcast.
    @Test
    fun aSignalBringsANewRequestToTheHomeScreen() =
        runTest {
            requests.inboxResult = InboxResult.Loaded(emptyList())
            val viewModel = viewModel()
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(NearbyRequests.None, viewModel.content().nearby)

            requests.inboxResult = InboxResult.Loaded(listOf(inboxRequest(id = "fresh")))
            requests.inboxSignals.emit(Unit)
            advanceUntilIdle()

            val nearby = viewModel.content().nearby as NearbyRequests.Some
            assertEquals("fresh", nearby.requests.single().id)
        }
}

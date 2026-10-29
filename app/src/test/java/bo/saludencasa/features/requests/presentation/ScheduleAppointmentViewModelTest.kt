@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.usecase.GetProfessionalServicesUseCase
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.professionalService
import bo.saludencasa.features.catalog.venousLine
import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.profile.FakeProfileRepository
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.usecase.GetPublicProfileUseCase
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.bookableStart
import bo.saludencasa.features.requests.domain.model.BookableTimesResult
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.usecase.CreateScheduledRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetBookableTimesUseCase
import bo.saludencasa.features.requests.loadedTimes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ScheduleAppointmentViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val monday = LocalDate.of(2026, 10, 19)
    private val tuesday = monday.plusDays(1)

    private val profiles = FakeProfileRepository()

    // Sorted by name, so the venous line comes first and is the one chosen.
    private val catalog =
        FakeCatalogRepository(
            services =
                listOf(
                    professionalService(id = "d1", type = venousLine),
                    professionalService(id = "d2", type = generalConsultation),
                ),
        )
    private val addresses =
        FakeAddressRepository(
            listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "a2", alias = "Oficina", isPrimary = false),
                        address(id = "a1", alias = "Casa", isPrimary = true),
                    ),
                ),
        )
    private val requests =
        FakeRequestRepository().apply {
            bookableTimes =
                mapOf(
                    venousLine.id to loadedTimes(bookableStart(monday, 9), bookableStart(tuesday, 10)),
                    generalConsultation.id to loadedTimes(bookableStart(tuesday, 15)),
                )
        }

    private fun viewModel(preferredServiceTypeId: String? = null): ScheduleAppointmentViewModel =
        ScheduleAppointmentViewModel(
            professionalId = PROFESSIONAL,
            preferredServiceTypeId = preferredServiceTypeId,
            getPublicProfile = GetPublicProfileUseCase(profiles),
            getProfessionalServices = GetProfessionalServicesUseCase(catalog),
            getMyAddresses = GetMyAddressesUseCase(addresses),
            getBookableTimes = GetBookableTimesUseCase(requests),
            createScheduledRequest = CreateScheduledRequestUseCase(requests),
        )

    private fun content(viewModel: ScheduleAppointmentViewModel): ScheduleAppointmentUiState.Content =
        viewModel.uiState.value as ScheduleAppointmentUiState.Content

    @Test
    fun theTimesOfTheFirstServiceShowWithoutATapAndOnItsFirstDay() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            val content = content(viewModel)
            assertEquals(venousLine.id, content.selectedServiceTypeId)
            assertEquals(PROFESSIONAL to venousLine.id, requests.bookableTimesRequests.single())
            assertEquals(monday, content.selectedDate)
            assertEquals("a1", content.selectedAddressId)
        }

    // HT-24: coming back from «Agregar otra dirección» chooses the one just added
    // and keeps the time and the description already chosen.
    @Test
    fun comingBackWithANewAddressChoosesItAndKeepsTheRest() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onDescriptionChanged("Control de presión")
            val chosenStart = content(viewModel).selectedStart

            addresses.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "a2", alias = "Oficina", isPrimary = false),
                        address(id = "a1", alias = "Casa", isPrimary = true),
                        address(id = "a3", alias = "Casa de mi mamá", isPrimary = false),
                    ),
                )
            viewModel.refreshAddresses()
            advanceUntilIdle()

            val content = content(viewModel)
            assertEquals("a3", content.selectedAddressId)
            assertEquals("Control de presión", content.description)
            assertEquals(chosenStart, content.selectedStart)
        }

    // With no address yet, the person had to leave to add one: coming back loads
    // the screen with it.
    @Test
    fun comingBackWithTheFirstAddressLoadsTheScreen() =
        runTest {
            addresses.listResult = AddressListResult.Success(emptyList())
            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(ScheduleAppointmentUiState.NoAddress, viewModel.uiState.value)

            addresses.listResult = AddressListResult.Success(listOf(address(id = "a1", alias = "Casa")))
            viewModel.refreshAddresses()
            advanceUntilIdle()

            assertEquals("a1", content(viewModel).selectedAddressId)
        }

    // The times depend on the service's duration: a time picked for a 60
    // minute service may not fit a longer one, so it must not survive the
    // switch and be sent for the other service.
    @Test
    fun switchingServiceReloadsTheTimesAndDropsThePickedOne() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTimeSelected(bookableStart(monday, 9))

            viewModel.onServiceSelected(generalConsultation.id)
            advanceUntilIdle()

            val content = content(viewModel)
            assertNull(content.selectedStart)
            assertEquals(tuesday, content.selectedDate)
            assertEquals(generalConsultation.id, requests.bookableTimesRequests.last().second)
        }

    // The answer for the service the patient left behind arrives last. Drawn,
    // it would show the old service's hours under the new service's name.
    @Test
    fun aLateAnswerForTheServiceLeftBehindIsNotDrawn() =
        runTest {
            val held = CompletableDeferred<Unit>()
            requests.bookableTimesGates = mapOf(venousLine.id to held)
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onServiceSelected(generalConsultation.id)
            advanceUntilIdle()
            held.complete(Unit)
            advanceUntilIdle()

            val times = (content(viewModel).times as TimesState.Loaded).times
            assertEquals(listOf(tuesday), times.starts.map { it.date })
        }

    @Test
    fun pickingAnotherDayDropsTheTimePickedOnThePreviousOne() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTimeSelected(bookableStart(monday, 9))

            viewModel.onDateSelected(tuesday)

            assertEquals(tuesday, content(viewModel).selectedDate)
            assertNull(content(viewModel).selectedStart)
        }

    @Test
    fun aDayWithoutTimesCannotBeChosen() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDateSelected(monday.plusDays(5))

            assertEquals(monday, content(viewModel).selectedDate)
        }

    @Test
    fun nothingCanBeSubmittedUntilATimeIsPicked() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            assertFalse(content(viewModel).canSubmit)

            viewModel.onSubmit()
            advanceUntilIdle()

            assertTrue(requests.scheduledDrafts.isEmpty())
        }

    @Test
    fun submittingSendsThePickedInstantServiceAndAddress() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTimeSelected(bookableStart(tuesday, 10))
            viewModel.onAddressSelected("a2")

            viewModel.onSubmit()
            advanceUntilIdle()

            val draft = requests.scheduledDrafts.single()
            assertEquals(bookableStart(tuesday, 10).startAt, draft.scheduledAt)
            assertEquals(venousLine.id, draft.serviceTypeId)
            assertEquals("a2", draft.addressId)
            assertEquals(PROFESSIONAL, draft.professionalId)
            assertEquals(ScheduleAppointmentUiState.Submitted, viewModel.uiState.value)
        }

    @Test
    fun aSecondTapWhileTheFirstIsInFlightSendsNothing() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTimeSelected(bookableStart(monday, 9))

            viewModel.onSubmit()
            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(1, requests.scheduledDrafts.size)
        }

    // The list was stale: the hour fell under the two hours while the screen
    // was open. The answer is the fresh list, with everything typed kept.
    @Test
    fun aTimeNoLongerBookableReloadsTheTimesAndKeepsWhatWasTyped() =
        runTest {
            requests.scheduleResult = RequestResult.Failure(RequestError.TimeNotBookable)
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTimeSelected(bookableStart(monday, 9))
            viewModel.onDescriptionChanged("Control semanal")

            viewModel.onSubmit()
            advanceUntilIdle()

            val content = content(viewModel)
            assertNull(content.selectedStart)
            assertEquals(RequestError.TimeNotBookable, content.submitError)
            assertEquals("Control semanal", content.description)
            assertEquals(2, requests.bookableTimesRequests.size)
            assertTrue(content.times is TimesState.Loaded)
        }

    @Test
    fun anAddressOutsideCoverageKeepsThePickedTime() =
        runTest {
            requests.scheduleResult = RequestResult.Failure(RequestError.OutsideCoverage)
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onTimeSelected(bookableStart(monday, 9))

            viewModel.onSubmit()
            advanceUntilIdle()

            val content = content(viewModel)
            assertEquals(RequestError.OutsideCoverage, content.submitError)
            assertEquals(bookableStart(monday, 9), content.selectedStart)
            assertFalse(content.submitting)
        }

    // HT-23: the patient filtered the list by a service and tapped «Agendar», so
    // the times shown are those of that service and not the first one declared.
    @Test
    fun theServiceTheListWasFilteredByOpensSelectedWithItsTimes() =
        runTest {
            val viewModel = viewModel(preferredServiceTypeId = generalConsultation.id)
            advanceUntilIdle()

            val content = content(viewModel)
            assertEquals(generalConsultation.id, content.selectedServiceTypeId)
            assertEquals(PROFESSIONAL to generalConsultation.id, requests.bookableTimesRequests.single())
            assertEquals(tuesday, content.selectedDate)
        }

    // The filter can name a service this professional does not offer when the
    // list was not narrowed by it; the screen falls back to one that exists.
    @Test
    fun aServiceTheProfessionalDoesNotOfferFallsBackToTheFirstDeclared() =
        runTest {
            val viewModel = viewModel(preferredServiceTypeId = "not-offered")
            advanceUntilIdle()

            assertEquals(venousLine.id, content(viewModel).selectedServiceTypeId)
        }

    @Test
    fun aProfessionalWhoDeclaresNoServiceCannotBeBooked() =
        runTest {
            val empty = FakeCatalogRepository(services = emptyList())
            val viewModel =
                ScheduleAppointmentViewModel(
                    PROFESSIONAL,
                    null,
                    GetPublicProfileUseCase(profiles),
                    GetProfessionalServicesUseCase(empty),
                    GetMyAddressesUseCase(addresses),
                    GetBookableTimesUseCase(requests),
                    CreateScheduledRequestUseCase(requests),
                )
            advanceUntilIdle()

            assertEquals(ScheduleAppointmentUiState.NotBookable, viewModel.uiState.value)
        }

    @Test
    fun aProfessionalNoLongerPublishedCannotBeBooked() =
        runTest {
            profiles.publicProfileResult = PublicProfileResult.NotPublished

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(ScheduleAppointmentUiState.NotBookable, viewModel.uiState.value)
        }

    @Test
    fun withoutAnAddressTheStateIsNoAddress() =
        runTest {
            addresses.listResult = AddressListResult.Success(emptyList())

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(ScheduleAppointmentUiState.NoAddress, viewModel.uiState.value)
        }

    @Test
    fun timesThatCannotBeReadLeaveTheFormStandingWithARetry() =
        runTest {
            requests.bookableTimes =
                mapOf(
                    venousLine.id to BookableTimesResult.Failure(RequestError.NetworkUnavailable),
                )
            val viewModel = viewModel()
            advanceUntilIdle()
            assertEquals(TimesState.Failed(RequestError.NetworkUnavailable), content(viewModel).times)

            requests.bookableTimes = mapOf(venousLine.id to loadedTimes(bookableStart(monday, 9)))
            viewModel.onRetryTimes()
            advanceUntilIdle()

            assertTrue(content(viewModel).times is TimesState.Loaded)
        }
}

private const val PROFESSIONAL = "fcab94c0-0000-4000-8000-000000000000"

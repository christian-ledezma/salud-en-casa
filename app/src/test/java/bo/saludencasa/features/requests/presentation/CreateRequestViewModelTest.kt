@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.requests.presentation

import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.usecase.GetServiceTypeCatalogUseCase
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.requests.FakeRequestRepository
import bo.saludencasa.features.requests.domain.model.RequestError
import bo.saludencasa.features.requests.domain.model.RequestResult
import bo.saludencasa.features.requests.domain.usecase.CreateImmediateRequestUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateRequestViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val secondary = address(id = "a2", alias = "Oficina", isPrimary = false)
    private val primary = address(id = "a1", alias = "Casa", isPrimary = true)

    private val catalog = FakeCatalogRepository(catalog = listOf(generalConsultation))
    private val addresses = FakeAddressRepository(listResult = AddressListResult.Success(listOf(secondary, primary)))
    private val requests = FakeRequestRepository()

    private fun viewModel(): CreateRequestViewModel =
        CreateRequestViewModel(
            GetServiceTypeCatalogUseCase(catalog),
            GetMyAddressesUseCase(addresses),
            CreateImmediateRequestUseCase(requests),
        )

    // A patient with no address cannot publish anywhere: the screen has to offer
    // the way to register one, not an empty form.
    @Test
    fun withoutAnAddressTheStateIsNoAddress() =
        runTest {
            addresses.listResult = AddressListResult.Success(emptyList())

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(CreateRequestUiState.NoAddress, viewModel.uiState.value)
        }

    @Test
    fun aFailureLoadingTheFormIsReportedAsFailed() =
        runTest {
            catalog.readFailure = CatalogError.NetworkUnavailable

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                CreateRequestUiState.Failed(RequestError.NetworkUnavailable),
                viewModel.uiState.value,
            )
        }

    // The primary address is the one offered by default, even when it is not the
    // first row the repository happens to return.
    @Test
    fun theContentSelectsThePrimaryAddressByDefault() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            val content = viewModel.uiState.value as CreateRequestUiState.Content
            assertEquals("a1", content.selectedAddressId)
        }

    @Test
    fun aSuccessfulSubmissionEndsInSubmitted() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onServiceTypeSelected(generalConsultation.id)

            viewModel.onSubmit()
            advanceUntilIdle()

            assertEquals(CreateRequestUiState.Submitted, viewModel.uiState.value)
            assertEquals(generalConsultation.id, requests.createdDrafts.single().serviceTypeId)
        }

    // A rejected budget keeps the form on screen with the number still in it,
    // carrying the typed reason, not a blank restart.
    @Test
    fun aRejectedBudgetKeepsTheFormAndCarriesTheReason() =
        runTest {
            requests.createResult = RequestResult.Failure(RequestError.InvalidBudget)

            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.onServiceTypeSelected(generalConsultation.id)
            viewModel.onBudgetChanged("-5")

            viewModel.onSubmit()
            advanceUntilIdle()

            val content = viewModel.uiState.value as CreateRequestUiState.Content
            assertEquals(RequestError.InvalidBudget, content.submitError)
            assertEquals("-5", content.budget)
            assertTrue(!content.submitting)
        }

    @Test
    fun theSubmitButtonIsHeldUntilAServiceTypeIsChosen() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            val content = viewModel.uiState.value as CreateRequestUiState.Content
            assertTrue(!content.canSubmit)
        }
}

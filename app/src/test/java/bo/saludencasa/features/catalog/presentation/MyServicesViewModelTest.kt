@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.catalog.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.usecase.DeclareServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.GetMyDeclaredServicesUseCase
import bo.saludencasa.features.catalog.domain.usecase.RemoveServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.UpdateServicePriceUseCase
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.professionalService
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

class MyServicesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalog = listOf(generalConsultation, physiotherapy)

    private fun repositoryWith(vararg declaredTypes: String): FakeCatalogRepository =
        FakeCatalogRepository(
            catalog = catalog,
            services =
                declaredTypes.mapIndexed { index, typeId ->
                    professionalService(id = "declared-$index", type = catalog.first { it.id == typeId })
                },
        )

    private fun viewModel(repository: FakeCatalogRepository): MyServicesViewModel =
        MyServicesViewModel(
            GetMyDeclaredServicesUseCase(repository),
            DeclareServiceUseCase(repository),
            UpdateServicePriceUseCase(repository),
            RemoveServiceUseCase(repository),
        )

    private fun TestScope.loaded(viewModel: MyServicesViewModel): MyServicesUiState.Content {
        advanceUntilIdle()
        return viewModel.uiState.value as MyServicesUiState.Content
    }

    private fun content(viewModel: MyServicesViewModel): MyServicesUiState.Content =
        viewModel.uiState.value as MyServicesUiState.Content

    // RF-05.1 keeps a reference price in the catalog so the professional
    // confirms a number instead of inventing one. An empty field here would
    // make the reference invisible at the only moment it is useful.
    @Test
    fun theDialogOpensWithTheCatalogReferenceAlreadyFilled() =
        runTest {
            val viewModel = viewModel(repositoryWith())
            loaded(viewModel)

            viewModel.onDeclareClick(physiotherapy)

            assertEquals(
                ServiceEditor.Declaring(physiotherapy, price = "130", error = null),
                content(viewModel).editor,
            )
        }

    // HU-10's second criterion through the view model: the type and the price
    // the person confirmed are what reach the repository, and the list is read
    // again so the new row appears without leaving the screen.
    @Test
    fun confirmingAPriceDeclaresTheTypeAndReadsTheListAgain() =
        runTest {
            val repository = repositoryWith()
            val viewModel = viewModel(repository)
            loaded(viewModel)

            viewModel.onDeclareClick(physiotherapy)
            viewModel.onPriceChanged("140")
            viewModel.onConfirmPrice()
            advanceUntilIdle()

            assertEquals(physiotherapy.id, repository.declarations.single().id)
            val after = content(viewModel)
            assertEquals(listOf(physiotherapy), after.declaredServices.services.map { it.type })
            // And the type it just declared is no longer offered for a second
            // declaration, which is the screen's half of the third criterion.
            assertEquals(listOf(generalConsultation), after.declaredServices.undeclaredTypes)
            assertNull(after.editor)
        }

    // A refused price has to stay where the person can fix it. Closing the
    // dialog on failure would throw away the number they typed and leave them
    // guessing what the screen objected to.
    @Test
    fun aRefusedPriceKeepsTheDialogOpenWithTheNumberThatWasTyped() =
        runTest {
            val repository = repositoryWith()
            val viewModel = viewModel(repository)
            loaded(viewModel)

            viewModel.onDeclareClick(physiotherapy)
            viewModel.onPriceChanged("0")
            viewModel.onConfirmPrice()
            advanceUntilIdle()

            assertEquals(
                ServiceEditor.Declaring(physiotherapy, price = "0", error = CatalogError.InvalidPrice),
                content(viewModel).editor,
            )
            assertTrue(repository.declarations.isEmpty())
        }

    // Removing a declared service takes the professional out of every search
    // that filters by that type, so the tap that offers the removal must not be
    // the tap that performs it.
    @Test
    fun removingAServiceWritesNothingUntilItIsConfirmed() =
        runTest {
            val repository = repositoryWith(generalConsultation.id)
            val viewModel = viewModel(repository)
            val service = loaded(viewModel).declaredServices.services.single()

            viewModel.onRemoveClick(service)
            advanceUntilIdle()

            assertEquals(ServiceEditor.Removing(service), content(viewModel).editor)
            assertTrue(repository.removals.isEmpty())

            viewModel.onConfirmRemoval()
            advanceUntilIdle()

            assertEquals(listOf(service.id), repository.removals)
            assertTrue(content(viewModel).declaredServices.services.isEmpty())
        }

    // The removal confirmation has no field to correct, so its reason belongs
    // on the screen. Leaving the dialog open would strand the person in front
    // of a button that already failed.
    @Test
    fun aFailedRemovalClosesItsConfirmationAndCarriesTheReasonToTheScreen() =
        runTest {
            val repository = repositoryWith(generalConsultation.id)
            repository.writeFailure = CatalogError.NetworkUnavailable
            val viewModel = viewModel(repository)
            val service = loaded(viewModel).declaredServices.services.single()

            viewModel.onRemoveClick(service)
            viewModel.onConfirmRemoval()
            advanceUntilIdle()

            val after = content(viewModel)
            assertNull(after.editor)
            assertEquals(CatalogError.NetworkUnavailable, after.notice)
            assertFalse(after.busy)
        }

    // Two taps on a slow connection would insert the row twice, and the second
    // one comes back as a unique violation on a write nobody meant to repeat.
    @Test
    fun aSecondConfirmationWhileTheFirstIsInFlightDoesNotWriteTwice() =
        runTest {
            val repository = repositoryWith()
            val viewModel = viewModel(repository)
            loaded(viewModel)

            viewModel.onDeclareClick(physiotherapy)
            viewModel.onConfirmPrice()
            viewModel.onConfirmPrice()
            advanceUntilIdle()

            assertEquals(1, repository.declarations.size)
        }

    @Test
    fun aCatalogThatCannotBeReadLeavesTheScreenInItsFailedState() =
        runTest {
            val repository = repositoryWith()
            repository.readFailure = CatalogError.NetworkUnavailable
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(MyServicesUiState.Loading, awaitItem())
                assertEquals(MyServicesUiState.Failed(CatalogError.NetworkUnavailable), awaitItem())
            }
        }
}

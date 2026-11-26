@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.catalog.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.usecase.DeclareServicesUseCase
import bo.saludencasa.features.catalog.domain.usecase.GetMyDeclaredServicesUseCase
import bo.saludencasa.features.catalog.domain.usecase.RemoveServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.UpdateServicePriceUseCase
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.professionalService
import bo.saludencasa.features.catalog.serviceType
import bo.saludencasa.features.catalog.venousLine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class MyServicesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val catalog = listOf(generalConsultation, venousLine, physiotherapy)

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
            DeclareServicesUseCase(repository),
            UpdateServicePriceUseCase(repository),
            RemoveServiceUseCase(repository),
        )

    private fun TestScope.loaded(viewModel: MyServicesViewModel): MyServicesUiState.Content {
        advanceUntilIdle()
        return viewModel.uiState.value as MyServicesUiState.Content
    }

    private fun content(viewModel: MyServicesViewModel): MyServicesUiState.Content =
        viewModel.uiState.value as MyServicesUiState.Content

    // ---- the selector -------------------------------------------------------

    @Test
    fun openingTheSelectorStartsWithNothingChosenAndLeavesTheListAsItWas() =
        runTest {
            val viewModel = viewModel(repositoryWith(generalConsultation.id))
            val before = loaded(viewModel)

            viewModel.onAddServicesClick()

            val after = content(viewModel)
            assertEquals(ServicePicker(), after.picker)
            assertEquals(before.declaredServices, after.declaredServices)
        }

    @Test
    fun choosingATypeASecondTimeUnchoosesIt() =
        runTest {
            val viewModel = viewModel(repositoryWith())
            loaded(viewModel)

            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(physiotherapy.id)
            viewModel.onPickerToggle(venousLine.id)
            assertEquals(setOf(physiotherapy.id, venousLine.id), content(viewModel).picker?.selected)

            viewModel.onPickerToggle(physiotherapy.id)
            assertEquals(setOf(venousLine.id), content(viewModel).picker?.selected)
        }

    // The selector offers what is left to declare, narrowed by what was typed.
    // Searching must never bring back a type that is already declared: that
    // is the write the unique index would refuse.
    @Test
    fun typingNarrowsWhatIsOfferedAndNeverOffersADeclaredType() =
        runTest {
            val viewModel = viewModel(repositoryWith(generalConsultation.id))
            loaded(viewModel)
            viewModel.onAddServicesClick()

            viewModel.onPickerQueryChanged("a")
            assertEquals(listOf(venousLine, physiotherapy), content(viewModel).pickerTypes)

            viewModel.onPickerQueryChanged("fisica")
            assertEquals(listOf(physiotherapy), content(viewModel).pickerTypes)

            viewModel.onPickerQueryChanged("medica")
            assertTrue(content(viewModel).pickerTypes.isEmpty())
        }

    @Test
    fun closingTheSelectorWritesNothing() =
        runTest {
            val repository = repositoryWith()
            val viewModel = viewModel(repository)
            loaded(viewModel)

            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(physiotherapy.id)
            viewModel.onPickerDismiss()
            advanceUntilIdle()

            assertNull(content(viewModel).picker)
            assertEquals(0, repository.declareCalls)
        }

    // HT-32's fifth criterion through the view model: the chosen types leave
    // in one write, the selector closes on the list read again, the new rows
    // are marked, and the notice that asks to review their prices is raised.
    @Test
    fun declaringTheChosenTypesWritesOnceClosesTheSelectorAndMarksTheNewRows() =
        runTest {
            val repository = repositoryWith(generalConsultation.id)
            val viewModel = viewModel(repository)
            loaded(viewModel)

            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(venousLine.id)
            viewModel.onPickerToggle(physiotherapy.id)
            viewModel.onConfirmDeclaration()
            advanceUntilIdle()

            val after = content(viewModel)
            assertEquals(1, repository.declareCalls)
            assertNull(after.picker)
            assertFalse(after.busy)
            assertEquals(
                setOf(generalConsultation.id, venousLine.id, physiotherapy.id),
                after.declaredServices.services
                    .map { it.type.id }
                    .toSet(),
            )
            assertEquals(setOf(venousLine.id, physiotherapy.id), after.newTypeIds)
            assertEquals(2, after.declarationNotice)
        }

    @Test
    fun theDeclarationNoticeIsConsumedOnceShown() =
        runTest {
            val viewModel = viewModel(repositoryWith())
            loaded(viewModel)
            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(physiotherapy.id)
            viewModel.onConfirmDeclaration()
            advanceUntilIdle()

            viewModel.onDeclarationNoticeShown()

            assertNull(content(viewModel).declarationNotice)
            // The mark on the row is not the notice: it stays while the screen does.
            assertEquals(setOf(physiotherapy.id), content(viewModel).newTypeIds)
        }

    // Two taps on a slow connection would otherwise send the same batch twice,
    // and the second comes back as a unique violation nobody meant to cause.
    @Test
    fun aSecondConfirmationWhileTheFirstIsInFlightDoesNotWriteTwice() =
        runTest {
            val repository = repositoryWith()
            val viewModel = viewModel(repository)
            loaded(viewModel)
            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(physiotherapy.id)

            viewModel.onConfirmDeclaration()
            viewModel.onConfirmDeclaration()
            advanceUntilIdle()

            assertEquals(1, repository.declareCalls)
        }

    // The button is disabled with nothing chosen, and the model must not
    // depend on that: confirming an empty selection writes nothing and leaves
    // the selector exactly as it was.
    @Test
    fun confirmingWithNothingChosenWritesNothing() =
        runTest {
            val repository = repositoryWith()
            val viewModel = viewModel(repository)
            loaded(viewModel)
            viewModel.onAddServicesClick()

            viewModel.onConfirmDeclaration()
            advanceUntilIdle()

            assertEquals(0, repository.declareCalls)
            assertEquals(ServicePicker(), content(viewModel).picker)
        }

    // Another device declared one of the chosen types while the selector was
    // open. The write is refused whole; what is still offered stays chosen,
    // what is not is dropped, and the person is told. Declaring again must go
    // through, or the selector would be a trap.
    @Test
    fun aTypeDeclaredElsewhereKeepsTheSelectorOpenWithTheRestChosenAndSaysWhy() =
        runTest {
            val repository = repositoryWith()
            val viewModel = viewModel(repository)
            loaded(viewModel)
            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(generalConsultation.id)
            viewModel.onPickerToggle(physiotherapy.id)
            repository.declareElsewhere(generalConsultation)

            viewModel.onConfirmDeclaration()
            advanceUntilIdle()

            val refused = content(viewModel)
            assertEquals(setOf(physiotherapy.id), refused.picker?.selected)
            assertEquals(CatalogError.ServiceAlreadyDeclared, refused.picker?.error)
            assertFalse(refused.busy)
            assertEquals(0, repository.declarations.size)
            assertFalse(generalConsultation in refused.pickerTypes)

            viewModel.onConfirmDeclaration()
            advanceUntilIdle()

            assertNull(content(viewModel).picker)
            assertEquals(physiotherapy.id, repository.declarations.single().id)
        }

    // A failed write that is not about the catalog has nothing to repair: the
    // selection stays exactly as chosen, so the person only has to try again.
    @Test
    fun aFailedWriteKeepsTheSelectorOpenWithTheSelectionIntact() =
        runTest {
            val repository = repositoryWith()
            repository.writeFailure = CatalogError.NetworkUnavailable
            val viewModel = viewModel(repository)
            loaded(viewModel)
            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(physiotherapy.id)
            viewModel.onPickerToggle(venousLine.id)

            viewModel.onConfirmDeclaration()
            advanceUntilIdle()

            val after = content(viewModel)
            assertEquals(setOf(physiotherapy.id, venousLine.id), after.picker?.selected)
            assertEquals(CatalogError.NetworkUnavailable, after.picker?.error)
            assertFalse(after.busy)
        }

    // ---- the list -----------------------------------------------------------

    // The search of the list appears past eight rows. Below that it does not
    // narrow anything, even with text in it: a hidden field that still filters
    // would leave the professional with rows missing and no way to see why.
    @Test
    fun theListSearchOnlyNarrowsPastEightDeclaredServices() =
        runTest {
            val few = viewModel(repositoryWith(generalConsultation.id, venousLine.id))
            loaded(few)
            few.onQueryChanged("terapia")
            assertFalse(content(few).showsSearch)
            assertEquals(2, content(few).visibleServices.size)

            val manyTypes = (1..9).map { serviceType(id = "type-$it", name = "Servicio $it") } + physiotherapy
            val many =
                viewModel(
                    FakeCatalogRepository(
                        catalog = manyTypes,
                        services =
                            manyTypes.mapIndexed {
                                i,
                                type,
                                ->
                                professionalService(id = "declared-$i", type = type)
                            },
                    ),
                )
            loaded(many)
            many.onQueryChanged("terapia")
            assertTrue(content(many).showsSearch)
            assertEquals(listOf(physiotherapy), content(many).visibleServices.map { it.type })
        }

    // ---- the price ----------------------------------------------------------

    @Test
    fun confirmingANewPriceWritesItAndReadsTheListAgain() =
        runTest {
            val repository = repositoryWith(physiotherapy.id)
            val viewModel = viewModel(repository)
            val service = loaded(viewModel).declaredServices.services.single()

            viewModel.onRepriceClick(service)
            assertEquals("180", (content(viewModel).editor as ServiceEditor.Repricing).price)
            viewModel.onPriceChanged("200")
            viewModel.onConfirmPrice()
            advanceUntilIdle()

            assertEquals(BigDecimal("200"), repository.repricings.single().price)
            val after = content(viewModel)
            assertNull(after.editor)
            assertEquals(
                BigDecimal("200"),
                after.declaredServices.services
                    .single()
                    .referencePriceBob,
            )
        }

    // A refused price has to stay where the person can fix it. Closing the
    // dialog on failure would throw away the number they typed.
    @Test
    fun aRefusedPriceKeepsTheDialogOpenWithTheNumberThatWasTyped() =
        runTest {
            val repository = repositoryWith(physiotherapy.id)
            val viewModel = viewModel(repository)
            val service = loaded(viewModel).declaredServices.services.single()

            viewModel.onRepriceClick(service)
            viewModel.onPriceChanged("0")
            viewModel.onConfirmPrice()
            advanceUntilIdle()

            assertEquals(
                ServiceEditor.Repricing(service, price = "0", error = CatalogError.InvalidPrice),
                content(viewModel).editor,
            )
            assertTrue(repository.repricings.isEmpty())
        }

    // A list of fifteen rows read again from the top is a list the professional
    // has to scroll back down. The write replaces the content in place, so the
    // screen never passes through its loading state.
    @Test
    fun aWriteNeverPassesThroughTheLoadingState() =
        runTest {
            val repository = repositoryWith(physiotherapy.id)
            val viewModel = viewModel(repository)
            val service = loaded(viewModel).declaredServices.services.single()
            val seen = mutableListOf<MyServicesUiState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.toList(seen) }

            viewModel.onRepriceClick(service)
            viewModel.onPriceChanged("200")
            viewModel.onConfirmPrice()
            advanceUntilIdle()

            assertTrue(seen.isNotEmpty())
            assertTrue(seen.none { it == MyServicesUiState.Loading })
        }

    // ---- removing -----------------------------------------------------------

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

    // The mark says «declared just now». A service that was removed and later
    // declared again is not new on the strength of an old mark.
    @Test
    fun aRemovedServiceLosesItsNewMark() =
        runTest {
            val viewModel = viewModel(repositoryWith())
            loaded(viewModel)
            viewModel.onAddServicesClick()
            viewModel.onPickerToggle(physiotherapy.id)
            viewModel.onConfirmDeclaration()
            advanceUntilIdle()
            val service = content(viewModel).declaredServices.services.single()

            viewModel.onRemoveClick(service)
            viewModel.onConfirmRemoval()
            advanceUntilIdle()

            assertTrue(content(viewModel).newTypeIds.isEmpty())
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

@file:OptIn(ExperimentalCoroutinesApi::class)

package bo.saludencasa.features.location.presentation

import app.cash.turbine.test
import bo.saludencasa.MainDispatcherRule
import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.usecase.DeleteAddressUseCase
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.location.domain.usecase.SetPrimaryAddressUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddressListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // No stories cover it directly, but a list screen with no rows and no
    // message is the blank screen .claude/rules/compose.md calls out by name:
    // it tells a first-time user nothing, instead of inviting them to add one.
    @Test
    fun `no registered addresses shows the empty state`() =
        runTest {
            val repository = FakeAddressRepository(listResult = AddressListResult.Success(emptyList()))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                assertEquals(AddressListUiState.Loading, awaitItem())
                assertEquals(AddressListUiState.Empty, awaitItem())
            }
        }

    // The first criterion. What the screen has to show for each address is
    // exactly what the story asks for and nothing computed on this side.
    @Test
    fun `the list carries the alias and the reference of every address`() =
        runTest {
            val casa = address(id = "1", alias = "Casa", reference = "Portón verde", isPrimary = true)
            val trabajo = address(id = "2", alias = "Trabajo", reference = null, isPrimary = false)
            val repository = FakeAddressRepository(listResult = AddressListResult.Success(listOf(casa, trabajo)))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                val content = awaitItem() as AddressListUiState.Content
                assertEquals(listOf(casa, trabajo), content.addresses)
            }
        }

    // The second criterion, as the list shows it: the previous primary stops
    // being one automatically, and that is a rule the trigger enforces, not
    // this screen. Re-reading the list is how the screen finds out.
    @Test
    fun `marking an address as primary reloads the list instead of guessing the result`() =
        runTest {
            val casa = address(id = "1", alias = "Casa", isPrimary = true)
            val trabajo = address(id = "2", alias = "Trabajo", isPrimary = false)
            val trabajoAsPrimary = trabajo.copy(isPrimary = true)
            val casaNotPrimary = casa.copy(isPrimary = false)
            val repository =
                FakeAddressRepository(listResult = AddressListResult.Success(listOf(casa, trabajo)))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onSetPrimaryClick("2")
                val pending = awaitItem() as AddressListUiState.Content
                assertEquals("2", pending.pendingId)

                repository.listResult = AddressListResult.Success(listOf(trabajoAsPrimary, casaNotPrimary))
                assertEquals(AddressListUiState.Loading, awaitItem())

                val reloaded = awaitItem() as AddressListUiState.Content
                assertTrue(reloaded.addresses.single { it.id == "2" }.isPrimary)
                assertTrue(reloaded.addresses.none { it.id == "1" && it.isPrimary })
            }

            assertEquals(listOf("2"), repository.setPrimaryAttempts)
            assertEquals(2, repository.listReads)
        }

    // An optimistic update here would leave a professional believing their
    // primary address changed when the server never accepted it, which is
    // the exact gap RN-02 depends on staying closed.
    @Test
    fun `a refused primary change reports the error and keeps the previous list`() =
        runTest {
            val casa = address(id = "1", alias = "Casa", isPrimary = true)
            val trabajo = address(id = "2", alias = "Trabajo", isPrimary = false)
            val repository =
                FakeAddressRepository(
                    listResult = AddressListResult.Success(listOf(casa, trabajo)),
                    setPrimaryResult = SetPrimaryAddressResult.Failure(AddressError.NetworkUnavailable),
                )
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onSetPrimaryClick("2")
                awaitItem()

                val failed = awaitItem() as AddressListUiState.Content
                assertEquals(null, failed.pendingId)
                assertEquals(AddressError.NetworkUnavailable, failed.notice)
                assertEquals(listOf(casa, trabajo), failed.addresses)
            }

            assertEquals(1, repository.listReads)
        }

    // The third criterion needs a confirmation step to exist at all: without
    // one, a single misplaced tap deletes an address with no way back.
    @Test
    fun `deleting an address asks for confirmation before touching the repository`() =
        runTest {
            val repository =
                FakeAddressRepository(listResult = AddressListResult.Success(listOf(address(id = "1"))))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onDeleteClick("1")
                val confirming = awaitItem() as AddressListUiState.Content
                assertEquals("1", confirming.confirmingDeleteId)
            }

            assertTrue(repository.deleteAttempts.isEmpty())
        }

    @Test
    fun `dismissing the delete confirmation leaves the address on the list`() =
        runTest {
            val repository =
                FakeAddressRepository(listResult = AddressListResult.Success(listOf(address(id = "1"))))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onDeleteClick("1")
                awaitItem()

                viewModel.onDismissDeleteConfirmation()
                val dismissed = awaitItem() as AddressListUiState.Content
                assertEquals(null, dismissed.confirmingDeleteId)
            }

            assertTrue(repository.deleteAttempts.isEmpty())
        }

    // The third criterion in full: confirming makes the address disappear
    // from the list, read back from the server rather than spliced out here.
    @Test
    fun `confirming the deletion removes the address once the server confirms it`() =
        runTest {
            val repository =
                FakeAddressRepository(listResult = AddressListResult.Success(listOf(address(id = "1"))))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onDeleteClick("1")
                awaitItem()

                viewModel.onConfirmDelete()
                awaitItem()

                repository.listResult = AddressListResult.Success(emptyList())
                assertEquals(AddressListUiState.Loading, awaitItem())
                assertEquals(AddressListUiState.Empty, awaitItem())
            }

            assertEquals(listOf("1"), repository.deleteAttempts)
        }

    @Test
    fun `a refused deletion reports the error and keeps the address on the list`() =
        runTest {
            val casa = address(id = "1")
            val repository =
                FakeAddressRepository(
                    listResult = AddressListResult.Success(listOf(casa)),
                    deleteResult = DeleteAddressResult.Failure(AddressError.NetworkUnavailable),
                )
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                awaitItem()

                viewModel.onDeleteClick("1")
                awaitItem()

                viewModel.onConfirmDelete()
                awaitItem()

                val failed = awaitItem() as AddressListUiState.Content
                assertEquals(null, failed.pendingId)
                assertEquals(AddressError.NetworkUnavailable, failed.notice)
                assertEquals(listOf(casa), failed.addresses)
            }
        }
}

private fun viewModel(repository: FakeAddressRepository): AddressListViewModel =
    AddressListViewModel(
        getMyAddresses = GetMyAddressesUseCase(repository),
        setPrimaryAddress = SetPrimaryAddressUseCase(repository),
        deleteAddress = DeleteAddressUseCase(repository),
    )

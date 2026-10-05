package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.model.SetProfessionalBaseResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAddressUseCaseTest {
    private val repository = FakeAddressRepository()
    private val useCase = DeleteAddressUseCase(repository)

    @Test
    fun `deleting an unmarked address never asks for a successor`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                        address(id = "3", alias = "Consultorio", isPrimary = false),
                    ),
                )

            val result = useCase("2")

            assertEquals(DeleteAddressResult.Success, result)
            assertEquals(listOf("2"), repository.deleteAttempts)
            assertTrue(repository.setPrimaryAttempts.isEmpty())
        }

    @Test
    fun `deleting the primary address with a single survivor lets the database promote it`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                    ),
                )

            val result = useCase("1")

            assertEquals(DeleteAddressResult.Success, result)
            assertEquals(listOf("1"), repository.deleteAttempts)
            assertTrue(repository.setPrimaryAttempts.isEmpty())
        }

    @Test
    fun `deleting the primary address with two survivors asks who inherits before writing`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                        address(id = "3", alias = "Consultorio", isPrimary = false),
                    ),
                )

            val result = useCase("1")

            assertEquals(DeleteAddressResult.SuccessorRequired, result)
            assertTrue(repository.deleteAttempts.isEmpty())
            assertTrue(repository.setPrimaryAttempts.isEmpty())
        }

    @Test
    fun `the chosen successor takes the primary mark before the address is deleted`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                        address(id = "3", alias = "Consultorio", isPrimary = false),
                    ),
                )

            val result = useCase("1", successorId = "3")

            assertEquals(DeleteAddressResult.Success, result)
            assertEquals(listOf("setPrimary:3", "delete:1"), repository.writes)
        }

    @Test
    fun `an address that is both primary and base hands over both marks`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true, isProfessionalBase = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                        address(id = "3", alias = "Consultorio", isPrimary = false),
                    ),
                )

            useCase("1", successorId = "2")

            assertEquals(
                listOf("setPrimary:2", "setProfessionalBase:2", "delete:1"),
                repository.writes,
            )
        }

    @Test
    fun `a failed handover stops before deleting, so no mark is lost`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                        address(id = "3", alias = "Consultorio", isPrimary = false),
                    ),
                )
            repository.setPrimaryResult = SetPrimaryAddressResult.Failure(AddressError.NetworkUnavailable)

            val result = useCase("1", successorId = "2")

            assertEquals(DeleteAddressResult.Failure(AddressError.NetworkUnavailable), result)
            assertEquals(listOf("setPrimary:2"), repository.writes)
        }

    @Test
    fun `a successor that is not one of my addresses is refused`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                        address(id = "3", alias = "Consultorio", isPrimary = false),
                    ),
                )

            val result = useCase("1", successorId = "someone-elses-address")

            assertEquals(DeleteAddressResult.SuccessorRequired, result)
            assertTrue(repository.setPrimaryAttempts.isEmpty())
            assertTrue(repository.deleteAttempts.isEmpty())
        }

    @Test
    fun `a base-only address hands over the base and leaves the primary alone`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Consultorio", isPrimary = false, isProfessionalBase = true),
                        address(id = "3", alias = "Trabajo", isPrimary = false),
                    ),
                )

            useCase("2", successorId = "3")

            assertEquals(listOf("setProfessionalBase:3", "delete:2"), repository.writes)
        }

    @Test
    fun `a failed base handover stops before deleting, even after the primary moved`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true, isProfessionalBase = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                        address(id = "3", alias = "Consultorio", isPrimary = false),
                    ),
                )
            repository.setProfessionalBaseResult =
                SetProfessionalBaseResult.Failure(AddressError.NetworkUnavailable)

            val result = useCase("1", successorId = "2")

            assertEquals(DeleteAddressResult.Failure(AddressError.NetworkUnavailable), result)
            assertEquals(listOf("setPrimary:2", "setProfessionalBase:2"), repository.writes)
        }

    @Test
    fun `an engine refusal becomes a successor request and not an error`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Trabajo", isPrimary = false),
                    ),
                )
            repository.deleteResult = DeleteAddressResult.Failure(AddressError.SuccessorRequired)

            val result = useCase("1")

            assertEquals(DeleteAddressResult.SuccessorRequired, result)
        }

    @Test
    fun `deleting the professional base with two survivors asks who inherits it`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(
                        address(id = "1", alias = "Casa", isPrimary = true),
                        address(id = "2", alias = "Consultorio", isPrimary = false, isProfessionalBase = true),
                        address(id = "3", alias = "Trabajo", isPrimary = false),
                    ),
                )

            val result = useCase("2")

            assertEquals(DeleteAddressResult.SuccessorRequired, result)
            assertTrue(repository.deleteAttempts.isEmpty())
        }

    @Test
    fun `deleting the only address asks nothing`() =
        runTest {
            repository.listResult =
                AddressListResult.Success(
                    listOf(address(id = "1", alias = "Casa", isPrimary = true, isProfessionalBase = true)),
                )

            val result = useCase("1")

            assertEquals(DeleteAddressResult.Success, result)
            assertEquals(listOf("1"), repository.deleteAttempts)
        }

    @Test
    fun `a list that cannot be read reports the error instead of deleting blind`() =
        runTest {
            repository.listResult = AddressListResult.Failure(AddressError.NetworkUnavailable)

            val result = useCase("1")

            assertEquals(DeleteAddressResult.Failure(AddressError.NetworkUnavailable), result)
            assertTrue(repository.deleteAttempts.isEmpty())
        }
}

package bo.saludencasa.features.search.domain.usecase

import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.address
import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.model.SearchOriginResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSearchOriginUseCaseTest {
    private fun useCase(result: AddressListResult): GetSearchOriginUseCase =
        GetSearchOriginUseCase(
            GetMyAddressesUseCase(FakeAddressRepository(listResult = result)),
        )

    // RN-02 splits the two addresses a dual-role person has: the primary one is
    // the home where they receive care and the professional base is where they
    // work from. Searching from the base would centre their results on their
    // workplace, which is the mirror of the rule the engine applies to the
    // other side.
    @Test
    fun usesThePrimaryAddressAndNotTheProfessionalBase() =
        runTest {
            val home =
                address(id = "home", alias = "Casa", coordinate = coordinate(-17.3835, -66.1568), isPrimary = true)
            val work =
                address(
                    id = "work",
                    alias = "Trabajo",
                    coordinate = coordinate(-17.3676, -66.1742),
                    isPrimary = false,
                    isProfessionalBase = true,
                )

            // The base first in the list, so picking the first row is not enough.
            val result = useCase(AddressListResult.Success(listOf(work, home)))()

            val found = result as SearchOriginResult.Found
            assertEquals("home", found.origin.addressId)
            assertEquals(coordinate(-17.3835, -66.1568), found.origin.coordinate)
        }

    // The first criterion of HU-11 starts at "given that I have a registered
    // address". Without one there is nothing to search around, and the screen
    // has to say so instead of searching around a point nobody declared.
    @Test
    fun reportsNoPrimaryAddressWhenNoneIsMarked() =
        runTest {
            val result = useCase(AddressListResult.Success(listOf(address(isPrimary = false))))()

            assertEquals(SearchOriginResult.Failure(SearchError.NoPrimaryAddress), result)
        }

    @Test
    fun reportsNoPrimaryAddressWhenThePersonHasNoAddresses() =
        runTest {
            val result = useCase(AddressListResult.Success(emptyList()))()

            assertEquals(SearchOriginResult.Failure(SearchError.NoPrimaryAddress), result)
        }

    // Losing the connection is not the same as having no address, and the two
    // lead the person to do different things.
    @Test
    fun anOfflineReadStaysAnOfflineProblem() =
        runTest {
            val result = useCase(AddressListResult.Failure(AddressError.NetworkUnavailable))()

            assertEquals(SearchOriginResult.Failure(SearchError.NetworkUnavailable), result)
        }
}

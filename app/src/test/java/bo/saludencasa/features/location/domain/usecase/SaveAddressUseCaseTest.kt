package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.core.vo.Coordinate
import bo.saludencasa.features.location.FakeAddressRepository
import bo.saludencasa.features.location.coordinate
import bo.saludencasa.features.location.domain.model.AddressDraft
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.SaveAddressResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaveAddressUseCaseTest {
    // The point is the reason the screen exists. A row with a written address
    // and no point is one the proximity search can never return, and INV-08
    // resolves distance inside the database, so nothing later could repair it
    // from the text.
    @Test
    fun refusesToSaveAnAddressWithNoPointOnTheMap() =
        runTest {
            val repository = FakeAddressRepository()

            val result = SaveAddressUseCase(repository)(draft(coordinate = null))

            assertEquals(SaveAddressResult.Failure(AddressError.PointNotChosen), result)
            assertEquals(0, repository.saveAttempts)
        }

    // The column admits null and an empty field is someone who had nothing to
    // add. Sending the empty string instead would store a reference that reads
    // as blank on the other party's screen and would break the length check the
    // migration added, which starts at one character.
    @Test
    fun anEmptyReferenceIsStoredAsNoReference() =
        runTest {
            val repository = FakeAddressRepository()

            SaveAddressUseCase(repository)(draft(reference = "   "))

            assertNull(repository.lastUpdate?.reference)
        }

    @Test
    fun aReferenceIsCarriedThroughForTheOtherPartyToRead() =
        runTest {
            val repository = FakeAddressRepository()

            SaveAddressUseCase(repository)(draft(reference = "Portón verde, timbre 2"))

            assertEquals("Portón verde, timbre 2", repository.lastUpdate?.reference?.value)
        }

    @Test
    fun refusesAnAliasLongerThanItsColumn() =
        runTest {
            val repository = FakeAddressRepository()

            val result = SaveAddressUseCase(repository)(draft(alias = "a".repeat(61)))

            assertEquals(SaveAddressResult.Failure(AddressError.InvalidAlias), result)
            assertEquals(0, repository.saveAttempts)
        }

    @Test
    fun refusesAnEmptyWrittenAddress() =
        runTest {
            val repository = FakeAddressRepository()

            val result = SaveAddressUseCase(repository)(draft(addressText = "  "))

            assertEquals(SaveAddressResult.Failure(AddressError.InvalidAddressText), result)
            assertEquals(0, repository.saveAttempts)
        }

    // city is not null and has no default, and the geocoder leaves it empty for
    // a point it cannot name. Without this the insert fails at the server with a
    // message that names a column the person never saw.
    @Test
    fun refusesAnEmptyCity() =
        runTest {
            val repository = FakeAddressRepository()

            val result = SaveAddressUseCase(repository)(draft(city = ""))

            assertEquals(SaveAddressResult.Failure(AddressError.InvalidCity), result)
            assertEquals(0, repository.saveAttempts)
        }

    // The identifier is what tells an edit from a first registration. Losing it
    // would add a second row every time the person corrected their address, and
    // the second one would not be the primary one.
    @Test
    fun anEditCarriesTheIdentifierOfTheRowItCorrects() =
        runTest {
            val repository = FakeAddressRepository()

            SaveAddressUseCase(repository)(draft(id = "6b1f1f2e-0000-4000-8000-000000000000"))

            assertEquals("6b1f1f2e-0000-4000-8000-000000000000", repository.lastUpdate?.id)
        }
}

private fun draft(
    id: String? = null,
    alias: String = "Casa",
    addressText: String = "Avenida Arce 2081, La Paz",
    reference: String = "Portón verde",
    city: String = "La Paz",
    coordinate: Coordinate? = coordinate(),
): AddressDraft =
    AddressDraft(
        id = id,
        alias = alias,
        addressText = addressText,
        reference = reference,
        city = city,
        coordinate = coordinate,
    )

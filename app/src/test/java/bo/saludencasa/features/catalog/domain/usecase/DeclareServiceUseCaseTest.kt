package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.professionalService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DeclareServiceUseCaseTest {
    private val repository =
        FakeCatalogRepository(
            catalog = listOf(generalConsultation, physiotherapy),
            services = listOf(professionalService(type = generalConsultation)),
        )
    private val useCase = DeclareServiceUseCase(repository)

    // HU-10's third criterion. The unique index is the guarantee, and refusing
    // here is what turns it into something the professional can act on instead
    // of a constraint name coming back from the server.
    @Test
    fun refusesATypeThatIsAlreadyDeclaredAndWritesNothing() =
        runTest {
            val result = useCase(generalConsultation.id, rawPrice = "200")

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.ServiceAlreadyDeclared), result)
            assertTrue(repository.declarations.isEmpty())
        }

    // The same call that is refused above has to succeed on a type the person
    // does not offer: a check that denied both would pass the test above and
    // leave the feature unusable.
    @Test
    fun declaresATypeTheProfessionalDoesNotOfferYet() =
        runTest {
            val result = useCase(physiotherapy.id, rawPrice = "130,50")

            assertEquals(ServiceDeclarationResult.Success, result)
            assertEquals(physiotherapy.id, repository.declarations.single().id)
            // The decimal keyboard of a device set to Spanish offers a comma,
            // and the column holds two decimals.
            assertEquals(BigDecimal("130.50"), repository.declarations.single().price)
        }

    // reference_price_bob carries check (reference_price_bob > 0). Letting a
    // zero reach the server spends a round trip to come back as a constraint
    // name, and a zero-priced service reads as free in the search results.
    @Test
    fun refusesAPriceOfZeroWithoutEvenReadingTheCatalog() =
        runTest {
            val result = useCase(physiotherapy.id, rawPrice = "0")

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.InvalidPrice), result)
            assertEquals(0, repository.reads)
            assertTrue(repository.declarations.isEmpty())
        }

    // numeric(10, 2) does not refuse a third decimal, it rounds it: 130.999
    // would be stored as 131.00 and published as a price nobody declared.
    @Test
    fun refusesAPriceWithThreeDecimalsWithoutEvenReadingTheCatalog() =
        runTest {
            val result = useCase(physiotherapy.id, rawPrice = "130.999")

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.InvalidPrice), result)
            assertEquals(0, repository.reads)
        }

    // A read that fails must not become a write against an unknown state:
    // without this, a professional with no connection would be told the
    // duplicate check passed and the insert would race the unique index.
    @Test
    fun aFailedReadStopsTheDeclarationAndCarriesItsOwnError() =
        runTest {
            repository.readFailure = CatalogError.NetworkUnavailable

            val result = useCase(physiotherapy.id, rawPrice = "130")

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.NetworkUnavailable), result)
            assertTrue(repository.declarations.isEmpty())
        }
}

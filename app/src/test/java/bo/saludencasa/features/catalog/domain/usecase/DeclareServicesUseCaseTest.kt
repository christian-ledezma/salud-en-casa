package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.physiotherapy
import bo.saludencasa.features.catalog.professionalService
import bo.saludencasa.features.catalog.venousLine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DeclareServicesUseCaseTest {
    private val repository =
        FakeCatalogRepository(
            catalog = listOf(generalConsultation, venousLine, physiotherapy),
            services = listOf(professionalService(type = generalConsultation, price = "180.00")),
        )
    private val useCase = DeclareServicesUseCase(repository)

    // HT-32's purpose: six services are one write, and each goes in at the
    // price the catalog has. A price taken from anywhere else, such as the
    // 180 this professional charges for the consultation, would publish a
    // number nobody confirmed for a type they never priced.
    @Test
    fun declaresEveryChosenTypeInOneWriteAtItsCatalogReferencePrice() =
        runTest {
            val result = useCase(setOf(venousLine.id, physiotherapy.id))

            assertEquals(ServiceDeclarationResult.Success, result)
            assertEquals(1, repository.declareCalls)
            assertEquals(
                setOf(
                    FakeCatalogRepository.PricedAttempt(venousLine.id, BigDecimal("120.00")),
                    FakeCatalogRepository.PricedAttempt(physiotherapy.id, BigDecimal("130.00")),
                ),
                repository.declarations.toSet(),
            )
        }

    // The write that refuses a duplicate must refuse the whole selection. Six
    // services declared and a seventh bounced would leave the professional
    // with half of what they asked for and an error about the other half.
    @Test
    fun refusesTheWholeSelectionWhenOneTypeIsAlreadyDeclaredAndWritesNothing() =
        runTest {
            val result = useCase(setOf(generalConsultation.id, physiotherapy.id))

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.ServiceAlreadyDeclared), result)
            assertEquals(0, repository.declareCalls)
            assertTrue(repository.declarations.isEmpty())
        }

    // The type that bounced the selection above is declarable on its own. A
    // check that refused every selection would pass the test above and leave
    // the feature unusable.
    @Test
    fun declaresTheSameTypeOnItsOwnWhenTheDuplicateIsLeftOut() =
        runTest {
            val result = useCase(setOf(physiotherapy.id))

            assertEquals(ServiceDeclarationResult.Success, result)
            assertEquals(physiotherapy.id, repository.declarations.single().id)
        }

    // The selector can sit open while an administrator takes a type out of the
    // catalog. The foreign key does not look at that flag, so without this the
    // professional would be declared in a type no patient can search for.
    @Test
    fun refusesATypeThatLeftTheCatalogAndWritesNothing() =
        runTest {
            val result = useCase(setOf(physiotherapy.id, "type-removed-meanwhile"))

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.ServiceTypeUnavailable), result)
            assertEquals(0, repository.declareCalls)
        }

    // An empty insert comes back as a success, and a success with nothing
    // written would close the selector as if the professional had declared.
    @Test
    fun refusesAnEmptySelectionWithoutEvenReadingTheCatalog() =
        runTest {
            val result = useCase(emptySet())

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.NothingSelected), result)
            assertEquals(0, repository.reads)
        }

    // A read that fails must not become a write against an unknown state.
    @Test
    fun aFailedReadStopsTheDeclarationAndCarriesItsOwnError() =
        runTest {
            repository.readFailure = CatalogError.NetworkUnavailable

            val result = useCase(setOf(physiotherapy.id))

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.NetworkUnavailable), result)
            assertEquals(0, repository.declareCalls)
        }

    @Test
    fun aFailedWriteReachesTheCallerWithItsOwnError() =
        runTest {
            repository.writeFailure = CatalogError.NetworkUnavailable

            val result = useCase(setOf(physiotherapy.id))

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.NetworkUnavailable), result)
        }
}

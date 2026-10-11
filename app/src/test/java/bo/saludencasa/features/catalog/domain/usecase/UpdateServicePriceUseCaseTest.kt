package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.features.catalog.FakeCatalogRepository
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.generalConsultation
import bo.saludencasa.features.catalog.professionalService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class UpdateServicePriceUseCaseTest {
    private val repository =
        FakeCatalogRepository(
            catalog = listOf(generalConsultation),
            services = listOf(professionalService(id = "declared-7", type = generalConsultation)),
        )
    private val useCase = UpdateServicePriceUseCase(repository)

    // The declared price is what a patient sees in the search results, so the
    // refusals that guard the first write have to guard the correction too.
    @Test
    fun refusesAnEmptyPriceAndLeavesTheStoredOneAlone() =
        runTest {
            val result = useCase("declared-7", rawPrice = "   ")

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.InvalidPrice), result)
            assertTrue(repository.repricings.isEmpty())
        }

    @Test
    fun refusesANegativePriceAndLeavesTheStoredOneAlone() =
        runTest {
            val result = useCase("declared-7", rawPrice = "-20")

            assertEquals(ServiceDeclarationResult.Failure(CatalogError.InvalidPrice), result)
            assertTrue(repository.repricings.isEmpty())
        }

    @Test
    fun writesTheCorrectedPriceAgainstTheServiceThatWasChosen() =
        runTest {
            val result = useCase("declared-7", rawPrice = "95,90")

            assertEquals(ServiceDeclarationResult.Success, result)
            assertEquals("declared-7", repository.repricings.single().id)
            assertEquals(BigDecimal("95.90"), repository.repricings.single().price)
        }
}

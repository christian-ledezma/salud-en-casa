package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository

class UpdateServicePriceUseCase(
    private val repository: ICatalogRepository,
) {
    suspend operator fun invoke(
        serviceId: String,
        rawPrice: String,
    ): ServiceDeclarationResult {
        val price =
            AmountBob.parse(rawPrice).getOrNull()
                ?: return ServiceDeclarationResult.Failure(CatalogError.InvalidPrice)

        return repository.updateServicePrice(serviceId, price)
    }
}

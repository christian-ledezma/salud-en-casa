package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository

class DeclareServiceUseCase(
    private val repository: ICatalogRepository,
) {
    suspend operator fun invoke(
        serviceTypeId: String,
        rawPrice: String,
    ): ServiceDeclarationResult {
        val price =
            AmountBob.parse(rawPrice).getOrNull()
                ?: return ServiceDeclarationResult.Failure(CatalogError.InvalidPrice)

        // The state is read again instead of trusting what the screen holds.
        // The unique index is the guarantee; this is what turns it into an
        // error the professional can act on, and the mapper covers the race
        // between the read and the write.
        val declared =
            when (val result = repository.getDeclaredServices()) {
                is DeclaredServicesResult.Loaded -> result.declaredServices
                is DeclaredServicesResult.Failure -> return ServiceDeclarationResult.Failure(result.error)
            }

        if (declared.declares(serviceTypeId)) {
            return ServiceDeclarationResult.Failure(CatalogError.ServiceAlreadyDeclared)
        }

        return repository.declareService(serviceTypeId, price)
    }
}

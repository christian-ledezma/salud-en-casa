package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ServiceDeclaration
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository

class DeclareServicesUseCase(
    private val repository: ICatalogRepository,
) {
    suspend operator fun invoke(serviceTypeIds: Set<String>): ServiceDeclarationResult {
        if (serviceTypeIds.isEmpty()) {
            return ServiceDeclarationResult.Failure(CatalogError.NothingSelected)
        }

        // The state is read again instead of trusting what the screen holds:
        // the price comes from this catalog and never from the caller, and a
        // type that left it since the selector opened is refused here and not
        // declared (docs/decisions.md, 2026-11-25, HT-32).
        val declared =
            when (val result = repository.getDeclaredServices()) {
                is DeclaredServicesResult.Loaded -> result.declaredServices
                is DeclaredServicesResult.Failure -> return ServiceDeclarationResult.Failure(result.error)
            }

        if (serviceTypeIds.any(declared::declares)) {
            return ServiceDeclarationResult.Failure(CatalogError.ServiceAlreadyDeclared)
        }

        val catalog = declared.catalog.associateBy(ServiceType::id)
        val declarations =
            serviceTypeIds.map { id ->
                val type = catalog[id] ?: return ServiceDeclarationResult.Failure(CatalogError.ServiceTypeUnavailable)
                val price =
                    AmountBob.create(type.referencePriceBob).getOrNull()
                        ?: return ServiceDeclarationResult.Failure(CatalogError.Unexpected)
                ServiceDeclaration(serviceTypeId = id, price = price)
            }

        return repository.declareServices(declarations)
    }
}

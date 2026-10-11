package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.features.catalog.domain.model.ServiceTypesResult
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository

class GetServiceTypeCatalogUseCase(
    private val repository: ICatalogRepository,
) {
    suspend operator fun invoke(): ServiceTypesResult = repository.getServiceTypes()
}

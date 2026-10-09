package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository

class RemoveServiceUseCase(
    private val repository: ICatalogRepository,
) {
    suspend operator fun invoke(serviceId: String): ServiceDeclarationResult = repository.removeService(serviceId)
}

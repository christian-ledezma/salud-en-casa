package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository

class GetMyDeclaredServicesUseCase(
    private val repository: ICatalogRepository,
) {
    suspend operator fun invoke(): DeclaredServicesResult = repository.getDeclaredServices()
}

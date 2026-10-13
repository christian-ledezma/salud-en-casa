package bo.saludencasa.features.catalog.domain.usecase

import bo.saludencasa.features.catalog.domain.model.ProfessionalServicesResult
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository

class GetProfessionalServicesUseCase(
    private val repository: ICatalogRepository,
) {
    suspend operator fun invoke(professionalId: String): ProfessionalServicesResult =
        repository.getServicesOf(professionalId)
}

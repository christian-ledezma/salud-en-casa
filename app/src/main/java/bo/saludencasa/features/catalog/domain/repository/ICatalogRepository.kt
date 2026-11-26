package bo.saludencasa.features.catalog.domain.repository

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ProfessionalServicesResult
import bo.saludencasa.features.catalog.domain.model.ServiceDeclaration
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.model.ServiceTypesResult

interface ICatalogRepository {
    suspend fun getServiceTypes(): ServiceTypesResult

    suspend fun getDeclaredServices(): DeclaredServicesResult

    suspend fun getServicesOf(professionalId: String): ProfessionalServicesResult

    suspend fun declareServices(declarations: List<ServiceDeclaration>): ServiceDeclarationResult

    suspend fun updateServicePrice(
        serviceId: String,
        price: AmountBob,
    ): ServiceDeclarationResult

    suspend fun removeService(serviceId: String): ServiceDeclarationResult
}

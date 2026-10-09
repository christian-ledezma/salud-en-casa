package bo.saludencasa.features.catalog.domain.repository

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult

interface ICatalogRepository {
    suspend fun getDeclaredServices(): DeclaredServicesResult

    suspend fun declareService(
        serviceTypeId: String,
        price: AmountBob,
    ): ServiceDeclarationResult

    suspend fun updateServicePrice(
        serviceId: String,
        price: AmountBob,
    ): ServiceDeclarationResult

    suspend fun removeService(serviceId: String): ServiceDeclarationResult
}

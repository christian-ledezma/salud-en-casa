package bo.saludencasa.features.catalog.data.repository

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.catalog.data.datasource.SupabaseCatalogDataSource
import bo.saludencasa.features.catalog.data.mapper.declaredServicesOf
import bo.saludencasa.features.catalog.data.mapper.toCatalogError
import bo.saludencasa.features.catalog.data.mapper.toServiceType
import bo.saludencasa.features.catalog.data.model.ProfessionalServiceRow
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ProfessionalServicesResult
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.model.ServiceTypesResult
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository
import kotlinx.coroutines.CancellationException

class CatalogRepository(
    private val dataSource: SupabaseCatalogDataSource,
) : ICatalogRepository {
    // The catalog alone, with no read of what the caller declared: this is how
    // the nearby search asks for the types it offers as filters.
    override suspend fun getServiceTypes(): ServiceTypesResult =
        try {
            ServiceTypesResult.Loaded(dataSource.findServiceTypes().map { it.toServiceType() })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ServiceTypesResult.Failure(failure.toCatalogError())
        }

    // docs/decisions.md, 2026-10-09, two reads instead of a PostgREST embed.
    override suspend fun getDeclaredServices(): DeclaredServicesResult {
        val userId =
            dataSource.currentUserId()
                ?: return DeclaredServicesResult.Failure(CatalogError.NotSignedIn)

        return try {
            DeclaredServicesResult.Loaded(
                declaredServicesOf(
                    types = dataSource.findServiceTypes(),
                    services = dataSource.findServicesOf(userId),
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            DeclaredServicesResult.Failure(failure.toCatalogError())
        }
    }

    // The same two reads as above, aimed at somebody else and without the
    // catalog: a visitor has nothing to declare (RF-02.6).
    override suspend fun getServicesOf(professionalId: String): ProfessionalServicesResult {
        if (dataSource.currentUserId() == null) {
            return ProfessionalServicesResult.Failure(CatalogError.NotSignedIn)
        }

        return try {
            ProfessionalServicesResult.Loaded(
                declaredServicesOf(
                    types = dataSource.findServiceTypes(),
                    services = dataSource.findServicesOf(professionalId),
                ).services,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ProfessionalServicesResult.Failure(failure.toCatalogError())
        }
    }

    override suspend fun declareService(
        serviceTypeId: String,
        price: AmountBob,
    ): ServiceDeclarationResult {
        val userId =
            dataSource.currentUserId()
                ?: return ServiceDeclarationResult.Failure(CatalogError.NotSignedIn)

        return try {
            dataSource.insertService(
                ProfessionalServiceRow(
                    professionalId = userId,
                    serviceTypeId = serviceTypeId,
                    referencePriceBob = price.value,
                ),
            )
            ServiceDeclarationResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ServiceDeclarationResult.Failure(failure.toCatalogError())
        }
    }

    override suspend fun updateServicePrice(
        serviceId: String,
        price: AmountBob,
    ): ServiceDeclarationResult =
        try {
            dataSource.updateServicePrice(serviceId, price.value)
            ServiceDeclarationResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ServiceDeclarationResult.Failure(failure.toCatalogError())
        }

    override suspend fun removeService(serviceId: String): ServiceDeclarationResult =
        try {
            dataSource.deleteService(serviceId)
            ServiceDeclarationResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ServiceDeclarationResult.Failure(failure.toCatalogError())
        }
}

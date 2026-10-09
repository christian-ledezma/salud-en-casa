package bo.saludencasa.features.catalog

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.catalog.domain.model.CatalogError
import bo.saludencasa.features.catalog.domain.model.DeclaredServices
import bo.saludencasa.features.catalog.domain.model.DeclaredServicesResult
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceDeclarationResult
import bo.saludencasa.features.catalog.domain.model.ServiceType
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository
import java.math.BigDecimal

// The fake keeps the rows it is told to write, so a declaration shows up in the
// next read the way it does against the server. A fake that only recorded the
// call would make every reload assertion pass without the write ever landing.
class FakeCatalogRepository(
    var catalog: List<ServiceType> = emptyList(),
    services: List<ProfessionalService> = emptyList(),
) : ICatalogRepository {
    data class PricedAttempt(
        val id: String,
        val price: BigDecimal,
    )

    var readFailure: CatalogError? = null
    var writeFailure: CatalogError? = null

    var reads: Int = 0
        private set
    val declarations: MutableList<PricedAttempt> = mutableListOf()
    val repricings: MutableList<PricedAttempt> = mutableListOf()
    val removals: MutableList<String> = mutableListOf()

    private val stored = services.toMutableList()

    override suspend fun getDeclaredServices(): DeclaredServicesResult {
        reads++
        readFailure?.let { return DeclaredServicesResult.Failure(it) }

        return DeclaredServicesResult.Loaded(
            DeclaredServices(services = stored.sortedBy { it.type.name }, catalog = catalog),
        )
    }

    override suspend fun declareService(
        serviceTypeId: String,
        price: AmountBob,
    ): ServiceDeclarationResult {
        declarations += PricedAttempt(serviceTypeId, price.value)
        writeFailure?.let { return ServiceDeclarationResult.Failure(it) }

        val type =
            catalog.firstOrNull { it.id == serviceTypeId }
                ?: error("The test declared $serviceTypeId, which is not in the fake catalog.")
        stored += professionalService(id = "declared-${stored.size}", type = type, price = price.value.toPlainString())
        return ServiceDeclarationResult.Success
    }

    override suspend fun updateServicePrice(
        serviceId: String,
        price: AmountBob,
    ): ServiceDeclarationResult {
        repricings += PricedAttempt(serviceId, price.value)
        writeFailure?.let { return ServiceDeclarationResult.Failure(it) }

        val index = stored.indexOfFirst { it.id == serviceId }
        if (index >= 0) stored[index] = stored[index].copy(referencePriceBob = price.value)
        return ServiceDeclarationResult.Success
    }

    override suspend fun removeService(serviceId: String): ServiceDeclarationResult {
        removals += serviceId
        writeFailure?.let { return ServiceDeclarationResult.Failure(it) }

        stored.removeAll { it.id == serviceId }
        return ServiceDeclarationResult.Success
    }
}

// Real catalog entries: the longest of the twelve is what the layout tests
// need, and a made-up string would not reproduce the width.
val generalConsultation: ServiceType =
    serviceType(
        id = "type-consultation",
        name = "Consulta médica general",
        price = "150.00",
        durationMin = 45,
        description = "Valoración médica en domicilio, anamnesis y examen físico.",
    )

val venousLine: ServiceType =
    serviceType(
        id = "type-venous-line",
        name = "Colocación y control de vía venosa",
        price = "120.00",
        durationMin = 60,
        description = "Canalización de vía periférica y administración de suero o medicación.",
    )

val physiotherapy: ServiceType =
    serviceType(
        id = "type-physiotherapy",
        name = "Terapia física y rehabilitación",
        price = "130.00",
        durationMin = 60,
        description = "Sesión de fisioterapia en domicilio según plan de tratamiento.",
    )

fun serviceType(
    id: String = "type-consultation",
    name: String = "Consulta médica general",
    price: String = "150.00",
    durationMin: Int = 45,
    description: String? = null,
): ServiceType =
    ServiceType(
        id = id,
        name = name,
        description = description,
        referencePriceBob = BigDecimal(price),
        estimatedDurationMin = durationMin,
    )

fun professionalService(
    id: String = "declared-0",
    type: ServiceType = generalConsultation,
    price: String = "180.00",
): ProfessionalService =
    ProfessionalService(
        id = id,
        type = type,
        referencePriceBob = BigDecimal(price),
    )

fun declaredServices(
    services: List<ProfessionalService> = emptyList(),
    catalog: List<ServiceType> = emptyList(),
): DeclaredServices = DeclaredServices(services = services, catalog = catalog)

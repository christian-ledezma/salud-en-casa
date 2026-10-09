package bo.saludencasa.features.catalog.data.mapper

import bo.saludencasa.features.catalog.data.model.ProfessionalServiceDto
import bo.saludencasa.features.catalog.data.model.ServiceTypeDto
import bo.saludencasa.features.catalog.domain.model.DeclaredServices
import bo.saludencasa.features.catalog.domain.model.ProfessionalService
import bo.saludencasa.features.catalog.domain.model.ServiceType

internal fun ServiceTypeDto.toServiceType(): ServiceType =
    ServiceType(
        id = id,
        name = name,
        description = description,
        referencePriceBob = referencePriceBob,
        estimatedDurationMin = estimatedDurationMin,
    )

internal fun declaredServicesOf(
    types: List<ServiceTypeDto>,
    services: List<ProfessionalServiceDto>,
): DeclaredServices {
    val catalog = types.map(ServiceTypeDto::toServiceType)
    val byId = catalog.associateBy(ServiceType::id)

    return DeclaredServices(
        // docs/decisions.md, 2026-10-09, a declared service whose type left
        // the catalog disappears from the screen.
        services =
            services
                .mapNotNull { service ->
                    byId[service.serviceTypeId]?.let { type ->
                        ProfessionalService(
                            id = service.id,
                            type = type,
                            referencePriceBob = service.referencePriceBob,
                        )
                    }
                }.sortedBy { it.type.name },
        catalog = catalog,
    )
}

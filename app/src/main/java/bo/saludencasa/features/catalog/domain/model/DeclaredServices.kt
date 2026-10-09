package bo.saludencasa.features.catalog.domain.model

data class DeclaredServices(
    val services: List<ProfessionalService>,
    val catalog: List<ServiceType>,
) {
    val undeclaredTypes: List<ServiceType> = catalog.filterNot { type -> declares(type.id) }

    fun declares(serviceTypeId: String): Boolean = services.any { it.type.id == serviceTypeId }
}

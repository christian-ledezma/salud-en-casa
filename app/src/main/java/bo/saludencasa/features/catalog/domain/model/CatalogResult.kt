package bo.saludencasa.features.catalog.domain.model

sealed interface DeclaredServicesResult {
    data class Loaded(
        val declaredServices: DeclaredServices,
    ) : DeclaredServicesResult

    data class Failure(
        val error: CatalogError,
    ) : DeclaredServicesResult
}

sealed interface ServiceDeclarationResult {
    data object Success : ServiceDeclarationResult

    data class Failure(
        val error: CatalogError,
    ) : ServiceDeclarationResult
}

sealed interface ServiceTypesResult {
    data class Loaded(
        val types: List<ServiceType>,
    ) : ServiceTypesResult

    data class Failure(
        val error: CatalogError,
    ) : ServiceTypesResult
}

// The visitor's read: what this professional offers today, with no catalog and
// no undeclared types, which are the owner's business (docs/decisions.md,
// 2026-10-11).
sealed interface ProfessionalServicesResult {
    data class Loaded(
        val services: List<ProfessionalService>,
    ) : ProfessionalServicesResult

    data class Failure(
        val error: CatalogError,
    ) : ProfessionalServicesResult
}

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

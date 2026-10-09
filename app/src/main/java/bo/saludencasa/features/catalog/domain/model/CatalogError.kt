package bo.saludencasa.features.catalog.domain.model

sealed interface CatalogError {
    data object NotSignedIn : CatalogError

    data object InvalidPrice : CatalogError

    data object ServiceAlreadyDeclared : CatalogError

    data object NetworkUnavailable : CatalogError

    data object Unexpected : CatalogError
}

package bo.saludencasa.features.catalog.presentation

import androidx.annotation.StringRes
import bo.saludencasa.R
import bo.saludencasa.features.catalog.domain.model.CatalogError

@StringRes
internal fun CatalogError.messageRes(): Int =
    when (this) {
        CatalogError.NotSignedIn -> R.string.error_catalog_not_signed_in
        CatalogError.InvalidPrice -> R.string.error_catalog_invalid_price
        CatalogError.ServiceAlreadyDeclared -> R.string.error_catalog_service_already_declared
        CatalogError.NetworkUnavailable -> R.string.error_network_unavailable
        CatalogError.Unexpected -> R.string.error_unexpected
    }

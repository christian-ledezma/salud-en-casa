package bo.saludencasa.features.search.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import bo.saludencasa.R
import bo.saludencasa.core.util.formatKilometres
import bo.saludencasa.features.search.domain.model.SearchError

@StringRes
internal fun SearchError.messageRes(): Int =
    when (this) {
        SearchError.NotSignedIn -> R.string.error_search_not_signed_in
        SearchError.NoPrimaryAddress -> R.string.error_search_no_primary_address
        SearchError.UnreadableResult -> R.string.error_search_unreadable_result
        SearchError.NetworkUnavailable -> R.string.error_network_unavailable
        SearchError.Unexpected -> R.string.error_unexpected
    }

// Zero is a value that happens, because the engine rounds to the hundred
// metres, and "a unos 0 m" reads as a defect.
@Composable
internal fun distanceText(distanceM: Int): String {
    val locale = LocalConfiguration.current.locales[0]

    return when {
        distanceM < METRES_IN_A_HUNDRED -> stringResource(R.string.search_distance_near)
        distanceM < METRES_IN_A_KILOMETRE -> stringResource(R.string.search_distance_m, distanceM)
        else -> stringResource(R.string.search_distance_km, formatKilometres(distanceM, locale))
    }
}

private const val METRES_IN_A_HUNDRED = 100
private const val METRES_IN_A_KILOMETRE = 1_000

package bo.saludencasa.features.search.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import bo.saludencasa.R
import bo.saludencasa.core.util.distanceText
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

@Composable
internal fun distanceText(distanceM: Int): String =
    distanceText(LocalResources.current, distanceM, LocalConfiguration.current.locales[0])

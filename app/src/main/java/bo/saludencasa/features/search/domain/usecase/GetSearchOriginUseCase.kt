package bo.saludencasa.features.search.domain.usecase

import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.search.domain.model.SearchError
import bo.saludencasa.features.search.domain.model.SearchOrigin
import bo.saludencasa.features.search.domain.model.SearchOriginResult

class GetSearchOriginUseCase(
    private val getMyAddresses: GetMyAddressesUseCase,
) {
    suspend operator fun invoke(): SearchOriginResult =
        when (val result = getMyAddresses()) {
            is AddressListResult.Success -> result.addresses.toOrigin()
            is AddressListResult.Failure -> SearchOriginResult.Failure(result.error.toSearchError())
        }

    // RN-02 draws the line this reads: the primary address is the home where
    // the person receives care, and the professional base is where they work
    // from. Searching from the base would centre a dual-role person's results
    // on their workplace.
    private fun List<Address>.toOrigin(): SearchOriginResult {
        val primary =
            firstOrNull { it.isPrimary }
                ?: return SearchOriginResult.Failure(SearchError.NoPrimaryAddress)

        return SearchOriginResult.Found(
            SearchOrigin(
                addressId = primary.id,
                alias = primary.alias,
                addressText = primary.addressText,
                coordinate = primary.coordinate,
            ),
        )
    }
}

private fun AddressError.toSearchError(): SearchError =
    when (this) {
        AddressError.NotSignedIn -> SearchError.NotSignedIn
        AddressError.NetworkUnavailable -> SearchError.NetworkUnavailable
        else -> SearchError.Unexpected
    }

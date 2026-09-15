package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.AddressDraft
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressUpdate
import bo.saludencasa.features.location.domain.model.SaveAddressResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository
import bo.saludencasa.features.location.domain.vo.AddressAlias
import bo.saludencasa.features.location.domain.vo.AddressReference
import bo.saludencasa.features.location.domain.vo.AddressText
import bo.saludencasa.features.location.domain.vo.CityName

class SaveAddressUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(draft: AddressDraft): SaveAddressResult {
        // The point is the reason the screen exists. A written address with no
        // point saves a row the proximity search can never return, and INV-08
        // resolves distance inside the database, so nothing later could repair
        // it from the text.
        val coordinate = draft.coordinate ?: return failure(AddressError.PointNotChosen)

        val alias =
            AddressAlias.create(draft.alias).getOrNull()
                ?: return failure(AddressError.InvalidAlias)

        val addressText =
            AddressText.create(draft.addressText).getOrNull()
                ?: return failure(AddressError.InvalidAddressText)

        val city =
            CityName.create(draft.city).getOrNull()
                ?: return failure(AddressError.InvalidCity)

        // The reference is the only optional text of the form, and the column
        // admits null. An empty field is someone who had nothing to add, not a
        // field to complain about.
        val reference =
            draft.reference.takeIf(String::isNotBlank)?.let { raw ->
                AddressReference.create(raw).getOrNull()
                    ?: return failure(AddressError.InvalidReference)
            }

        return repository.saveAddress(
            AddressUpdate(
                id = draft.id,
                alias = alias,
                addressText = addressText,
                reference = reference,
                city = city,
                coordinate = coordinate,
            ),
        )
    }
}

private fun failure(error: AddressError): SaveAddressResult = SaveAddressResult.Failure(error)

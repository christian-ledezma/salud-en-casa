package bo.saludencasa.features.location.domain.repository

import bo.saludencasa.features.location.domain.model.AddressUpdate
import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.model.SaveAddressResult

interface IAddressRepository {
    suspend fun getMyPrimaryAddress(): MyAddressResult

    suspend fun saveAddress(update: AddressUpdate): SaveAddressResult
}

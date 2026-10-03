package bo.saludencasa.features.location.domain.repository

import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.AddressUpdate
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.model.SaveAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.model.SetProfessionalBaseResult

interface IAddressRepository {
    suspend fun getMyAddresses(): AddressListResult

    suspend fun getAddress(id: String): MyAddressResult

    suspend fun saveAddress(update: AddressUpdate): SaveAddressResult

    suspend fun setPrimaryAddress(id: String): SetPrimaryAddressResult

    suspend fun setProfessionalBaseAddress(id: String): SetProfessionalBaseResult

    suspend fun deleteAddress(id: String): DeleteAddressResult
}

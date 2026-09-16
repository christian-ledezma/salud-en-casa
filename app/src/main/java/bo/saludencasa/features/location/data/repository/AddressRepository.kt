package bo.saludencasa.features.location.data.repository

import bo.saludencasa.features.location.data.datasource.SupabaseAddressDataSource
import bo.saludencasa.features.location.data.mapper.toAddress
import bo.saludencasa.features.location.data.mapper.toAddressError
import bo.saludencasa.features.location.data.mapper.toAddressRow
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.AddressUpdate
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.MyAddressResult
import bo.saludencasa.features.location.domain.model.SaveAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository
import kotlinx.coroutines.CancellationException

class AddressRepository(
    private val dataSource: SupabaseAddressDataSource,
) : IAddressRepository {
    override suspend fun getMyAddresses(): AddressListResult {
        if (dataSource.currentUserId() == null) return AddressListResult.Failure(AddressError.NotSignedIn)

        return try {
            AddressListResult.Success(dataSource.findAllAddresses().mapNotNull { it.toAddress() })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AddressListResult.Failure(failure.toAddressError())
        }
    }

    override suspend fun getAddress(id: String): MyAddressResult {
        if (dataSource.currentUserId() == null) return MyAddressResult.Failure(AddressError.NotSignedIn)

        return try {
            val stored = dataSource.findAddress(id) ?: return MyAddressResult.NotRegistered
            stored.toAddress()?.let(MyAddressResult::Registered)
                ?: MyAddressResult.Failure(AddressError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            MyAddressResult.Failure(failure.toAddressError())
        }
    }

    override suspend fun saveAddress(update: AddressUpdate): SaveAddressResult {
        val userId = dataSource.currentUserId() ?: return SaveAddressResult.Failure(AddressError.NotSignedIn)

        return try {
            val row = update.toAddressRow(userId)
            val id =
                if (update.id == null) {
                    dataSource.insertAddress(row)
                } else {
                    dataSource.updateAddress(update.id, row)
                    update.id
                }

            // The screen is redrawn from the row the server stored and not from
            // the text that was typed, so the point it shows is the one that
            // came back out of the geography column.
            dataSource.findAddress(id)?.toAddress()?.let(SaveAddressResult::Success)
                ?: SaveAddressResult.Failure(AddressError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            SaveAddressResult.Failure(failure.toAddressError())
        }
    }

    override suspend fun setPrimaryAddress(id: String): SetPrimaryAddressResult {
        if (dataSource.currentUserId() == null) return SetPrimaryAddressResult.Failure(AddressError.NotSignedIn)

        return try {
            dataSource.setPrimary(id)
            SetPrimaryAddressResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            SetPrimaryAddressResult.Failure(failure.toAddressError())
        }
    }

    override suspend fun deleteAddress(id: String): DeleteAddressResult {
        if (dataSource.currentUserId() == null) return DeleteAddressResult.Failure(AddressError.NotSignedIn)

        return try {
            dataSource.deleteAddress(id)
            DeleteAddressResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            DeleteAddressResult.Failure(failure.toAddressError())
        }
    }
}

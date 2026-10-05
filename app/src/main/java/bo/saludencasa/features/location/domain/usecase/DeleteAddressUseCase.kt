package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressError
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.model.SetProfessionalBaseResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

// docs/decisions.md, 2026-10-03: successor choice on marked-address delete.
class DeleteAddressUseCase(
    private val repository: IAddressRepository,
) {
    suspend operator fun invoke(
        id: String,
        successorId: String? = null,
    ): DeleteAddressResult {
        val addresses =
            when (val result = repository.getMyAddresses()) {
                is AddressListResult.Success -> result.addresses
                is AddressListResult.Failure -> return DeleteAddressResult.Failure(result.error)
            }

        val target = addresses.firstOrNull { it.id == id } ?: return delete(id)
        if (!target.carriesAMark()) return delete(id)

        val survivors = addresses.filter { it.id != id }
        if (survivors.size < 2) return delete(id)

        val successor =
            survivors.firstOrNull { it.id == successorId }
                ?: return DeleteAddressResult.SuccessorRequired

        // addresses_guard_marked_delete refuses a delete while the row is marked.
        if (target.isPrimary) {
            val moved = repository.setPrimaryAddress(successor.id)
            if (moved is SetPrimaryAddressResult.Failure) {
                return DeleteAddressResult.Failure(moved.error)
            }
        }
        if (target.isProfessionalBase) {
            val moved = repository.setProfessionalBaseAddress(successor.id)
            if (moved is SetProfessionalBaseResult.Failure) {
                return DeleteAddressResult.Failure(moved.error)
            }
        }

        return delete(id)
    }

    // docs/decisions.md, 2026-10-04: an engine refusal is the same answer.
    private suspend fun delete(id: String): DeleteAddressResult {
        val result = repository.deleteAddress(id)
        return if (result is DeleteAddressResult.Failure && result.error == AddressError.SuccessorRequired) {
            DeleteAddressResult.SuccessorRequired
        } else {
            result
        }
    }

    private fun Address.carriesAMark(): Boolean = isPrimary || isProfessionalBase
}

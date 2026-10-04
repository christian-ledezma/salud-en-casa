package bo.saludencasa.features.location.domain.usecase

import bo.saludencasa.features.location.domain.model.Address
import bo.saludencasa.features.location.domain.model.AddressListResult
import bo.saludencasa.features.location.domain.model.DeleteAddressResult
import bo.saludencasa.features.location.domain.model.SetPrimaryAddressResult
import bo.saludencasa.features.location.domain.model.SetProfessionalBaseResult
import bo.saludencasa.features.location.domain.repository.IAddressRepository

// Deleting the primary address or the professional base used to leave the
// person with neither, because the trigger that marks one only fires on insert.
// With a single address left the database promotes it; with two or more the
// choice is the person's, and that is the case this use case resolves before
// writing anything. See docs/decisions.md, 2026-10-03.
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

        val target = addresses.firstOrNull { it.id == id } ?: return repository.deleteAddress(id)
        if (!target.carriesAMark()) return repository.deleteAddress(id)

        val survivors = addresses.filter { it.id != id }
        if (survivors.size < 2) return repository.deleteAddress(id)

        val successor =
            survivors.firstOrNull { it.id == successorId }
                ?: return DeleteAddressResult.SuccessorRequired

        // The marks move first so the row being deleted no longer carries them,
        // which is also what lets addresses_guard_marked_delete through. A
        // failure here stops before the delete, so nothing is lost.
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

        return repository.deleteAddress(id)
    }

    private fun Address.carriesAMark(): Boolean = isPrimary || isProfessionalBase
}

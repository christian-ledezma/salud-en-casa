package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

// Only what the screen shows; create_scheduled_request enforces INV-16 itself.
class CanScheduleAppointmentUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(professionalId: String): Boolean {
        val roles = (profileRepository.getRoles() as? RoleResult.Loaded)?.roles ?: return false
        val callerId = profileRepository.currentUserId() ?: return false
        return roles.active == UserRole.PATIENT && callerId != professionalId
    }
}

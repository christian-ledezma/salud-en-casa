package bo.saludencasa.features.verification.domain.usecase

import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.GetRolesUseCase
import bo.saludencasa.features.verification.domain.model.MyDocumentsResult
import bo.saludencasa.features.verification.domain.model.VerificationChecklist
import bo.saludencasa.features.verification.domain.model.VerificationChecklistResult
import bo.saludencasa.features.verification.domain.model.VerificationError
import bo.saludencasa.features.verification.domain.repository.IVerificationRepository

class GetMyVerificationChecklistUseCase(
    private val repository: IVerificationRepository,
    private val getRoles: GetRolesUseCase,
    private val getProfile: GetProfileUseCase,
) {
    suspend operator fun invoke(): VerificationChecklistResult {
        val roles =
            when (val result = getRoles()) {
                is RoleResult.Loaded -> result.roles
                is RoleResult.Failure -> return VerificationChecklistResult.Failure(result.error.toVerificationError())
            }

        val professionalType =
            if (roles.has(UserRole.PROFESSIONAL)) {
                when (val result = getProfile()) {
                    is ProfileResult.Success -> {
                        result.profile.professional?.professionalType
                    }

                    is ProfileResult.Failure -> {
                        return VerificationChecklistResult.Failure(result.error.toVerificationError())
                    }
                }
            } else {
                null
            }

        val documents =
            when (val result = repository.getMyDocuments()) {
                is MyDocumentsResult.Loaded -> result.documents
                is MyDocumentsResult.Failure -> return VerificationChecklistResult.Failure(result.error)
            }

        return VerificationChecklistResult.Loaded(
            checklist = VerificationChecklist.create(roles, professionalType, documents),
        )
    }

    // Keeps a connectivity failure that happened while reading roles or
    // profile from reaching the screen as "unexpected".
    private fun ProfileError.toVerificationError(): VerificationError =
        when (this) {
            ProfileError.NetworkUnavailable -> VerificationError.NetworkUnavailable
            ProfileError.NotSignedIn -> VerificationError.NotSignedIn
            else -> VerificationError.Unexpected
        }
}

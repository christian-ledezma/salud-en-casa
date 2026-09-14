package bo.saludencasa.features.profile

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.ChooseRoleResult
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.UserProfile
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.repository.IProfileRepository

class FakeProfileRepository(
    var roleResult: RoleResult = RoleResult.Unassigned,
    var assignResult: ChooseRoleResult? = null,
    var profileResult: ProfileResult = ProfileResult.Success(userProfile()),
    var saveResult: ProfileResult? = null,
) : IProfileRepository {
    var roleReads: Int = 0
        private set
    var assignAttempts: Int = 0
        private set
    var lastAssignedRole: AssignableRole? = null
        private set
    var profileReads: Int = 0
        private set
    var saveAttempts: Int = 0
        private set
    var lastUpdate: ProfileUpdate? = null
        private set

    override suspend fun getRole(): RoleResult {
        roleReads++
        return roleResult
    }

    override suspend fun assignRole(role: AssignableRole): ChooseRoleResult {
        assignAttempts++
        lastAssignedRole = role
        return assignResult ?: ChooseRoleResult.Success(role.role)
    }

    override suspend fun getProfile(): ProfileResult {
        profileReads++
        return profileResult
    }

    override suspend fun saveProfile(update: ProfileUpdate): ProfileResult {
        saveAttempts++
        lastUpdate = update
        return saveResult ?: profileResult
    }
}

fun userProfile(
    userId: String = "08ddb28f-0000-4000-8000-000000000000",
    fullName: String = "Ana Quispe",
    phone: String? = "+59171234567",
    photoUrl: String? = "https://lh3.googleusercontent.com/a/ana",
    role: UserRole? = UserRole.PATIENT,
    patient: PatientDetails? = PatientDetails(null, null, null),
): UserProfile =
    UserProfile(
        userId = userId,
        fullName = fullName,
        email = Email.create("ana.quispe@example.com").getOrNull(),
        phone = phone?.let { PhoneNumber.create(it).getOrNull() },
        photoUrl = photoUrl,
        role = role,
        patient = patient,
    )

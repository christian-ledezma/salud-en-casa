package bo.saludencasa.features.profile

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.model.AddRoleResult
import bo.saludencasa.features.profile.domain.model.AssignableRole
import bo.saludencasa.features.profile.domain.model.AvailabilityResult
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfessionalDetails
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileRoles
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.profile.domain.model.PublicProfileResult
import bo.saludencasa.features.profile.domain.model.RoleResult
import bo.saludencasa.features.profile.domain.model.SwitchRoleResult
import bo.saludencasa.features.profile.domain.model.UserProfile
import bo.saludencasa.features.profile.domain.model.UserRole
import bo.saludencasa.features.profile.domain.repository.IProfileRepository
import java.math.BigDecimal

class FakeProfileRepository(
    var roleResult: RoleResult = RoleResult.Loaded(ProfileRoles.none),
    var addResult: AddRoleResult? = null,
    var switchResult: SwitchRoleResult? = null,
    var profileResult: ProfileResult = ProfileResult.Success(userProfile()),
    var saveResult: ProfileResult? = null,
    var availabilityResult: AvailabilityResult? = null,
    var publicProfileResult: PublicProfileResult = PublicProfileResult.Success(publicProfile()),
) : IProfileRepository {
    var roleReads: Int = 0
        private set
    var addAttempts: Int = 0
        private set
    var lastAddedRole: AssignableRole? = null
        private set
    var switchAttempts: Int = 0
        private set
    var lastActiveRole: UserRole? = null
        private set
    var profileReads: Int = 0
        private set
    var saveAttempts: Int = 0
        private set
    var lastUpdate: ProfileUpdate? = null
        private set
    var availabilityWrites: Int = 0
        private set
    var lastAvailability: Boolean? = null
        private set
    var lastRequestedProfessionalId: String? = null
        private set

    override suspend fun getRoles(): RoleResult {
        roleReads++
        return roleResult
    }

    // A successful write moves the stored role state, the way the server does.
    // Without it a test would have to pre-set roleResult, and the pre-set value
    // would reach the read that AddRoleUseCase performs before writing, which is
    // the opposite of the situation under test.
    override suspend fun addRole(role: AssignableRole): AddRoleResult {
        addAttempts++
        lastAddedRole = role
        val result = addResult ?: AddRoleResult.Success(role.role)
        if (result is AddRoleResult.Success) {
            // add_my_role inserts the role and leaves it active.
            roleResult = loadedRoles(held = heldRoles() + role.role, active = role.role)
        }
        return result
    }

    override suspend fun setActiveRole(role: UserRole): SwitchRoleResult {
        switchAttempts++
        lastActiveRole = role
        val result = switchResult ?: SwitchRoleResult.Success(role)
        if (result is SwitchRoleResult.Success) {
            roleResult = loadedRoles(held = heldRoles() + role, active = role)
        }
        return result
    }

    private fun heldRoles(): Set<UserRole> = (roleResult as? RoleResult.Loaded)?.roles?.held ?: emptySet()

    override suspend fun getProfile(): ProfileResult {
        profileReads++
        return profileResult
    }

    override suspend fun saveProfile(update: ProfileUpdate): ProfileResult {
        saveAttempts++
        lastUpdate = update
        return saveResult ?: profileResult
    }

    override suspend fun setAvailableNow(availableNow: Boolean): AvailabilityResult {
        availabilityWrites++
        lastAvailability = availableNow
        return availabilityResult ?: AvailabilityResult.Success(availableNow)
    }

    override suspend fun getPublicProfile(professionalId: String): PublicProfileResult {
        lastRequestedProfessionalId = professionalId
        return publicProfileResult
    }
}

fun profileRoles(
    held: Set<UserRole> = setOf(UserRole.PATIENT),
    active: UserRole? = UserRole.PATIENT,
): ProfileRoles = ProfileRoles.create(held, active).getOrThrow()

fun loadedRoles(
    held: Set<UserRole> = setOf(UserRole.PATIENT),
    active: UserRole? = UserRole.PATIENT,
): RoleResult = RoleResult.Loaded(profileRoles(held, active))

fun userProfile(
    userId: String = "08ddb28f-0000-4000-8000-000000000000",
    fullName: String = "Ana Quispe",
    phone: String? = "+59171234567",
    photoUrl: String? = "https://lh3.googleusercontent.com/a/ana",
    activeRole: UserRole? = UserRole.PATIENT,
    patient: PatientDetails? = PatientDetails(null, null, null),
    professional: ProfessionalDetails? = null,
): UserProfile =
    UserProfile(
        userId = userId,
        fullName = fullName,
        email = Email.create("ana.quispe@example.com").getOrNull(),
        phone = phone?.let { PhoneNumber.create(it).getOrNull() },
        photoUrl = photoUrl,
        activeRole = activeRole,
        patient = patient,
        professional = professional,
    )

fun professionalDetails(
    professionalType: ProfessionalType? = ProfessionalType.NURSE,
    specialty: String? = "Enfermería geriátrica",
    biography: String? = "Diez años atendiendo a domicilio.",
    yearsOfExperience: Int = 10,
    baseRateBob: BigDecimal? = BigDecimal("120.00"),
    coverageRadiusKm: BigDecimal = BigDecimal("8.00"),
    availableNow: Boolean = false,
    isVerified: Boolean = false,
): ProfessionalDetails =
    ProfessionalDetails(
        professionalType = professionalType,
        specialty = specialty,
        biography = biography,
        yearsOfExperience = yearsOfExperience,
        baseRateBob = baseRateBob,
        coverageRadiusKm = coverageRadiusKm,
        availableNow = availableNow,
        isVerified = isVerified,
    )

fun professionalProfile(professional: ProfessionalDetails = professionalDetails()): UserProfile =
    userProfile(
        userId = "fcab94c0-0000-4000-8000-000000000000",
        activeRole = UserRole.PROFESSIONAL,
        patient = null,
        professional = professional,
    )

// Someone who holds both roles carries both sections at once (RF-01.8). Which
// one the screen edits is decided by activeRole, never by which section loaded.
fun dualRoleProfile(
    activeRole: UserRole = UserRole.PATIENT,
    professional: ProfessionalDetails = professionalDetails(),
): UserProfile =
    userProfile(
        activeRole = activeRole,
        patient = PatientDetails(null, null, null),
        professional = professional,
    )

fun publicProfile(
    professionalId: String = "fcab94c0-0000-4000-8000-000000000000",
    baseRateBob: BigDecimal? = BigDecimal("120.00"),
    totalReviews: Int = 12,
): PublicProfile =
    PublicProfile(
        professionalId = professionalId,
        fullName = "Ana Quispe",
        photoUrl = null,
        averageRating = 4.8,
        totalReviews = totalReviews,
        totalServices = 34,
        professionalType = ProfessionalType.NURSE,
        specialty = "Enfermería geriátrica",
        biography = "Diez años atendiendo a domicilio.",
        yearsOfExperience = 10,
        baseRateBob = baseRateBob,
        availableNow = true,
    )

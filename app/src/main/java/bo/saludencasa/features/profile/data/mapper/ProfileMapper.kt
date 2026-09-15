package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.data.model.PatientDto
import bo.saludencasa.features.profile.data.model.ProfessionalDto
import bo.saludencasa.features.profile.data.model.ProfileDto
import bo.saludencasa.features.profile.data.model.PublicProfileDto
import bo.saludencasa.features.profile.data.model.SaveProfileParams
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfessionalDetails
import bo.saludencasa.features.profile.domain.model.ProfessionalType
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.PublicProfile
import bo.saludencasa.features.profile.domain.model.UserProfile
import bo.saludencasa.features.profile.domain.vo.BirthDate
import java.time.LocalDate

internal fun ProfileDto.toUserProfile(
    patient: PatientDto?,
    professional: ProfessionalDto?,
): UserProfile =
    UserProfile(
        userId = id,
        fullName = fullName,
        email = email?.let { Email.create(it).getOrNull() },
        phone = phone?.let { PhoneNumber.create(it).getOrNull() },
        photoUrl = photoUrl?.takeIf(String::isNotBlank),
        role = role?.toUserRole(),
        patient = patient?.toPatientDetails(),
        professional = professional?.toProfessionalDetails(),
    )

internal fun PatientDto.toPatientDetails(): PatientDetails =
    PatientDetails(
        birthDate = birthDate?.toBirthDate(),
        emergencyContact = emergencyContact?.takeIf(String::isNotBlank),
        medicalNotes = medicalNotes?.takeIf(String::isNotBlank),
    )

internal fun ProfessionalDto.toProfessionalDetails(): ProfessionalDetails =
    ProfessionalDetails(
        professionalType = professionalType?.toProfessionalType(),
        specialty = specialty?.takeIf(String::isNotBlank),
        biography = biography?.takeIf(String::isNotBlank),
        yearsOfExperience = yearsOfExperience,
        baseRateBob = baseRateBob,
        coverageRadiusKm = coverageRadiusKm,
        availableNow = availableNow,
    )

internal fun PublicProfileDto.toPublicProfile(): PublicProfile =
    PublicProfile(
        professionalId = id,
        fullName = fullName,
        photoUrl = photoUrl?.takeIf(String::isNotBlank),
        averageRating = averageRating,
        totalReviews = totalReviews,
        totalServices = totalServices,
        professionalType = professionalType?.toProfessionalType(),
        specialty = specialty?.takeIf(String::isNotBlank),
        biography = biography?.takeIf(String::isNotBlank),
        yearsOfExperience = yearsOfExperience,
        baseRateBob = baseRateBob,
        availableNow = availableNow,
    )

internal fun ProfileUpdate.toSaveProfileParams(): SaveProfileParams =
    SaveProfileParams(
        fullName = fullName.value,
        phone = phone?.value,
        birthDate = patient?.birthDate?.value?.toString(),
        emergencyContact = patient?.emergencyContact,
        medicalNotes = patient?.medicalNotes,
        professionalType = professional?.professionalType?.name,
        specialty = professional?.specialty,
        biography = professional?.biography,
        yearsOfExperience = professional?.yearsOfExperience?.value,
        baseRateBob = professional?.baseRateBob?.value,
        coverageRadiusKm = professional?.coverageRadiusKm?.value,
    )

internal fun String.toProfessionalType(): ProfessionalType? = ProfessionalType.entries.firstOrNull { it.name == this }

private fun String.toBirthDate(): BirthDate? =
    runCatching { LocalDate.parse(this) }
        .getOrNull()
        ?.let { BirthDate.create(it).getOrNull() }

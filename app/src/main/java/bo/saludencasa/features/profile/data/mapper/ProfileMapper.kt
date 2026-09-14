package bo.saludencasa.features.profile.data.mapper

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.data.model.PatientDto
import bo.saludencasa.features.profile.data.model.PatientUpdateDto
import bo.saludencasa.features.profile.data.model.ProfileDto
import bo.saludencasa.features.profile.data.model.ProfileUpdateDto
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.model.UserProfile
import bo.saludencasa.features.profile.domain.vo.BirthDate
import java.time.LocalDate

internal fun ProfileDto.toUserProfile(patient: PatientDto?): UserProfile =
    UserProfile(
        userId = id,
        fullName = fullName,
        email = email?.let { Email.create(it).getOrNull() },
        phone = phone?.let { PhoneNumber.create(it).getOrNull() },
        photoUrl = photoUrl?.takeIf(String::isNotBlank),
        role = role?.toUserRole(),
        patient = patient?.toPatientDetails(),
    )

internal fun PatientDto.toPatientDetails(): PatientDetails =
    PatientDetails(
        birthDate = birthDate?.toBirthDate(),
        emergencyContact = emergencyContact?.takeIf(String::isNotBlank),
        medicalNotes = medicalNotes?.takeIf(String::isNotBlank),
    )

internal fun ProfileUpdate.toProfileUpdateDto(): ProfileUpdateDto =
    ProfileUpdateDto(
        fullName = fullName.value,
        phone = phone?.value,
    )

internal fun PatientDetails.toPatientUpdateDto(): PatientUpdateDto =
    PatientUpdateDto(
        birthDate = birthDate?.value?.toString(),
        emergencyContact = emergencyContact,
        medicalNotes = medicalNotes,
    )

private fun String.toBirthDate(): BirthDate? =
    runCatching { LocalDate.parse(this) }
        .getOrNull()
        ?.let { BirthDate.create(it).getOrNull() }

package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfessionalUpdate
import bo.saludencasa.features.profile.domain.model.ProfileDraft
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.repository.IProfileRepository
import bo.saludencasa.features.profile.domain.vo.BirthDate
import bo.saludencasa.features.profile.domain.vo.CoverageRadiusKm
import bo.saludencasa.features.profile.domain.vo.YearsOfExperience

class SaveProfileUseCase(
    private val profileRepository: IProfileRepository,
) {
    suspend operator fun invoke(draft: ProfileDraft): ProfileResult {
        val fullName =
            PersonName.create(draft.fullName).getOrNull()
                ?: return ProfileResult.Failure(ProfileError.InvalidName)

        val phone =
            draft.phone.takeIf(String::isNotBlank)?.let { raw ->
                PhoneNumber.create(raw).getOrNull()
                    ?: return ProfileResult.Failure(ProfileError.InvalidPhone)
            }

        val patient =
            draft.patient?.let { details ->
                val birthDate =
                    details.birthDate?.let { date ->
                        BirthDate.create(date).getOrNull()
                            ?: return ProfileResult.Failure(ProfileError.InvalidBirthDate)
                    }
                PatientDetails(
                    birthDate = birthDate,
                    emergencyContact = details.emergencyContact.trim().takeIf(String::isNotBlank),
                    medicalNotes = details.medicalNotes.trim().takeIf(String::isNotBlank),
                )
            }

        val professional =
            draft.professional?.let { details ->
                val years =
                    YearsOfExperience.parse(details.yearsOfExperience).getOrNull()
                        ?: return ProfileResult.Failure(ProfileError.InvalidYearsOfExperience)

                val radius =
                    CoverageRadiusKm.parse(details.coverageRadiusKm).getOrNull()
                        ?: return ProfileResult.Failure(ProfileError.InvalidCoverageRadius)

                // The rate is the only optional number. base_rate_bob admits
                // null, so an empty field is a professional who has not set a
                // price yet; the years and the radius sit in columns that
                // cannot be null and always carry a value.
                val rate =
                    details.baseRateBob.takeIf(String::isNotBlank)?.let { raw ->
                        AmountBob.parse(raw).getOrNull()
                            ?: return ProfileResult.Failure(ProfileError.InvalidBaseRate)
                    }

                ProfessionalUpdate(
                    professionalType = details.professionalType,
                    specialty = details.specialty.trim().takeIf(String::isNotBlank),
                    biography = details.biography.trim().takeIf(String::isNotBlank),
                    yearsOfExperience = years,
                    baseRateBob = rate,
                    coverageRadiusKm = radius,
                )
            }

        return profileRepository.saveProfile(
            ProfileUpdate(
                fullName = fullName,
                phone = phone,
                patient = patient,
                professional = professional,
            ),
        )
    }
}

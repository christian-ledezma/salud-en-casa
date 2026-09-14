package bo.saludencasa.features.profile.domain.usecase

import bo.saludencasa.core.vo.PersonName
import bo.saludencasa.core.vo.PhoneNumber
import bo.saludencasa.features.profile.domain.model.PatientDetails
import bo.saludencasa.features.profile.domain.model.ProfileDraft
import bo.saludencasa.features.profile.domain.model.ProfileError
import bo.saludencasa.features.profile.domain.model.ProfileResult
import bo.saludencasa.features.profile.domain.model.ProfileUpdate
import bo.saludencasa.features.profile.domain.repository.IProfileRepository
import bo.saludencasa.features.profile.domain.vo.BirthDate

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

        return profileRepository.saveProfile(
            ProfileUpdate(fullName = fullName, phone = phone, patient = patient),
        )
    }
}

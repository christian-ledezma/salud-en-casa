package bo.saludencasa.features.profile.domain.model

sealed interface ProfileError {
    data object NotSignedIn : ProfileError

    data object ProfileNotFound : ProfileError

    data object RoleAlreadyHeld : ProfileError

    data object RoleNotHeld : ProfileError

    data object InvalidName : ProfileError

    data object InvalidPhone : ProfileError

    data object InvalidBirthDate : ProfileError

    data object InvalidBaseRate : ProfileError

    data object InvalidCoverageRadius : ProfileError

    data object InvalidYearsOfExperience : ProfileError

    data object NetworkUnavailable : ProfileError

    data object Unexpected : ProfileError
}

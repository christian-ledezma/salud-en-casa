package bo.saludencasa.features.profile.domain.model

sealed interface ProfileError {
    data object NotSignedIn : ProfileError

    data object ProfileNotFound : ProfileError

    data object RoleAlreadyAssigned : ProfileError

    data object InvalidName : ProfileError

    data object InvalidPhone : ProfileError

    data object InvalidBirthDate : ProfileError

    data object NetworkUnavailable : ProfileError

    data object Unexpected : ProfileError
}

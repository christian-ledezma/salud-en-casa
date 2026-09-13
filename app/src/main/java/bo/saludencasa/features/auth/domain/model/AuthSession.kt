package bo.saludencasa.features.auth.domain.model

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName

data class AuthSession(
    val userId: String,
    val email: Email?,
    val fullName: PersonName?,
)

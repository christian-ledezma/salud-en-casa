package bo.saludencasa.features.auth.domain.model

import bo.saludencasa.core.vo.Email
import bo.saludencasa.core.vo.PersonName

// The identity provider is the only source of these three values, and it can
// deliver a name or an address this project's value objects reject, so both
// arrive nullable rather than blocking a sign in that Supabase already accepted.
data class AuthSession(
    val userId: String,
    val email: Email?,
    val fullName: PersonName?,
)

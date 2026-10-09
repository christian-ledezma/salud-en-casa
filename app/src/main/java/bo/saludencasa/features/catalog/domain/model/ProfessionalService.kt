package bo.saludencasa.features.catalog.domain.model

import java.math.BigDecimal

data class ProfessionalService(
    val id: String,
    val type: ServiceType,
    val referencePriceBob: BigDecimal,
)

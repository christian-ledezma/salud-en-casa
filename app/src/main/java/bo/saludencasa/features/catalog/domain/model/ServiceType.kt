package bo.saludencasa.features.catalog.domain.model

import java.math.BigDecimal

data class ServiceType(
    val id: String,
    val name: String,
    val description: String?,
    val referencePriceBob: BigDecimal,
    val estimatedDurationMin: Int,
)

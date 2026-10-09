package bo.saludencasa.features.catalog.data.model

import bo.saludencasa.core.network.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ServiceTypeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("reference_price_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val referencePriceBob: BigDecimal,
    @SerialName("estimated_duration_min") val estimatedDurationMin: Int,
)

@Serializable
data class ProfessionalServiceDto(
    val id: String,
    @SerialName("service_type_id") val serviceTypeId: String,
    @SerialName("reference_price_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val referencePriceBob: BigDecimal,
)

// What travels towards public.professional_services. The active column is left
// out so the table default applies (docs/decisions.md, 2026-10-09, removing a
// declared service is a delete).
@Serializable
data class ProfessionalServiceRow(
    @SerialName("professional_id") val professionalId: String,
    @SerialName("service_type_id") val serviceTypeId: String,
    @SerialName("reference_price_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val referencePriceBob: BigDecimal,
)

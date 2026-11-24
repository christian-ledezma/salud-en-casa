package bo.saludencasa.features.disputes.data.model

import bo.saludencasa.core.network.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

// A row of payment_dispute_queue.
@Serializable
data class PaymentDisputeDto(
    @SerialName("payment_id") val paymentId: String,
    @SerialName("request_id") val requestId: String,
    @SerialName("service_type_name") val serviceTypeName: String,
    @SerialName("total_amount_bob")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmountBob: BigDecimal,
    @SerialName("method") val method: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("patient_confirmed_at") val patientConfirmedAt: String,
    @SerialName("disputed_at") val disputedAt: String,
    @SerialName("patient_name") val patientName: String,
    @SerialName("patient_email") val patientEmail: String,
    @SerialName("professional_name") val professionalName: String,
    @SerialName("professional_email") val professionalEmail: String,
)

@Serializable
data class PaymentStatusRow(
    val status: String,
)

package bo.saludencasa.features.services

import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.AnswerPaymentResult
import bo.saludencasa.features.services.domain.model.Attention
import bo.saludencasa.features.services.domain.model.AttentionCancellation
import bo.saludencasa.features.services.domain.model.AttentionPayment
import bo.saludencasa.features.services.domain.model.AttentionPlace
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.AttentionSummary
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.CancelAttentionResult
import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentResult
import bo.saludencasa.features.services.domain.model.Counterpart
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import bo.saludencasa.features.services.domain.model.MyReview
import bo.saludencasa.features.services.domain.model.PaymentAnswer
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.model.PaymentStatus
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.model.ServiceStatus
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import bo.saludencasa.features.services.domain.vo.CancellationReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigDecimal
import java.time.Instant

class FakeServiceRepository : IServiceRepository {
    var result: AttentionResult = AttentionResult.NotFound
    var attentions: List<AttentionSummary> = emptyList()
    var attentionsError: ServiceError? = null
    var activeAttention: ActiveAttentionResult = ActiveAttentionResult.None
    val sidesRead = mutableListOf<AttentionSide>()
    var advanceError: ServiceError? = null
    var holdAdvance: CompletableDeferred<Unit>? = null
    val advances = mutableListOf<AttentionStep>()
    var cancelError: ServiceError? = null
    var holdCancel: CompletableDeferred<Unit>? = null
    val cancels = mutableListOf<String>()
    var paymentError: ServiceError? = null
    var holdPayment: CompletableDeferred<Unit>? = null
    val payments = mutableListOf<PaymentMethod>()
    var answerError: ServiceError? = null
    var holdAnswer: CompletableDeferred<Unit>? = null
    val answers = mutableListOf<PaymentAnswer>()
    val receipts = mutableListOf<PaymentMethod>()
    var acceptError: ServiceError? = null
    var holdAccept: CompletableDeferred<Unit>? = null
    var accepts = 0
    val signals = MutableSharedFlow<Unit>()

    override suspend fun getAttention(requestId: String): AttentionResult = result

    override suspend fun getMyAttentions(
        side: AttentionSide,
        offset: Int,
        limit: Int,
    ): MyAttentionsResult {
        sidesRead += side
        return attentionsError?.let(MyAttentionsResult::Failure)
            ?: MyAttentionsResult.Loaded(attentions.drop(offset).take(limit))
    }

    override suspend fun getActiveAttention(side: AttentionSide): ActiveAttentionResult {
        sidesRead += side
        return activeAttention
    }

    // The engine moves the state and stamps its date, so the reread finds them.
    override suspend fun advance(
        requestId: String,
        step: AttentionStep,
    ): AdvanceAttentionResult {
        advances += step
        holdAdvance?.await()
        advanceError?.let { return AdvanceAttentionResult.Failure(it) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            val at = Instant.parse("2026-11-04T14:00:00Z")
            result =
                AttentionResult.Loaded(
                    when (step) {
                        AttentionStep.MarkArrival -> current.copy(status = step.targetStatus, startedAt = at)
                        AttentionStep.MarkCompleted -> current.copy(status = step.targetStatus, finishedAt = at)
                    },
                )
        }
        return AdvanceAttentionResult.Advanced
    }

    override suspend fun cancel(
        requestId: String,
        reason: CancellationReason,
    ): CancelAttentionResult {
        cancels += reason.value
        holdCancel?.await()
        cancelError?.let { return CancelAttentionResult.Failure(CancelAttentionError.Service(it)) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            result =
                AttentionResult.Loaded(
                    current.copy(
                        status = ServiceStatus.CANCELLED,
                        conversationOpen = false,
                        cancellation =
                            AttentionCancellation(
                                reason.value,
                                byMe = true,
                                at = Instant.parse("2026-11-04T14:00:00Z"),
                            ),
                    ),
                )
        }
        return CancelAttentionResult.Cancelled
    }

    // The engine seals the time, so the reread finds it.
    override suspend fun confirmPayment(
        serviceId: String,
        method: PaymentMethod,
    ): ConfirmPaymentResult {
        payments += method
        holdPayment?.await()
        paymentError?.let { return ConfirmPaymentResult.Failure(ConfirmPaymentError.Service(it)) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            result =
                AttentionResult.Loaded(
                    current.copy(
                        payment =
                            AttentionPayment(
                                status = PaymentStatus.PATIENT_CONFIRMED,
                                method = method,
                                patientConfirmedAt = Instant.parse("2026-11-04T16:00:00Z"),
                                professionalConfirmedAt = null,
                            ),
                    ),
                )
        }
        return ConfirmPaymentResult.Confirmed
    }

    // The engine seals the time of the answer, so the reread finds it.
    override suspend fun answerPayment(
        serviceId: String,
        answer: PaymentAnswer,
    ): AnswerPaymentResult {
        answers += answer
        holdAnswer?.await()
        answerError?.let { return AnswerPaymentResult.Failure(it) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            val at = Instant.parse("2026-11-04T16:30:00Z")
            result =
                AttentionResult.Loaded(
                    current.copy(
                        payment =
                            when (answer) {
                                PaymentAnswer.RECEIVED -> {
                                    current.payment.copy(
                                        status = PaymentStatus.BOTH_CONFIRMED,
                                        professionalConfirmedAt = at,
                                    )
                                }

                                PaymentAnswer.NOT_RECEIVED -> {
                                    current.payment.copy(status = PaymentStatus.DISPUTED, disputedAt = at)
                                }
                            },
                    ),
                )
        }
        return AnswerPaymentResult.Answered
    }

    // The engine seals the professional's time and keeps the method they named.
    override suspend fun confirmReceipt(
        serviceId: String,
        method: PaymentMethod,
    ): ConfirmPaymentResult {
        receipts += method
        holdPayment?.await()
        paymentError?.let { return ConfirmPaymentResult.Failure(ConfirmPaymentError.Service(it)) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            result = AttentionResult.Loaded(current.copy(payment = receivedFirst(method)))
        }
        return ConfirmPaymentResult.Confirmed
    }

    // Accepting seals the patient's time; the professional's method stays.
    override suspend fun acceptReceipt(serviceId: String): AnswerPaymentResult {
        accepts++
        holdAccept?.await()
        acceptError?.let { return AnswerPaymentResult.Failure(it) }
        val current = (result as? AttentionResult.Loaded)?.attention
        if (current != null) {
            result =
                AttentionResult.Loaded(
                    current.copy(
                        payment =
                            current.payment.copy(
                                status = PaymentStatus.BOTH_CONFIRMED,
                                patientConfirmedAt = Instant.parse("2026-11-04T16:45:00Z"),
                            ),
                    ),
                )
        }
        return AnswerPaymentResult.Answered
    }

    override fun attentionSignals(requestId: String): Flow<Unit> = signals
}

fun receivedFirst(
    method: PaymentMethod = PaymentMethod.CASH,
    at: Instant = Instant.parse("2026-11-04T16:00:00Z"),
): AttentionPayment =
    AttentionPayment(
        status = PaymentStatus.PROFESSIONAL_CONFIRMED,
        method = method,
        patientConfirmedAt = null,
        professionalConfirmedAt = at,
    )

fun resolved(paid: Boolean): AttentionPayment =
    paidInCash().copy(
        status = if (paid) PaymentStatus.RESOLVED_PAID else PaymentStatus.RESOLVED_NOT_PAID,
        disputedAt = Instant.parse("2026-11-04T16:30:00Z"),
        resolvedAt = Instant.parse("2026-11-05T15:20:00Z"),
    )

fun attentionSummary(
    requestId: String = "r1",
    status: ServiceStatus = ServiceStatus.ASSIGNED,
    scheduledAt: Instant? = null,
    startedAt: Instant? = null,
): AttentionSummary =
    AttentionSummary(
        requestId = requestId,
        status = status,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        counterpartName = "María Quispe",
        createdAt = Instant.parse("2026-10-29T12:00:00Z"),
        scheduledAt = scheduledAt,
        startedAt = startedAt,
    )

fun attention(
    requestId: String = "r1",
    status: ServiceStatus = ServiceStatus.ASSIGNED,
    iAmThePatient: Boolean = true,
    counterpartId: String = "p1",
    place: AttentionPlace = attentionPlace(),
    conversationOpen: Boolean = status != ServiceStatus.CANCELLED,
    conversationWritableUntil: Instant? = null,
    cancellation: AttentionCancellation? = null,
    payment: AttentionPayment = AttentionPayment.Pending,
    myReview: MyReview? = null,
): Attention =
    Attention(
        serviceId = "s1",
        requestId = requestId,
        status = status,
        finalAmountBob = BigDecimal("150.00"),
        serviceTypeName = "Curación de heridas",
        counterpart =
            Counterpart(
                id = counterpartId,
                name = "María Quispe",
                photoUrl = null,
            ),
        iAmThePatient = iAmThePatient,
        createdAt = Instant.parse("2026-10-29T12:00:00Z"),
        scheduledAt = null,
        startedAt = null,
        finishedAt = null,
        description = "Curación de una herida en la pierna derecha.",
        place = place,
        conversationOpen = conversationOpen,
        conversationWritableUntil = conversationWritableUntil,
        cancellation = cancellation,
        payment = payment,
        myReview = myReview,
    )

fun paidInCash(at: Instant = Instant.parse("2026-11-04T16:00:00Z")): AttentionPayment =
    AttentionPayment(
        status = PaymentStatus.PATIENT_CONFIRMED,
        method = PaymentMethod.CASH,
        patientConfirmedAt = at,
        professionalConfirmedAt = null,
    )

fun attentionPlace(
    addressText: String? = "Av. Arce 2631",
    latitude: Double? = -16.5103,
    longitude: Double? = -68.1218,
): AttentionPlace =
    AttentionPlace(
        city = "La Paz",
        addressText = addressText,
        reference = addressText?.let { "Tercer piso" },
        latitude = latitude,
        longitude = longitude,
    )

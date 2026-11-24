package bo.saludencasa.features.services.data.repository

import bo.saludencasa.features.services.data.datasource.SupabaseServiceDataSource
import bo.saludencasa.features.services.data.mapper.toAttention
import bo.saludencasa.features.services.data.mapper.toAttentionSummary
import bo.saludencasa.features.services.data.mapper.toServiceError
import bo.saludencasa.features.services.domain.model.ActiveAttentionResult
import bo.saludencasa.features.services.domain.model.AdvanceAttentionResult
import bo.saludencasa.features.services.domain.model.AnswerPaymentResult
import bo.saludencasa.features.services.domain.model.AttentionResult
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.model.AttentionStep
import bo.saludencasa.features.services.domain.model.CancelAttentionError
import bo.saludencasa.features.services.domain.model.CancelAttentionResult
import bo.saludencasa.features.services.domain.model.ConfirmPaymentError
import bo.saludencasa.features.services.domain.model.ConfirmPaymentResult
import bo.saludencasa.features.services.domain.model.MyAttentionsResult
import bo.saludencasa.features.services.domain.model.PaymentAnswer
import bo.saludencasa.features.services.domain.model.PaymentMethod
import bo.saludencasa.features.services.domain.model.PaymentStatus
import bo.saludencasa.features.services.domain.model.ServiceError
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import bo.saludencasa.features.services.domain.vo.CancellationReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class ServiceRepository(
    private val dataSource: SupabaseServiceDataSource,
) : IServiceRepository {
    override suspend fun getAttention(requestId: String): AttentionResult {
        if (dataSource.currentUserId() == null) {
            return AttentionResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            val dto = dataSource.fetchAttention(requestId) ?: return AttentionResult.NotFound
            dto.toAttention()?.let(AttentionResult::Loaded)
                ?: AttentionResult.Failure(ServiceError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AttentionResult.Failure(failure.toServiceError())
        }
    }

    override suspend fun getMyAttentions(
        side: AttentionSide,
        offset: Int,
        limit: Int,
    ): MyAttentionsResult {
        if (dataSource.currentUserId() == null) {
            return MyAttentionsResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            val attentions =
                dataSource.fetchMyAttentions(side.name, offset, limit).map { dto ->
                    dto.toAttentionSummary() ?: return MyAttentionsResult.Failure(ServiceError.Unexpected)
                }
            MyAttentionsResult.Loaded(attentions)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            MyAttentionsResult.Failure(failure.toServiceError())
        }
    }

    override suspend fun getActiveAttention(side: AttentionSide): ActiveAttentionResult {
        if (dataSource.currentUserId() == null) {
            return ActiveAttentionResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            val dto = dataSource.fetchActiveAttention(side.name) ?: return ActiveAttentionResult.None
            dto.toAttentionSummary()?.let(ActiveAttentionResult::Found)
                ?: ActiveAttentionResult.Failure(ServiceError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            ActiveAttentionResult.Failure(failure.toServiceError())
        }
    }

    override suspend fun advance(
        requestId: String,
        step: AttentionStep,
    ): AdvanceAttentionResult {
        if (dataSource.currentUserId() == null) {
            return AdvanceAttentionResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            if (dataSource.updateStatus(requestId, step.targetStatus.name) == 0) {
                AdvanceAttentionResult.Failure(ServiceError.Unexpected)
            } else {
                AdvanceAttentionResult.Advanced
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AdvanceAttentionResult.Failure(failure.toServiceError())
        }
    }

    override suspend fun cancel(
        requestId: String,
        reason: CancellationReason,
    ): CancelAttentionResult {
        if (dataSource.currentUserId() == null) {
            return cancelFailure(ServiceError.NotSignedIn)
        }

        return try {
            if (dataSource.cancel(requestId, reason.value) == 0) {
                cancelFailure(ServiceError.Unexpected)
            } else {
                CancelAttentionResult.Cancelled
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            cancelFailure(failure.toServiceError())
        }
    }

    private fun cancelFailure(error: ServiceError) = CancelAttentionResult.Failure(CancelAttentionError.Service(error))

    override suspend fun confirmPayment(
        serviceId: String,
        method: PaymentMethod,
    ): ConfirmPaymentResult = declare(serviceId, PaymentStatus.PATIENT_CONFIRMED, method)

    override suspend fun confirmReceipt(
        serviceId: String,
        method: PaymentMethod,
    ): ConfirmPaymentResult = declare(serviceId, PaymentStatus.PROFESSIONAL_CONFIRMED, method)

    private suspend fun declare(
        serviceId: String,
        status: PaymentStatus,
        method: PaymentMethod,
    ): ConfirmPaymentResult {
        if (dataSource.currentUserId() == null) {
            return paymentFailure(ServiceError.NotSignedIn)
        }

        return try {
            if (dataSource.declarePayment(serviceId, status.name, method.name) == 0) {
                paymentFailure(ServiceError.Unexpected)
            } else {
                ConfirmPaymentResult.Confirmed
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            paymentFailure(failure.toServiceError())
        }
    }

    private fun paymentFailure(error: ServiceError) = ConfirmPaymentResult.Failure(ConfirmPaymentError.Service(error))

    override suspend fun answerPayment(
        serviceId: String,
        answer: PaymentAnswer,
    ): AnswerPaymentResult =
        answer(
            serviceId,
            when (answer) {
                PaymentAnswer.RECEIVED -> PaymentStatus.BOTH_CONFIRMED
                PaymentAnswer.NOT_RECEIVED -> PaymentStatus.DISPUTED
            },
        )

    // The patient accepting the professional's receipt is the patient's own
    // confirmation (INV-09); the engine seals it and keeps the method.
    override suspend fun acceptReceipt(serviceId: String): AnswerPaymentResult =
        answer(serviceId, PaymentStatus.BOTH_CONFIRMED)

    private suspend fun answer(
        serviceId: String,
        status: PaymentStatus,
    ): AnswerPaymentResult {
        if (dataSource.currentUserId() == null) {
            return AnswerPaymentResult.Failure(ServiceError.NotSignedIn)
        }

        return try {
            if (dataSource.answerPayment(serviceId, status.name) == 0) {
                AnswerPaymentResult.Failure(ServiceError.Unexpected)
            } else {
                AnswerPaymentResult.Answered
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AnswerPaymentResult.Failure(failure.toServiceError())
        }
    }

    override fun attentionSignals(requestId: String): Flow<Unit> {
        if (dataSource.currentUserId() == null) return emptyFlow()
        return dataSource.signals(requestId)
    }
}

package bo.saludencasa.features.requests.data.repository

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.data.datasource.SupabaseOfferDataSource
import bo.saludencasa.features.requests.data.mapper.toOfferError
import bo.saludencasa.features.requests.data.mapper.toOfferThreads
import bo.saludencasa.features.requests.data.mapper.toOfferableRequest
import bo.saludencasa.features.requests.data.model.NewOfferRow
import bo.saludencasa.features.requests.domain.model.OfferActionResult
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferIssuer
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferThreadResult
import bo.saludencasa.features.requests.domain.model.OfferThreadsResult
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.repository.IOfferRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class OfferRepository(
    private val dataSource: SupabaseOfferDataSource,
) : IOfferRepository {
    override suspend fun submitOffer(
        requestId: String,
        amount: AmountBob,
        message: String?,
    ): OfferSubmitResult {
        val userId = dataSource.currentUserId() ?: return OfferSubmitResult.Failure(OfferError.NotSignedIn)

        return try {
            dataSource.submitOffer(
                NewOfferRow(
                    requestId = requestId,
                    professionalId = userId,
                    issuer = OfferIssuer.PROFESSIONAL.name,
                    amountBob = amount.value,
                    message = message,
                ),
            )
            OfferSubmitResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            OfferSubmitResult.Failure(failure.toOfferError())
        }
    }

    // The issuer is not passed in: it follows from which side of this thread the
    // caller is on, and INV-16 keeps that unambiguous.
    override suspend fun submitCounterOffer(
        requestId: String,
        professionalId: String,
        parentOfferId: String,
        amount: AmountBob,
        message: String?,
    ): OfferSubmitResult {
        val userId = dataSource.currentUserId() ?: return OfferSubmitResult.Failure(OfferError.NotSignedIn)
        val issuer = if (userId == professionalId) OfferIssuer.PROFESSIONAL else OfferIssuer.PATIENT

        return try {
            dataSource.submitOffer(
                NewOfferRow(
                    requestId = requestId,
                    professionalId = professionalId,
                    parentOfferId = parentOfferId,
                    issuer = issuer.name,
                    amountBob = amount.value,
                    message = message,
                ),
            )
            OfferSubmitResult.Success
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            OfferSubmitResult.Failure(failure.toOfferError())
        }
    }

    override suspend fun getOfferThreads(requestId: String): OfferThreadsResult {
        val userId = dataSource.currentUserId() ?: return OfferThreadsResult.Failure(OfferError.NotSignedIn)

        return try {
            dataSource
                .fetchOffersForRequest(requestId)
                .toOfferThreads(userId)
                ?.let(OfferThreadsResult::Loaded)
                ?: OfferThreadsResult.Failure(OfferError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            OfferThreadsResult.Failure(failure.toOfferError())
        }
    }

    // Each side has its own way in: the professional reads their own chain off
    // the table, and the patient reads the definer pinned to their request.
    override suspend fun getOfferThread(
        requestId: String,
        professionalId: String?,
    ): OfferThreadResult {
        val userId = dataSource.currentUserId() ?: return OfferThreadResult.Failure(OfferError.NotSignedIn)
        val target = professionalId ?: userId

        return try {
            val rows =
                if (userId == target) {
                    dataSource.fetchMyThread(requestId, target)
                } else {
                    dataSource.fetchOffersForRequest(requestId).filter { it.professionalId == target }
                }
            if (rows.isEmpty()) return OfferThreadResult.NotFound

            rows
                .toOfferThreads(userId)
                ?.firstOrNull()
                ?.let(OfferThreadResult::Loaded)
                ?: OfferThreadResult.Failure(OfferError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            OfferThreadResult.Failure(failure.toOfferError())
        }
    }

    override suspend fun getOfferableRequest(requestId: String): OfferableRequestResult {
        if (dataSource.currentUserId() == null) {
            return OfferableRequestResult.Failure(OfferError.NotSignedIn)
        }

        return try {
            val dto = dataSource.fetchOfferableRequest(requestId) ?: return OfferableRequestResult.NotFound
            dto.toOfferableRequest()?.let(OfferableRequestResult::Loaded)
                ?: OfferableRequestResult.Failure(OfferError.Unexpected)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            OfferableRequestResult.Failure(failure.toOfferError())
        }
    }

    override suspend fun rejectOffer(offerId: String): OfferActionResult {
        if (dataSource.currentUserId() == null) {
            return OfferActionResult.Failure(OfferError.NotSignedIn)
        }

        return try {
            if (dataSource.rejectOffer(offerId) == 0) {
                OfferActionResult.Failure(OfferError.NotAllowed)
            } else {
                OfferActionResult.Success
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            OfferActionResult.Failure(failure.toOfferError())
        }
    }

    override fun observeOfferSignals(asProfessional: Boolean): Flow<Unit> {
        val userId = dataSource.currentUserId() ?: return emptyFlow()
        return dataSource.offerSignals(userId, asProfessional)
    }
}

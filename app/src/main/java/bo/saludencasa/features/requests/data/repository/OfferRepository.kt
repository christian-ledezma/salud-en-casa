package bo.saludencasa.features.requests.data.repository

import bo.saludencasa.core.vo.AmountBob
import bo.saludencasa.features.requests.data.datasource.SupabaseOfferDataSource
import bo.saludencasa.features.requests.data.mapper.toOfferError
import bo.saludencasa.features.requests.data.mapper.toOfferableRequest
import bo.saludencasa.features.requests.data.mapper.toRequestOffer
import bo.saludencasa.features.requests.data.model.NewOfferRow
import bo.saludencasa.features.requests.domain.model.OfferError
import bo.saludencasa.features.requests.domain.model.OfferSubmitResult
import bo.saludencasa.features.requests.domain.model.OfferableRequestResult
import bo.saludencasa.features.requests.domain.model.RequestOffersResult
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
        message: String,
    ): OfferSubmitResult {
        val userId = dataSource.currentUserId() ?: return OfferSubmitResult.Failure(OfferError.NotSignedIn)

        return try {
            dataSource.submitOffer(
                NewOfferRow(
                    requestId = requestId,
                    professionalId = userId,
                    issuer = "PROFESSIONAL",
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

    override suspend fun getOffersForRequest(requestId: String): RequestOffersResult {
        if (dataSource.currentUserId() == null) {
            return RequestOffersResult.Failure(OfferError.NotSignedIn)
        }

        return try {
            val offers = dataSource.fetchOffersForRequest(requestId).map { it.toRequestOffer() }
            if (offers.any { it == null }) {
                RequestOffersResult.Failure(OfferError.Unexpected)
            } else {
                RequestOffersResult.Loaded(offers.filterNotNull())
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            RequestOffersResult.Failure(failure.toOfferError())
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

    override fun observeMyOfferSignals(): Flow<Unit> {
        val userId = dataSource.currentUserId() ?: return emptyFlow()
        return dataSource.offerSignals(userId)
    }
}

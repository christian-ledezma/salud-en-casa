package bo.saludencasa.features.subscription.data.repository

import bo.saludencasa.features.subscription.data.datasource.SupabaseSubscriptionDataSource
import bo.saludencasa.features.subscription.data.mapper.toSubscriptionError
import bo.saludencasa.features.subscription.data.mapper.toSubscriptionState
import bo.saludencasa.features.subscription.domain.model.SubscriptionError
import bo.saludencasa.features.subscription.domain.model.SubscriptionStateResult
import bo.saludencasa.features.subscription.domain.repository.ISubscriptionRepository
import kotlinx.coroutines.CancellationException

class SubscriptionRepository(
    private val dataSource: SupabaseSubscriptionDataSource,
) : ISubscriptionRepository {
    override suspend fun getMine(): SubscriptionStateResult {
        if (dataSource.currentUserId() == null) {
            return SubscriptionStateResult.Failure(SubscriptionError.NotSignedIn)
        }

        return try {
            val state = dataSource.fetchMine()?.toSubscriptionState()
            if (state == null) {
                SubscriptionStateResult.Failure(SubscriptionError.UnreadableResult)
            } else {
                SubscriptionStateResult.Loaded(state)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            SubscriptionStateResult.Failure(failure.toSubscriptionError())
        }
    }
}

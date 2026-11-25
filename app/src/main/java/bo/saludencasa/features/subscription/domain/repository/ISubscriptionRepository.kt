package bo.saludencasa.features.subscription.domain.repository

import bo.saludencasa.features.subscription.domain.model.SubscriptionStateResult

interface ISubscriptionRepository {
    suspend fun getMine(): SubscriptionStateResult
}

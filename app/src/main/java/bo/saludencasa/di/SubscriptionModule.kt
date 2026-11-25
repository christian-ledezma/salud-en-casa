package bo.saludencasa.di

import bo.saludencasa.features.subscription.data.datasource.SupabaseSubscriptionDataSource
import bo.saludencasa.features.subscription.data.repository.SubscriptionRepository
import bo.saludencasa.features.subscription.domain.repository.ISubscriptionRepository
import bo.saludencasa.features.subscription.domain.usecase.GetMySubscriptionUseCase
import bo.saludencasa.features.subscription.presentation.SubscriptionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val subscriptionModule =
    module {
        single { SupabaseSubscriptionDataSource(supabase = get()) }
        single<ISubscriptionRepository> { SubscriptionRepository(get()) }

        factory { GetMySubscriptionUseCase(get()) }

        viewModel { SubscriptionViewModel(get()) }
    }

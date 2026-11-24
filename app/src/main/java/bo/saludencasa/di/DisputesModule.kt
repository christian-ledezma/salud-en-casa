package bo.saludencasa.di

import bo.saludencasa.features.disputes.data.datasource.SupabaseDisputeDataSource
import bo.saludencasa.features.disputes.data.repository.DisputeRepository
import bo.saludencasa.features.disputes.domain.repository.IDisputeRepository
import bo.saludencasa.features.disputes.domain.usecase.GetDisputeUseCase
import bo.saludencasa.features.disputes.domain.usecase.GetDisputesUseCase
import bo.saludencasa.features.disputes.domain.usecase.ResolveDisputeUseCase
import bo.saludencasa.features.disputes.presentation.DisputeQueueViewModel
import bo.saludencasa.features.disputes.presentation.DisputeReviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val disputesModule =
    module {
        single { SupabaseDisputeDataSource(supabase = get()) }
        single<IDisputeRepository> { DisputeRepository(get()) }

        factory { GetDisputesUseCase(get()) }
        factory { GetDisputeUseCase(get()) }
        factory { ResolveDisputeUseCase(get()) }

        viewModel { DisputeQueueViewModel(get()) }
        viewModel { (paymentId: String) -> DisputeReviewViewModel(paymentId, get(), get()) }
    }

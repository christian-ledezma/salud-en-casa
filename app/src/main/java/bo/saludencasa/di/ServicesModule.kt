package bo.saludencasa.di

import bo.saludencasa.features.services.data.datasource.SupabaseServiceDataSource
import bo.saludencasa.features.services.data.repository.ServiceRepository
import bo.saludencasa.features.services.domain.model.AttentionSide
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import bo.saludencasa.features.services.domain.usecase.AdvanceAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.CancelAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.GetActiveAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import bo.saludencasa.features.services.domain.usecase.GetMyAttentionsUseCase
import bo.saludencasa.features.services.domain.usecase.ObserveAttentionUseCase
import bo.saludencasa.features.services.presentation.AttentionViewModel
import bo.saludencasa.features.services.presentation.MyAttentionsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val servicesModule =
    module {
        single { SupabaseServiceDataSource(supabase = get()) }
        single<IServiceRepository> { ServiceRepository(get()) }

        factory { GetAttentionUseCase(get()) }
        factory { GetMyAttentionsUseCase(get()) }
        factory { GetActiveAttentionUseCase(get()) }
        factory { AdvanceAttentionUseCase(get()) }
        factory { ObserveAttentionUseCase(get()) }
        factory { CancelAttentionUseCase(get()) }

        viewModel { (requestId: String) ->
            AttentionViewModel(
                requestId = requestId,
                getAttention = get(),
                advanceAttention = get(),
                observeAttention = get(),
                cancelAttention = get(),
            )
        }
        viewModel { (side: AttentionSide) -> MyAttentionsViewModel(side, get()) }
    }

package bo.saludencasa.di

import bo.saludencasa.features.services.data.datasource.SupabaseServiceDataSource
import bo.saludencasa.features.services.data.repository.ServiceRepository
import bo.saludencasa.features.services.domain.repository.IServiceRepository
import bo.saludencasa.features.services.domain.usecase.GetAttentionUseCase
import bo.saludencasa.features.services.presentation.AttentionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val servicesModule =
    module {
        single { SupabaseServiceDataSource(supabase = get()) }
        single<IServiceRepository> { ServiceRepository(get()) }

        factory { GetAttentionUseCase(get()) }

        viewModel { (requestId: String) ->
            AttentionViewModel(requestId, get())
        }
    }

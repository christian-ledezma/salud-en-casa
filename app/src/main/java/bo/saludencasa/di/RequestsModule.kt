package bo.saludencasa.di

import bo.saludencasa.features.requests.data.datasource.SupabaseRequestDataSource
import bo.saludencasa.features.requests.data.repository.RequestRepository
import bo.saludencasa.features.requests.domain.repository.IRequestRepository
import bo.saludencasa.features.requests.domain.usecase.CancelRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.CreateImmediateRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetMyRequestsUseCase
import bo.saludencasa.features.requests.presentation.CreateRequestViewModel
import bo.saludencasa.features.requests.presentation.MyRequestsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val requestsModule =
    module {
        single { SupabaseRequestDataSource(supabase = get()) }

        single<IRequestRepository> { RequestRepository(get()) }

        factory { CreateImmediateRequestUseCase(get()) }
        factory { GetMyRequestsUseCase(get()) }
        factory { CancelRequestUseCase(get()) }

        // The create screen reads the catalog and the patient's addresses
        // through the use cases of the features that own them (catalogModule,
        // locationModule), which Koin resolves across modules.
        viewModel { CreateRequestViewModel(get(), get(), get()) }
        viewModel { MyRequestsViewModel(get(), get()) }
    }

package bo.saludencasa.di

import bo.saludencasa.features.requests.data.datasource.SupabaseOfferDataSource
import bo.saludencasa.features.requests.data.datasource.SupabaseRequestDataSource
import bo.saludencasa.features.requests.data.repository.OfferRepository
import bo.saludencasa.features.requests.data.repository.RequestRepository
import bo.saludencasa.features.requests.domain.repository.IOfferRepository
import bo.saludencasa.features.requests.domain.repository.IRequestRepository
import bo.saludencasa.features.requests.domain.usecase.CancelRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.CreateImmediateRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.CreateScheduledRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.FindProfessionalsAvailableNowUseCase
import bo.saludencasa.features.requests.domain.usecase.GetActiveImmediateRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.GetBookableTimesUseCase
import bo.saludencasa.features.requests.domain.usecase.GetInboxUseCase
import bo.saludencasa.features.requests.domain.usecase.GetMyRequestsUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferThreadUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferThreadsUseCase
import bo.saludencasa.features.requests.domain.usecase.GetOfferableRequestUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveInboxSignalsUseCase
import bo.saludencasa.features.requests.domain.usecase.ObserveOfferSignalsUseCase
import bo.saludencasa.features.requests.domain.usecase.RejectOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitCounterOfferUseCase
import bo.saludencasa.features.requests.domain.usecase.SubmitOfferUseCase
import bo.saludencasa.features.requests.presentation.ImmediateCareViewModel
import bo.saludencasa.features.requests.presentation.InboxViewModel
import bo.saludencasa.features.requests.presentation.MyRequestsViewModel
import bo.saludencasa.features.requests.presentation.OfferThreadViewModel
import bo.saludencasa.features.requests.presentation.ScheduleAppointmentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val requestsModule =
    module {
        single { SupabaseRequestDataSource(supabase = get()) }
        single { SupabaseOfferDataSource(supabase = get(), scope = get(ApplicationScope)) }

        single<IRequestRepository> { RequestRepository(get()) }
        single<IOfferRepository> { OfferRepository(get()) }

        factory { CreateImmediateRequestUseCase(get()) }
        factory { GetMyRequestsUseCase(get()) }
        factory { CancelRequestUseCase(get()) }
        factory { GetActiveImmediateRequestUseCase(get()) }
        factory { FindProfessionalsAvailableNowUseCase(get()) }
        factory { GetInboxUseCase(get()) }
        factory { ObserveInboxSignalsUseCase(get()) }
        factory { GetBookableTimesUseCase(get()) }
        factory { CreateScheduledRequestUseCase(get()) }
        factory { SubmitOfferUseCase(get()) }
        factory { SubmitCounterOfferUseCase(get()) }
        factory { GetOfferThreadsUseCase(get()) }
        factory { GetOfferThreadUseCase(get()) }
        factory { GetOfferableRequestUseCase(get()) }
        factory { RejectOfferUseCase(get()) }
        factory { ObserveOfferSignalsUseCase(get()) }

        // «Atención ahora» reads the catalog, the patient's addresses and the
        // nearby search through the use cases of the features that own them
        // (catalogModule, locationModule, searchModule), which Koin resolves
        // across modules.
        viewModel { ImmediateCareViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
        viewModel { MyRequestsViewModel(get(), get()) }
        viewModel { InboxViewModel(get(), get()) }
        viewModel { (professionalId: String, serviceTypeId: String?) ->
            ScheduleAppointmentViewModel(professionalId, serviceTypeId, get(), get(), get(), get(), get())
        }
        viewModel { (requestId: String, professionalId: String?) ->
            OfferThreadViewModel(requestId, professionalId, get(), get(), get(), get(), get(), get())
        }
    }

package bo.saludencasa.di

import bo.saludencasa.features.search.data.datasource.SupabaseProfessionalSearchDataSource
import bo.saludencasa.features.search.data.repository.ProfessionalSearchRepository
import bo.saludencasa.features.search.domain.repository.IProfessionalSearchRepository
import bo.saludencasa.features.search.domain.usecase.GetSearchOriginUseCase
import bo.saludencasa.features.search.domain.usecase.SearchNearbyProfessionalsUseCase
import bo.saludencasa.features.search.presentation.ProfessionalSearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchModule =
    module {
        single { SupabaseProfessionalSearchDataSource(supabase = get()) }

        single<IProfessionalSearchRepository> { ProfessionalSearchRepository(get()) }

        factory { GetSearchOriginUseCase(get()) }
        factory { SearchNearbyProfessionalsUseCase(get()) }

        viewModel { ProfessionalSearchViewModel(get(), get(), get()) }
    }

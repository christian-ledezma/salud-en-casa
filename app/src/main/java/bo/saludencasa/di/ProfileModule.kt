package bo.saludencasa.di

import bo.saludencasa.features.profile.data.datasource.SupabaseProfileDataSource
import bo.saludencasa.features.profile.data.repository.ProfileRepository
import bo.saludencasa.features.profile.domain.repository.IProfileRepository
import bo.saludencasa.features.profile.domain.usecase.ChooseRoleUseCase
import bo.saludencasa.features.profile.domain.usecase.GetProfileUseCase
import bo.saludencasa.features.profile.domain.usecase.GetRoleUseCase
import bo.saludencasa.features.profile.domain.usecase.SaveProfileUseCase
import bo.saludencasa.features.profile.presentation.ProfileViewModel
import bo.saludencasa.features.profile.presentation.RoleSelectionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule =
    module {
        single { SupabaseProfileDataSource(supabase = get()) }

        single<IProfileRepository> { ProfileRepository(get()) }

        factory { GetRoleUseCase(get()) }
        factory { ChooseRoleUseCase(get()) }
        factory { GetProfileUseCase(get()) }
        factory { SaveProfileUseCase(get()) }

        viewModel { RoleSelectionViewModel(get()) }
        viewModel { ProfileViewModel(get(), get()) }
    }

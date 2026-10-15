package bo.saludencasa.di

import bo.saludencasa.features.availability.data.datasource.SupabaseAvailabilityDataSource
import bo.saludencasa.features.availability.data.repository.AvailabilityRepository
import bo.saludencasa.features.availability.domain.repository.IAvailabilityRepository
import bo.saludencasa.features.availability.domain.usecase.AddAvailabilitySlotUseCase
import bo.saludencasa.features.availability.domain.usecase.GetMyAvailabilityUseCase
import bo.saludencasa.features.availability.domain.usecase.RemoveAvailabilitySlotUseCase
import bo.saludencasa.features.availability.presentation.AvailabilityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val availabilityModule =
    module {
        single { SupabaseAvailabilityDataSource(supabase = get()) }

        single<IAvailabilityRepository> { AvailabilityRepository(get()) }

        factory { GetMyAvailabilityUseCase(get()) }
        factory { AddAvailabilitySlotUseCase(get()) }
        factory { RemoveAvailabilitySlotUseCase(get()) }

        viewModel { AvailabilityViewModel(get(), get(), get()) }
    }

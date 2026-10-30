package bo.saludencasa.di

import bo.saludencasa.features.home.presentation.PatientHomeViewModel
import bo.saludencasa.features.home.presentation.ProfessionalHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// The home screen owns no data of its own: it reads through the use cases the
// profile and requests modules declare.
val homeModule =
    module {
        viewModel { PatientHomeViewModel(get()) }
        viewModel { ProfessionalHomeViewModel(get(), get(), get(), get()) }
    }

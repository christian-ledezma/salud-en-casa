package bo.saludencasa.di

import bo.saludencasa.features.auth.presentation.AuthSmokeTestViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// Temporary: HU-01 replaces this module (plan.md, HT-05).
val authModule =
    module {
        viewModel { AuthSmokeTestViewModel(googleAuthClient = get()) }
    }

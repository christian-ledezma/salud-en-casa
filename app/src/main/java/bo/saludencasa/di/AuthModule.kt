package bo.saludencasa.di

import bo.saludencasa.features.auth.presentation.AuthSmokeTestViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Authentication feature. For now it only holds the smoke test view model,
 * which HU-01 replaces with the real sign in flow and its use cases.
 */
val authModule =
    module {
        viewModel { AuthSmokeTestViewModel(googleAuthClient = get()) }
    }

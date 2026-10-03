package bo.saludencasa.di

import bo.saludencasa.BuildConfig
import bo.saludencasa.features.auth.data.datasource.SupabaseAuthDataSource
import bo.saludencasa.features.auth.data.repository.AuthRepository
import bo.saludencasa.features.auth.domain.repository.IAuthRepository
import bo.saludencasa.features.auth.domain.usecase.ObserveSessionUseCase
import bo.saludencasa.features.auth.domain.usecase.SignInWithGoogleUseCase
import bo.saludencasa.features.auth.domain.usecase.SignOutUseCase
import bo.saludencasa.features.auth.presentation.AccountViewModel
import bo.saludencasa.features.auth.presentation.GoogleCredentialClient
import bo.saludencasa.features.auth.presentation.StartupViewModel
import bo.saludencasa.features.auth.presentation.WelcomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule =
    module {
        single {
            SupabaseAuthDataSource(
                supabase = get(),
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            )
        }

        single<IAuthRepository> { AuthRepository(get()) }

        single { GoogleCredentialClient(webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID) }

        factory { SignInWithGoogleUseCase(get()) }
        factory { SignOutUseCase(get()) }
        factory { ObserveSessionUseCase(get()) }

        viewModel { StartupViewModel(get(), get()) }
        viewModel { WelcomeViewModel(get()) }
        viewModel { AccountViewModel(get(), get(), get(), get(), get()) }
    }

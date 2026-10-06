package bo.saludencasa.di

import bo.saludencasa.features.verification.data.datasource.SupabaseVerificationDataSource
import bo.saludencasa.features.verification.data.repository.VerificationRepository
import bo.saludencasa.features.verification.domain.repository.IVerificationRepository
import bo.saludencasa.features.verification.domain.usecase.GetMyVerificationChecklistUseCase
import bo.saludencasa.features.verification.domain.usecase.UploadVerificationDocumentUseCase
import bo.saludencasa.features.verification.presentation.DocumentImageCompressor
import bo.saludencasa.features.verification.presentation.VerificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val verificationModule =
    module {
        single { SupabaseVerificationDataSource(supabase = get()) }
        single<IVerificationRepository> { VerificationRepository(get()) }

        single { DocumentImageCompressor(context = androidContext()) }

        factory { GetMyVerificationChecklistUseCase(get(), get(), get()) }
        factory { UploadVerificationDocumentUseCase(get()) }

        viewModel { VerificationViewModel(get(), get()) }
    }

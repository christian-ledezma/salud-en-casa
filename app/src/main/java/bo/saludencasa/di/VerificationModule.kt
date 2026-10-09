package bo.saludencasa.di

import bo.saludencasa.features.verification.data.datasource.SupabaseVerificationDataSource
import bo.saludencasa.features.verification.data.repository.DocumentReviewRepository
import bo.saludencasa.features.verification.data.repository.VerificationRepository
import bo.saludencasa.features.verification.domain.repository.IDocumentReviewRepository
import bo.saludencasa.features.verification.domain.repository.IVerificationRepository
import bo.saludencasa.features.verification.domain.usecase.ApproveDocumentsUseCase
import bo.saludencasa.features.verification.domain.usecase.ApproveProfessionalVerificationUseCase
import bo.saludencasa.features.verification.domain.usecase.GetDocumentReviewDossierUseCase
import bo.saludencasa.features.verification.domain.usecase.GetMyVerificationChecklistUseCase
import bo.saludencasa.features.verification.domain.usecase.GetPendingReviewSubjectsUseCase
import bo.saludencasa.features.verification.domain.usecase.GetSignedDocumentUrlUseCase
import bo.saludencasa.features.verification.domain.usecase.RejectDocumentsUseCase
import bo.saludencasa.features.verification.domain.usecase.UploadVerificationDocumentUseCase
import bo.saludencasa.features.verification.presentation.DocumentImageCompressor
import bo.saludencasa.features.verification.presentation.DocumentReviewQueueViewModel
import bo.saludencasa.features.verification.presentation.DocumentReviewViewModel
import bo.saludencasa.features.verification.presentation.VerificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val verificationModule =
    module {
        single { SupabaseVerificationDataSource(supabase = get()) }
        single<IVerificationRepository> { VerificationRepository(get()) }
        single<IDocumentReviewRepository> { DocumentReviewRepository(get()) }

        single { DocumentImageCompressor(context = androidContext()) }

        factory { GetMyVerificationChecklistUseCase(get(), get(), get()) }
        factory { UploadVerificationDocumentUseCase(get()) }
        factory { GetPendingReviewSubjectsUseCase(get()) }
        factory { GetDocumentReviewDossierUseCase(get()) }
        factory { ApproveDocumentsUseCase(get()) }
        factory { RejectDocumentsUseCase(get()) }
        factory { ApproveProfessionalVerificationUseCase(get()) }
        factory { GetSignedDocumentUrlUseCase(get()) }

        viewModel { VerificationViewModel(get(), get()) }
        viewModel { DocumentReviewQueueViewModel(get()) }
        viewModel { (profileId: String) ->
            DocumentReviewViewModel(profileId, get(), get(), get(), get(), get())
        }
    }

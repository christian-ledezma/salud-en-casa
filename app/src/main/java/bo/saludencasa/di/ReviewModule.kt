package bo.saludencasa.di

import bo.saludencasa.features.reviews.data.datasource.SupabaseReviewDataSource
import bo.saludencasa.features.reviews.data.repository.ReviewRepository
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository
import bo.saludencasa.features.reviews.domain.usecase.GetProfessionalReviewsUseCase
import bo.saludencasa.features.reviews.domain.usecase.SubmitReviewUseCase
import bo.saludencasa.features.reviews.presentation.RateAttentionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val reviewModule =
    module {
        single { SupabaseReviewDataSource(supabase = get()) }

        single<IReviewRepository> { ReviewRepository(get()) }

        factory { GetProfessionalReviewsUseCase(get()) }

        factory { SubmitReviewUseCase(repository = get(), getAttention = get()) }

        viewModel { (requestId: String, rating: Int) ->
            RateAttentionViewModel(
                requestId = requestId,
                initialRating = rating,
                getAttention = get(),
                submitReview = get(),
            )
        }
    }

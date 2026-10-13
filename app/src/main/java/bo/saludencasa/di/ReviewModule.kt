package bo.saludencasa.di

import bo.saludencasa.features.reviews.data.datasource.SupabaseReviewDataSource
import bo.saludencasa.features.reviews.data.repository.ReviewRepository
import bo.saludencasa.features.reviews.domain.repository.IReviewRepository
import bo.saludencasa.features.reviews.domain.usecase.GetProfessionalReviewsUseCase
import org.koin.dsl.module

val reviewModule =
    module {
        single { SupabaseReviewDataSource(supabase = get()) }

        single<IReviewRepository> { ReviewRepository(get()) }

        factory { GetProfessionalReviewsUseCase(get()) }
    }

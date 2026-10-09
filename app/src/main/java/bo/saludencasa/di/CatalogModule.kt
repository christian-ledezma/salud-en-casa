package bo.saludencasa.di

import bo.saludencasa.features.catalog.data.datasource.SupabaseCatalogDataSource
import bo.saludencasa.features.catalog.data.repository.CatalogRepository
import bo.saludencasa.features.catalog.domain.repository.ICatalogRepository
import bo.saludencasa.features.catalog.domain.usecase.DeclareServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.GetMyDeclaredServicesUseCase
import bo.saludencasa.features.catalog.domain.usecase.RemoveServiceUseCase
import bo.saludencasa.features.catalog.domain.usecase.UpdateServicePriceUseCase
import bo.saludencasa.features.catalog.presentation.MyServicesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val catalogModule =
    module {
        single { SupabaseCatalogDataSource(supabase = get()) }

        single<ICatalogRepository> { CatalogRepository(get()) }

        factory { GetMyDeclaredServicesUseCase(get()) }
        factory { DeclareServiceUseCase(get()) }
        factory { UpdateServicePriceUseCase(get()) }
        factory { RemoveServiceUseCase(get()) }

        viewModel { MyServicesViewModel(get(), get(), get(), get()) }
    }

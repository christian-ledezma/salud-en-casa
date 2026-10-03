package bo.saludencasa.di

import bo.saludencasa.features.location.data.datasource.DeviceLocationDataSource
import bo.saludencasa.features.location.data.datasource.PlatformGeocoderDataSource
import bo.saludencasa.features.location.data.datasource.SupabaseAddressDataSource
import bo.saludencasa.features.location.data.repository.AddressRepository
import bo.saludencasa.features.location.data.repository.DeviceLocationRepository
import bo.saludencasa.features.location.data.repository.GeocodingRepository
import bo.saludencasa.features.location.domain.repository.IAddressRepository
import bo.saludencasa.features.location.domain.repository.IDeviceLocationRepository
import bo.saludencasa.features.location.domain.repository.IGeocodingRepository
import bo.saludencasa.features.location.domain.usecase.DeleteAddressUseCase
import bo.saludencasa.features.location.domain.usecase.DescribePointUseCase
import bo.saludencasa.features.location.domain.usecase.FindPlaceUseCase
import bo.saludencasa.features.location.domain.usecase.GetAddressUseCase
import bo.saludencasa.features.location.domain.usecase.GetCurrentPositionUseCase
import bo.saludencasa.features.location.domain.usecase.GetMyAddressesUseCase
import bo.saludencasa.features.location.domain.usecase.SaveAddressUseCase
import bo.saludencasa.features.location.domain.usecase.SetPrimaryAddressUseCase
import bo.saludencasa.features.location.domain.usecase.SetProfessionalBaseAddressUseCase
import bo.saludencasa.features.location.presentation.AddressListViewModel
import bo.saludencasa.features.location.presentation.AddressViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val locationModule =
    module {
        single { SupabaseAddressDataSource(supabase = get()) }
        single { PlatformGeocoderDataSource(context = androidContext()) }
        single { DeviceLocationDataSource(context = androidContext()) }

        single<IAddressRepository> { AddressRepository(get()) }
        // The repository holds what has already been asked of the geocoder, so
        // it is a single: one per screen would forget it between visits.
        single<IGeocodingRepository> { GeocodingRepository(get()) }
        single<IDeviceLocationRepository> { DeviceLocationRepository(get()) }

        factory { GetAddressUseCase(get()) }
        factory { GetMyAddressesUseCase(get()) }
        factory { SaveAddressUseCase(get()) }
        factory { SetPrimaryAddressUseCase(get()) }
        factory { SetProfessionalBaseAddressUseCase(get()) }
        factory { DeleteAddressUseCase(get()) }
        factory { DescribePointUseCase(get()) }
        factory { FindPlaceUseCase(get()) }
        factory { GetCurrentPositionUseCase(get()) }

        viewModel { (addressId: String?) -> AddressViewModel(addressId, get(), get(), get(), get(), get()) }
        viewModel { AddressListViewModel(get(), get(), get(), get(), get()) }
    }

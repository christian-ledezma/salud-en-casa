package bo.saludencasa.di

import bo.saludencasa.features.notifications.data.datasource.FcmRegistrationDataSource
import bo.saludencasa.features.notifications.data.datasource.NotificationPreferencesDataSource
import bo.saludencasa.features.notifications.data.datasource.ShownNotificationsDataSource
import bo.saludencasa.features.notifications.data.datasource.SupabaseDeviceDataSource
import bo.saludencasa.features.notifications.data.repository.NotificationRepository
import bo.saludencasa.features.notifications.domain.repository.INotificationRepository
import bo.saludencasa.features.notifications.domain.usecase.AnswerNotificationPromptUseCase
import bo.saludencasa.features.notifications.domain.usecase.ObserveNotificationPromptAnsweredUseCase
import bo.saludencasa.features.notifications.domain.usecase.OpenNotificationUseCase
import bo.saludencasa.features.notifications.domain.usecase.RegisterDeviceUseCase
import bo.saludencasa.features.notifications.domain.usecase.ResolveIncomingNotificationUseCase
import bo.saludencasa.features.notifications.domain.usecase.StartReceivingNotificationsUseCase
import bo.saludencasa.features.notifications.domain.usecase.StopReceivingNotificationsUseCase
import bo.saludencasa.features.notifications.presentation.NotificationPermissionViewModel
import bo.saludencasa.features.notifications.presentation.NotificationPublisher
import bo.saludencasa.features.notifications.presentation.OpenNotificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val notificationsModule =
    module {
        single { SupabaseDeviceDataSource(supabase = get()) }
        single { FcmRegistrationDataSource() }
        single { NotificationPreferencesDataSource(dataStore = get()) }
        single { ShownNotificationsDataSource(androidContext()) }

        single<INotificationRepository> { NotificationRepository(get(), get(), get(), get()) }

        single { NotificationPublisher(androidContext()) }

        factory { StartReceivingNotificationsUseCase(get()) }
        factory { RegisterDeviceUseCase(get()) }
        factory { StopReceivingNotificationsUseCase(get()) }
        factory { ResolveIncomingNotificationUseCase(get()) }
        factory { OpenNotificationUseCase(get(), get(), get()) }
        factory { ObserveNotificationPromptAnsweredUseCase(get()) }
        factory { AnswerNotificationPromptUseCase(get()) }

        viewModel { NotificationPermissionViewModel(get(), get()) }
        viewModel { OpenNotificationViewModel(get()) }
    }

package bo.saludencasa

import android.app.Application
import bo.saludencasa.di.authModule
import bo.saludencasa.di.coreModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SaludEnCasaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@SaludEnCasaApplication)
            modules(coreModule, authModule)
        }
    }
}

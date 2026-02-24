package com.personal.cinevault.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CineVaultApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)   // use ERROR in release to silence verbose output
            androidContext(this@CineVaultApp)
            modules(appModule)
        }
    }
}

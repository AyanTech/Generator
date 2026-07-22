package com.alirezabdn.generator

import android.app.Application
import com.alirezabdn.generator.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/** Application entry point responsible for starting the Koin dependency graph. */
class GeneratorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@GeneratorApplication)
            modules(appModules)
        }
    }
}

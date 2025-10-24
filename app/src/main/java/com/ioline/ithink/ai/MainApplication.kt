package com.ioline.ithink.ai

import android.app.Application
import com.ioline.ithink.ai.data.ObjectBoxStore
import com.ioline.ithink.ai.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MainApplication)
            modules(AppModule().module)
        }
        ObjectBoxStore.init(this)
    }
}

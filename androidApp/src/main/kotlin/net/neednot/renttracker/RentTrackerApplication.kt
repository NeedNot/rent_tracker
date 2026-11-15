package net.neednot.renttracker

import android.app.Application
import net.neednot.renttracker.app.androidPlatformModule
import net.neednot.renttracker.app.initKoin
import net.neednot.renttracker.di.androidAppModule
import net.neednot.renttracker.push.FirebaseSetup
import org.koin.android.ext.koin.androidContext

class RentTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseSetup.initialize(this)
        initKoin(platformModules = listOf(androidPlatformModule, androidAppModule)) {
            androidContext(this@RentTrackerApplication)
        }
    }
}

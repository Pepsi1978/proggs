package de.frank.perfectmoment

import android.app.Application
import de.frank.perfectmoment.di.AppContainer

class PerfectMomentApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appLockManager.start()
    }
}

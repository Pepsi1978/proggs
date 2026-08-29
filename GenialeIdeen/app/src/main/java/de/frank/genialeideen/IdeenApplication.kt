package de.frank.genialeideen

import android.app.Application
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.observability.IdeenCrashHandler
import de.frank.genialeideen.observability.IdeenLog

class GenialeIdeenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        IdeenLog.start(this)
        IdeenCrashHandler.installiere(this)
        container = AppContainer(this)
        container.appLockManager.start()
    }
}

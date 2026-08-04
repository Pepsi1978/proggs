package de.frank.perfectmoment

import android.app.Application
import de.frank.perfectmoment.di.AppContainer
import de.frank.perfectmoment.session.SessionSilence

class PerfectMomentApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // A fresh process means no session is running. Was the last one killed before it could
        // clean up, do-not-disturb is still switched on — this takes it back. Runs before the
        // session service can switch it on again, so a starting session is unaffected.
        SessionSilence(this).restore()
        container = AppContainer(this)
        container.appLockManager.start()
        container.autoBackup.start()
    }
}

package de.frank.genialeideen.di

import android.app.Application
import de.frank.genialeideen.auth.CodexAuthManager
import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.repository.IdeenRepository
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.security.AppLockManager
import de.frank.genialeideen.speech.Vorleser

class AppContainer(application: Application) {
    val database = GenialeIdeenDatabase.getInstance(application)
    val settings = SecureSettings(application)
    val ideenRepository = IdeenRepository(database)
    val codexAuthManager = CodexAuthManager(application)
    val appLockManager = AppLockManager(settings)
    val vorleser = Vorleser.hole(application, settings)
}

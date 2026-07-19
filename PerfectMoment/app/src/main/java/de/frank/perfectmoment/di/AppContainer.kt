package de.frank.perfectmoment.di

import android.app.Application
import de.frank.perfectmoment.auth.CodexAuthManager
import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import de.frank.perfectmoment.data.repository.RoomContentRepository
import de.frank.perfectmoment.data.repository.RoomSessionRepository
import de.frank.perfectmoment.data.settings.SecureSettings
import de.frank.perfectmoment.security.AppLockManager
import de.frank.perfectmoment.session.SessionController

class AppContainer(application: Application) {
    val database = PerfectMomentDatabase.getInstance(application)
    val settings = SecureSettings(application)
    val contentRepository = RoomContentRepository(database)
    val sessionRepository = RoomSessionRepository(database)
    val codexAuthManager = CodexAuthManager(application)
    val appLockManager = AppLockManager(settings)
    val sessionController = SessionController(
        context = application,
        settings = settings,
        contentRepository = contentRepository,
        sessionRepository = sessionRepository,
        codexAuthManager = codexAuthManager,
    )
}

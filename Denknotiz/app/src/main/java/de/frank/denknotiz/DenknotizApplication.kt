package de.frank.denknotiz

import android.app.Application
import de.frank.denknotiz.ai.CodexClient
import de.frank.denknotiz.data.DenknotizRepository
import de.frank.denknotiz.data.SafBackup
import de.frank.denknotiz.data.SecureSettings
import de.frank.denknotiz.data.local.DenknotizDatabase

class DenknotizApplication : Application() {
    val container by lazy { AppContainer(this) }
}

class AppContainer(val application: Application) {
    val database = DenknotizDatabase.create(application)
    val repository = DenknotizRepository(database)
    val settings = SecureSettings(application)
    val backup = SafBackup(application)
    val codex = CodexClient(application)
}

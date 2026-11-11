package de.frank.wecker

import android.app.Application
import android.os.UserManager

class WeckerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { SchlafErinnerung.ensureChannel(this) }
        // Standort für den Sonnenwecker laden, bevor irgendein Pfad eine Weckzeit berechnet.
        runCatching { SonnenOrt.aktualisiere(this) }
        // Im Direct-Boot-Pfad weder verschlüsselte Schlüssel noch WorkManager öffnen.
        if (getSystemService(UserManager::class.java).isUserUnlocked) PreparationWorker.periodic(this)
    }
}

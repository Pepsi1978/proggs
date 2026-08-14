package de.frank.experimente

import android.app.Application
import android.util.Log
import de.frank.experimente.ai.Aufgaben
import de.frank.experimente.auth.CodexZugang
import de.frank.experimente.data.backup.BackupVerwaltung
import de.frank.experimente.data.local.ExperimenteDatenbank
import de.frank.experimente.data.repo.Ablage
import de.frank.experimente.data.settings.Einstellungen
import de.frank.experimente.notify.Erinnerungen
import de.frank.experimente.tts.Vorleser

/**
 * Hält die Bausteine, die die ganze App über braucht. Kein Baukasten-Rahmenwerk — die App
 * hat genau einen Satz davon, und der entsteht hier.
 */
class ExperimenteApp : Application() {

    lateinit var einstellungen: Einstellungen
        private set
    lateinit var codex: CodexZugang
        private set
    lateinit var ablage: Ablage
        private set
    lateinit var backup: BackupVerwaltung
        private set
    lateinit var vorleser: Vorleser
        private set

    override fun onCreate() {
        super.onCreate()

        // Version beim Start ins Log — damit sichtbar ist, welche Fassung läuft.
        Log.i(TAG, "Experimente ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) — ${BuildConfig.VERSION_BUMPED_AT}")

        einstellungen = Einstellungen(this)
        codex = CodexZugang(this)
        val datenbank = ExperimenteDatenbank.hol(this)
        backup = BackupVerwaltung(this, datenbank, einstellungen)
        backup.stelleImportKonsistenzHer()
        ablage = Ablage(
            db = datenbank,
            einstellungen = einstellungen,
            aufgabenKi = Aufgaben(codex),
        )
        vorleser = Vorleser(this, einstellungen)

        Erinnerungen.legeKanalAn(this)
        // Nach einem Neuinstallieren sind die Weckzeiten weg — hier wieder setzen.
        Erinnerungen.setzeAlle(this, einstellungen)
    }

    private companion object {
        const val TAG = "Experimente"
    }
}

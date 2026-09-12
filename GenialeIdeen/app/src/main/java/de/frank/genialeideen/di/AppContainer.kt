package de.frank.genialeideen.di

import android.app.Application
import de.frank.genialeideen.auth.CodexAuthManager
import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.repository.IdeenRepository
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.security.AppLockManager
import de.frank.genialeideen.speech.Vorleser
import de.frank.module.sicherung.AutoSicherung
import de.frank.module.sicherung.IDEEN_SICHERUNGSNAMEN
import de.frank.module.sicherung.IdeenProtokoll
import de.frank.module.sicherung.IdeenRuecknahme
import de.frank.module.sicherung.IdeenSicherungsInhalt
import de.frank.module.sicherung.SicherungsDienst
import de.frank.module.sicherung.gewaehlteTeile

class AppContainer(application: Application) {
    val database = GenialeIdeenDatabase.getInstance(application)
    val settings = SecureSettings(application)
    val ideenRepository = IdeenRepository(database)
    val codexAuthManager = CodexAuthManager(application)
    val appLockManager = AppLockManager(settings)
    val vorleser = Vorleser.hole(application, settings)

    // ---- Sicherung (Modul M1.1) ----

    private val sicherungsRuecknahme = IdeenRuecknahme(database)

    /**
     * Der Sicherungsdienst gehört hierher und nicht ins ViewModel: Die selbsttätige Sicherung
     * hängt am Lebenslauf des ganzen Vorgangs, nicht an dem eines Bildschirms.
     */
    val sicherungsInhalt = IdeenSicherungsInhalt(database, sicherungsRuecknahme)

    val sicherung = SicherungsDienst(
        context = application,
        inhalt = sicherungsInhalt,
        namen = IDEEN_SICHERUNGSNAMEN,
        ruecknahme = sicherungsRuecknahme,
        protokoll = IdeenProtokoll,
        umfangGeber = { gewaehlteTeile(settings.sicherungsTeile) },
    )

    val autoSicherung = AutoSicherung(sicherung, { settings.autoBackupEnabled }, IdeenProtokoll)

    /** Was das letzte Einspielen bewirkt hat — für die Meldung an den Benutzer. */
    fun letzterEinspielbericht() = sicherungsRuecknahme.bericht()
}

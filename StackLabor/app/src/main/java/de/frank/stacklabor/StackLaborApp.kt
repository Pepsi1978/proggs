package de.frank.stacklabor

import android.app.Application
import de.frank.stacklabor.codex.CodexAnmeldung
import de.frank.stacklabor.codex.CodexDienst
import de.frank.stacklabor.daten.Einstellungen
import de.frank.stacklabor.daten.StackLaborDatenbank
import de.frank.stacklabor.daten.StackRepository
import de.frank.stacklabor.logik.AuswertungsSteuerung
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Der Behälter der App.
 *
 * Bewusst ohne Abhängigkeits-Rahmenwerk: StackLabor hat eine überschaubare Zahl an
 * Bausteinen, und ein handgeschriebener Behälter macht sichtbar, was woran hängt —
 * genau wie in `PerfectMoment/di/AppContainer.kt`.
 */
class StackLaborApp : Application() {

    lateinit var einstellungen: Einstellungen
        private set
    lateinit var datenbank: StackLaborDatenbank
        private set
    lateinit var repository: StackRepository
        private set
    lateinit var codexAnmeldung: CodexAnmeldung
        private set
    lateinit var codexDienst: CodexDienst
        private set
    lateinit var auswertung: AuswertungsSteuerung
        private set

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        einstellungen = Einstellungen(this)
        datenbank = StackLaborDatenbank.erzeuge(this)
        repository = StackRepository(this, datenbank, einstellungen)
        codexAnmeldung = CodexAnmeldung(this)
        codexDienst = CodexDienst(codexAnmeldung)
        auswertung = AuswertungsSteuerung(bereich, datenbank, repository, codexDienst, einstellungen)

        // F-21: Beim allerersten Start kommen die sechs Stacks, 72 Eintraege und
        // zwoelf Ziele aus `assets/startbestand.json` in die Datenbank.
        bereich.launch { repository.stelleStartbestandSicher() }
    }
}

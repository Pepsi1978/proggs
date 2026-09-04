package de.frank.codexkompass

import android.app.Application
import android.content.Context
import de.frank.codexkompass.ai.CodexClient
import de.frank.codexkompass.audio.GroqTranskribierer
import de.frank.codexkompass.audio.FilterSchalter
import de.frank.codexkompass.audio.Mikrofon
import de.frank.codexkompass.data.EinstellungenStore
import de.frank.codexkompass.data.KompassRepository
import de.frank.codexkompass.observability.KompassCrashHandler
import de.frank.codexkompass.observability.KompassLog
import de.frank.codexkompass.security.AppSperre
import de.frank.codexkompass.tts.QwenStimmVerwaltung
import de.frank.codexkompass.tts.VorleseManager
import de.frank.codexkompass.update.Aktualisierer
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Hält die langlebigen Bausteine der App.
 *
 * Bewusst von Hand zusammengesteckt statt über ein Rahmenwerk: Es sind wenige Bausteine, und
 * so ist an einer Stelle nachlesbar, was wovon abhängt.
 */
class KompassContainer(context: Context) {

    private val appContext = context.applicationContext

    val einstellungen = EinstellungenStore(appContext)
    val repository = KompassRepository(appContext)
    val codex = CodexClient(appContext)
    val vorlesen = VorleseManager(appContext, einstellungen)
    val mikrofon = Mikrofon(appContext)
    val stimmVerwaltung = QwenStimmVerwaltung { einstellungen.alibabaSchluessel }
    val appSperre = AppSperre(einstellungen)

    val transkribierer = GroqTranskribierer(
        schluesselGeber = { einstellungen.groqSchluessel },
        modellGeber = { einstellungen.groqModell },
        schalterGeber = {
            FilterSchalter(
                schicht1Stille = einstellungen.filterSchichtAktiv(1),
                schicht2Kennzahlen = einstellungen.filterSchichtAktiv(2),
                schicht3Zeitstempel = einstellungen.filterSchichtAktiv(3),
                schicht4Floskeln = einstellungen.filterSchichtAktiv(4),
            )
        },
    )

    val aktualisierer = Aktualisierer(repository, codex, einstellungen)

    fun beende() {
        vorlesen.beende()
        mikrofon.gibFrei()
        transkribierer.beende()
        stimmVerwaltung.beende()
        aktualisierer.beende()
        codex.brichAnfragenAb()
    }
}

class KompassApplication : Application() {

    lateinit var container: KompassContainer
        private set

    private val startBereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Zuerst die Beobachtungsschicht: Was danach schiefgeht, soll aufgezeichnet werden.
        KompassLog.start(this, UUID.randomUUID().toString().take(8))
        KompassCrashHandler.install()
        KompassLog.info(
            "Application",
            "onCreate",
            "Codex Kompass startet",
            mapOf(
                "version" to BuildConfig.VERSION_NAME,
                "gebaut" to BuildConfig.VERSION_BUMPED_AT,
                "wissenStand" to BuildConfig.SEEDED_CLI_VERSION,
            ),
        )

        container = KompassContainer(this)
        container.appSperre.beobachte()
        container.appSperre.beimKaltstart()

        // Die Erstbefüllung läuft im Hintergrund. Die Oberfläche zeigt so lange ihren
        // Ladezustand und wird durch den Datenfluss von selbst gefüllt, sobald es fertig ist.
        startBereich.launch {
            runCatching { container.repository.befuelleWennLeer(this@KompassApplication) }
                .onFailure {
                    KompassLog.error(
                        "Application",
                        "onCreate",
                        "Erstbefüllung fehlgeschlagen",
                        mapOf("grund" to it.message),
                    )
                }
        }
    }

    override fun onTerminate() {
        container.beende()
        super.onTerminate()
    }
}

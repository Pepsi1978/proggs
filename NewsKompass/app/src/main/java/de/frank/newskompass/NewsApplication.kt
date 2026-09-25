package de.frank.newskompass

import android.app.Application
import de.frank.newskompass.ai.CodexClient
import de.frank.newskompass.data.AusgabenSpeicher
import de.frank.newskompass.data.EinstellungenStore
import de.frank.newskompass.news.NewsRecherche
import de.frank.newskompass.news.SprachFrage
import de.frank.newskompass.news.Zeitplan
import de.frank.newskompass.observability.KompassLog
import de.frank.newskompass.tts.VorleseManager
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hält die langlebigen Bausteine — ein Exemplar für Oberfläche und Hintergrundlauf. */
class NewsApplication : Application() {

    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val einstellungen by lazy { EinstellungenStore(this) }
    val codex by lazy { CodexClient(this) }
    val speicher by lazy { AusgabenSpeicher(this) }
    val recherche by lazy { NewsRecherche(codex, einstellungen, speicher) }
    val vorleser by lazy { VorleseManager(this, einstellungen) }
    val sprachFrage by lazy { SprachFrage(this) }

    override fun onCreate() {
        super.onCreate()
        KompassLog.start(this, UUID.randomUUID().toString().take(8))
        Zeitplan.legeKanaeleAn(this)
        Zeitplan.plane(this)
        bereich.launch { speicher.lade() }
    }
}

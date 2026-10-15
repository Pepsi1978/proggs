package de.frank.stacklabor.werftstudio

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.room.InvalidationTracker
import de.frank.stacklabor.werftstudio.service.tts.TtsServiceIntegration
import de.frank.stacklabor.werftstudio.service.work.CodexWorkIntegration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StackLaborApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppContainer(this) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CodexWorkIntegration.runner = container.evaluator
        TtsServiceIntegration.engine = container.ttsEngine
        TtsServiceIntegration.contentIntentFactory = { context ->
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        meldeAenderungenAnJarvis()
        applicationScope.launch { container.repository.initialisiereStartbestandWennLeer() }
    }

    /** Eigener Faden für das Signal an Jarvis, hält den Hauptfaden frei. */
    private val jarvisTakt = Executors.newSingleThreadScheduledExecutor()
    @Volatile private var jarvisSignal: ScheduledFuture<*>? = null

    /**
     * Jede Änderung an Stacks, Mitteln, Dosen, Zielen oder Fragen meldet sich bei Jarvis, damit sein Live-Gedächtnis
     * sofort nachzieht. Über den Änderungsmelder von Room zählen Oberfläche, Import und Schreibaufrufe über die Brücke
     * gleichermaßen; Lesen ändert nichts und löst kein Signal aus. Mehrere Änderungen binnen 700 ms ergeben ein Signal,
     * ohne installiertes Jarvis geht es ins Leere. Auswertungen (Codex) zählen nicht: Sie ändern den Plan nicht.
     */
    private fun meldeAenderungenAnJarvis() {
        val bruecke = Uri.parse("content://$packageName.jarvis")
        container.database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(
                "stack", "stack_eintrag", "stack_eintrag_dosis", "alternations_partner", "mittel", "wirkstoffkomponente",
                "ziel", "stack_ziel", "eigene_frage",
            ) {
                override fun onInvalidated(tables: Set<String>) {
                    jarvisSignal?.cancel(false)
                    jarvisSignal = jarvisTakt.schedule(
                        { runCatching { contentResolver.notifyChange(bruecke, null) } },
                        700,
                        TimeUnit.MILLISECONDS,
                    )
                }
            },
        )
    }
}

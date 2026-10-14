package de.frank.entropyreducer.data.bruecke

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Live-Gedächtnis von Jarvis: meldet jede Änderung an den Daten, die Jarvis über die [JarvisBruecke]
 * liest, per `notifyChange(content://<paket>.jarvis)`. Jarvis beobachtet diese Wurzel-URI und frischt
 * sein Gedächtnis sofort auf.
 *
 * Zentral an der Datenbank statt an jeder Schreibstelle: Der Room-InvalidationTracker feuert bei jedem
 * Schreiben in die beobachteten Tabellen — egal ob Oberfläche, Hintergrund-Abgleich (Worker,
 * Vordergrund-Sync), Import oder der `abgleich`-Aufruf der Brücke selbst. Reines Lesen löst nichts aus,
 * deshalb keine Schleife, wenn Jarvis nach dem Signal neu liest. Dazu kommt der Abgleich-Stand
 * (DataStore), den die Brücke unter „stand" ausliefert. Mehrere Änderungen in Folge = ein Signal.
 */
@Singleton
class JarvisSignal @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val db: AppDatabase,
    private val appSettings: AppSettings,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            runCatching {
                val wurzel = Uri.parse("content://${ctx.packageName}.jarvis")
                merge(
                    db.invalidationTracker.createFlow(*TABELLEN, emitInitialState = false).map { },
                    // Erster Wert ist der gespeicherte Stand beim Start, keine Änderung.
                    appSettings.biomarkerSyncStatusFlow.drop(1).map { },
                )
                    .debounce(ENTPRELLEN_MS)
                    .collect { runCatching { ctx.contentResolver.notifyChange(wurzel, null) } }
            }
        }
    }

    private companion object {
        const val ENTPRELLEN_MS = 800L

        /** Genau die Tabellen, aus denen die Brücke liest (Whoop, Oura, Körperwerte, Trainings). */
        val TABELLEN = arrayOf(
            "biomarker_snapshots",
            "oura_daily_readiness",
            "oura_daily_sleep",
            "oura_daily_resilience",
            "oura_daily_activity",
            "hc_value_cache",
            "amazfit_workouts",
        )
    }
}

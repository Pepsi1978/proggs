package de.frank.epigenetik

import android.app.Application
import android.net.Uri
import androidx.room.InvalidationTracker
import de.frank.epigenetik.data.Datenbank
import de.frank.epigenetik.data.Repository
import de.frank.epigenetik.ki.Forscher
import de.frank.epigenetik.ki.KiLog
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EpigenetikApplication : Application() {
    private val takt = Executors.newSingleThreadScheduledExecutor()
    private var signal: ScheduledFuture<*>? = null

    override fun onCreate() {
        super.onCreate()
        KiLog.init(this)
        Forscher.kanalAnlegen(this)
        meldeAenderungenAnJarvis()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { runCatching { Repository.get(this@EpigenetikApplication).offeneFortsetzen() } }
    }

    /** Jede Änderung an der Bibliothek meldet Jarvis über die Brücke (entprellt), damit sein Gedächtnis nachzieht. */
    private fun meldeAenderungenAnJarvis() {
        val bruecke = Uri.parse("content://$packageName.jarvis")
        Datenbank.get(this).invalidationTracker.addObserver(
            object : InvalidationTracker.Observer("recherche", "schalter_wirkung") {
                override fun onInvalidated(tables: Set<String>) {
                    signal?.cancel(false)
                    signal = takt.schedule({ runCatching { contentResolver.notifyChange(bruecke, null) } }, 800, TimeUnit.MILLISECONDS)
                }
            },
        )
    }
}

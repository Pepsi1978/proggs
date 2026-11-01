package de.frank.jarvis.ablage

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.meldung.WeckerTon
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.GespraechsFokus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Derselbe Ablauf für den Vordergrunddienst und den Ersatzweg: nie eine Erinnerung dafür anlegen. */
object ErinnerungsWeckablauf {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private fun blockiert() = GespraechsFokus.stilleHinweise || GespraechsFokus.aufnahme

    fun starten(context: Context, nummer: Int, faellige: () -> List<Faellig>): Job {
        val app = context.applicationContext
        return bereich.launch {
            val p = Meldungen.profil(app, Meldungsarten.ERINNERUNG_WECKER)
            val wach = app.getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:erinnerungswecker").apply { setReferenceCounted(false) }
            var spricht = false
            var stimme: Job? = null
            var gesprochen: Faellig? = null
            try {
                coroutineScope {
                    // Weckt auch bei reinem Vorlesen ohne laufenden MediaPlayer rechtzeitig wieder auf.
                    val wache = launch {
                        var erneuern = 0L
                        var naechsterTonversuch = 0L
                        var ton = p.weckton
                        while (isActive) {
                            val jetzt = SystemClock.elapsedRealtime()
                            if (jetzt >= erneuern) {
                                runCatching { wach.acquire(15 * 60_000L) }.onFailure { Logbuch.fehler("Erinnerungswecker wach halten", it) }
                                erneuern = jetzt + 60_000L
                            }
                            val liste = faellige()
                            if (blockiert() || !p.vorlesen || gesprochen?.let { f -> liste.none { it.quittung == f.quittung && it.erinnerung == f.erinnerung } } == true)
                                stimme?.cancel()
                            val hoerbar = p.ton && !blockiert() && !spricht && liste.isNotEmpty()
                            if (!hoerbar || ton != p.weckton) {
                                WeckerTon.stopp(nummer)
                                naechsterTonversuch = 0L
                            }
                            ton = p.weckton
                            if (hoerbar && !WeckerTon.klingelt(nummer) && jetzt >= naechsterTonversuch) {
                                naechsterTonversuch = jetzt + 5_000L
                                WeckerTon.starte(app, nummer, ton, Meldungsarten.ERINNERUNG_WECKER.id)
                            }
                            delay(200L)
                        }
                    }
                    suspend fun warte(ms: Long): Boolean {
                        val bis = SystemClock.elapsedRealtime() + ms
                        while (SystemClock.elapsedRealtime() < bis) {
                            if (blockiert() || faellige().isEmpty()) return false
                            delay(minOf(200L, (bis - SystemClock.elapsedRealtime()).coerceAtLeast(1L)))
                        }
                        return !blockiert() && faellige().isNotEmpty()
                    }
                    try {
                        while (isActive && faellige().isNotEmpty()) {
                            while (blockiert()) delay(200L)
                            if (p.ton && !warte((p.wecktonSekunden.takeIf { it > 0 } ?: 10) * 1_000L)) continue
                            if (p.vorlesen && !blockiert()) {
                                spricht = true
                                WeckerTon.stopp(nummer)
                                try {
                                    for (f in faellige()) {
                                        if (blockiert() || !p.vorlesen) break
                                        if (faellige().none { it.quittung == f.quittung && it.erinnerung == f.erinnerung }) continue
                                        gesprochen = f
                                        stimme = launch {
                                            try { ErinnerungStimme.sprich(app, f.erinnerung, alsWecker = true) }
                                            catch (c: CancellationException) { throw c }
                                            catch (e: Exception) { Logbuch.fehler("Erinnerungswecker vorlesen", e) }
                                        }
                                        stimme?.join()
                                    }
                                } finally { stimme?.cancel(); stimme = null; gesprochen = null; spricht = false }
                            }
                            warte(120_000L)
                        }
                    } finally { wache.cancel() }
                }
            } finally {
                stimme?.cancel()
                WeckerTon.stopp(nummer)
                runCatching { if (wach.isHeld) wach.release() }
            }
        }
    }
}

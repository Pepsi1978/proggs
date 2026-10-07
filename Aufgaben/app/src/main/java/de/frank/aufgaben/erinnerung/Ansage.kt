package de.frank.aufgaben.erinnerung

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.data.Tage
import de.frank.aufgaben.tts.EdgeTtsPlayer
import de.frank.aufgaben.tts.GoogleCloudTtsPlayer
import de.frank.aufgaben.tts.QwenTtsPlayer
import de.frank.aufgaben.tts.TtsProvider
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Vorgelesene Erinnerungen, wie im Genialen Wecker: Beim Speichern entstehen sechs Fassungen des
 * Aufgabentextes mit der gewählten Stimme (leicht unterschiedliches Tempo). Sie liegen als Dateien in
 * der App, damit die Erinnerung auch offline spricht.
 *
 * Die Dateien werden über einen Hash aus Stimme, Variante und Text gefunden; die Datenbank weiß davon
 * nichts. Ändert sich Text oder Stimme, entstehen neue Dateien, die alten räumt [anstossen] später weg.
 * Jede Fassung wird einzeln und mit einem eigenen Player erzeugt: Ein Player bricht beim nächsten
 * speak() den vorigen Auftrag ab.
 */
object Ansage {
    const val ANZAHL = 6
    /** Pause zwischen zwei vorgelesenen Fassungen. */
    const val PAUSE_MS = 3_000L
    private const val TAG = "Ansage"
    private const val FORMAT = "ansage-v1"
    /** Behutsame Tempo-Unterschiede wie im Wecker; Stimme und Grundtempo bleiben. */
    private val FAKTOREN = floatArrayOf(1f, .98f, 1.02f, .96f, 1.04f, 1.01f)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val angefordert = AtomicBoolean(false)

    private data class Stimme(val anbieter: String, val id: String, val tempo: Float, val deutsch: Boolean)

    fun ordner(context: Context): File = File(context.filesDir, "ansagen").apply { mkdirs() }

    /** Was vorgelesen wird: der (von der KI in Befehlsform gebrachte) Aufgabentext, sonst der Titel. */
    fun text(a: Aufgabe): String = a.text.trim().ifBlank { a.titel.trim() }.take(800)

    /** Aufgaben, deren Erinnerung noch kommt und vorlesen soll. */
    fun braucht(a: Aufgabe, heute: Long): Boolean =
        a.vorlesen && a.erinnerung && !a.erledigt && a.tag != null && a.minuten != null && a.tag >= heute

    private fun gewaehlt(e: Einstellungen): Stimme = when (e.ttsAnbieter) {
        TtsProvider.GOOGLE_CLOUD.id -> Stimme(TtsProvider.GOOGLE_CLOUD.id, e.googleStimme, e.sprechtempo, e.immerDeutsch)
        // Die eigene Stimme kennt kein Tempo; jede Synthese betont von selbst etwas anders.
        TtsProvider.QWEN_CLONE.id -> Stimme(TtsProvider.QWEN_CLONE.id, e.qwenStimme, 1f, true)
        else -> edge(e)
    }

    /** Notfallstimme ohne Schlüssel, falls die gewählte Stimme nicht erreichbar ist. */
    private fun edge(e: Einstellungen) = Stimme(TtsProvider.EDGE.id, e.edgeStimme, e.sprechtempo, e.immerDeutsch)

    private fun datei(context: Context, s: Stimme, variante: Int, text: String): File {
        val endung = if (s.anbieter == TtsProvider.QWEN_CLONE.id) "wav" else "mp3"
        return File(ordner(context), "a_${hash("$FORMAT|${s.anbieter}|${s.id}|${s.tempo}|${s.deutsch}|$variante|$text")}.$endung")
    }

    private fun gueltig(f: File) = f.length() > 44

    /** Die fertigen Fassungen in Abspielreihenfolge: gewählte Stimme, sonst Edge-Notfallstimme. Leer = nichts vorbereitet. */
    fun dateien(context: Context, a: Aufgabe): List<File> {
        val e = Einstellungen.get(context)
        val t = text(a)
        if (t.isBlank()) return emptyList()
        val s = gewaehlt(e)
        val n = edge(e)
        return (0 until ANZAHL).mapNotNull { i -> datei(context, s, i, t).takeIf { gueltig(it) } ?: datei(context, n, i, t).takeIf { gueltig(it) } }
    }

    /**
     * Erzeugt im Hintergrund alle fehlenden Fassungen für kommende Erinnerungen und räumt alte weg.
     * Mehrfache Aufrufe während eines Durchlaufs werden zu einem weiteren Durchlauf zusammengefasst.
     */
    fun anstossen(context: Context) {
        val app = context.applicationContext
        if (!angefordert.compareAndSet(false, true)) return
        scope.launch {
            mutex.withLock {
                angefordert.set(false)
                runCatching { durchlauf(app) }.onFailure { Log.w(TAG, "Durchlauf fehlgeschlagen", it) }
            }
        }
    }

    private suspend fun durchlauf(app: Context) {
        val heute = Tage.heute()
        val offen = AufgabenRepository.get(app).alleEinmal().filter { braucht(it, heute) }
            .sortedBy { Tage.millis(it.tag!!, it.minuten!!) }
        offen.forEach { a ->
            try {
                bereite(app, a)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                // Kein Netz o. Ä.: Beim nächsten Anstoß (Änderung, App-Start, Mitternacht) neuer Versuch.
                Log.w(TAG, "Aufgabe ${a.id}: ${e.message}")
            }
        }
        aufraeumen(app, offen)
    }

    private suspend fun bereite(app: Context, a: Aufgabe) = bereiteText(app, text(a), ANZAHL)

    /**
     * Die Ansage am Ende des Fokus-Timers (eine Fassung) mit der gewählten Stimme vorbereiten, solange
     * die App offen ist und Netz hat; ohne Datei spricht am Ende die Android-Stimme.
     */
    fun fokusVorbereiten(context: Context) {
        val app = context.applicationContext
        val e = Einstellungen.get(app)
        if (gueltig(datei(app, gewaehlt(e), 0, Fokus.TEXT)) || gueltig(datei(app, edge(e), 0, Fokus.TEXT))) return
        scope.launch {
            mutex.withLock {
                runCatching { bereiteText(app, Fokus.TEXT, 1) }.onFailure { Log.w(TAG, "Fokus-Ansage: ${it.message}") }
            }
        }
    }

    private suspend fun bereiteText(app: Context, t: String, anzahl: Int) {
        val e = Einstellungen.get(app)
        if (t.isBlank()) return
        val s = gewaehlt(e)
        val n = edge(e)
        var gewaehlteAus = false
        for (i in 0 until anzahl) {
            if (gueltig(datei(app, s, i, t)) || gueltig(datei(app, n, i, t))) continue
            if (!gewaehlteAus) {
                try {
                    rendere(app, s, i, t, datei(app, s, i, t), e)
                    continue
                } catch (c: CancellationException) {
                    throw c
                } catch (x: Exception) {
                    if (s.anbieter == TtsProvider.EDGE.id) throw x
                    gewaehlteAus = true
                    Log.w(TAG, "Gewählte Stimme fehlgeschlagen, Edge-Notfallstimme: ${x.message}")
                }
            }
            rendere(app, n, i, t, datei(app, n, i, t), e)
        }
    }

    private suspend fun rendere(app: Context, s: Stimme, variante: Int, text: String, ziel: File, e: Einstellungen) {
        val tmp = File(ziel.parentFile, "${ziel.name}.${UUID.randomUUID()}.tmp")
        val tempo = (s.tempo * FAKTOREN[variante % FAKTOREN.size]).coerceIn(0.5f, 2f)
        try {
            withTimeout(120_000) {
                withContext(Dispatchers.Main) {
                    when (s.anbieter) {
                        TtsProvider.GOOGLE_CLOUD.id -> {
                            val player = GoogleCloudTtsPlayer(app)
                            try {
                                warte { fertig, fehler ->
                                    player.speak(
                                        text = text, apiKey = e.googleKey, voiceName = s.id, speechRate = tempo,
                                        erzwingeDeutsch = s.deutsch, onPlaybackStart = {}, onComplete = fertig, onError = fehler,
                                        onAudioReady = { it.copyTo(tmp, overwrite = true) },
                                    )
                                }
                            } finally {
                                player.shutdown()
                            }
                        }
                        TtsProvider.QWEN_CLONE.id -> {
                            val player = QwenTtsPlayer(app)
                            try {
                                warte { fertig, fehler ->
                                    player.speak(
                                        text = text, rawApiKey = e.qwenKey, rawVoiceId = s.id,
                                        onPlaybackStart = {}, onComplete = fertig, onError = fehler,
                                        onAudioReady = { it.copyTo(tmp, overwrite = true) },
                                    )
                                }
                            } finally {
                                player.shutdown()
                            }
                        }
                        else -> {
                            val player = EdgeTtsPlayer(app)
                            try {
                                warte { fertig, fehler ->
                                    player.speak(
                                        text = text, voice = s.id, speechRate = tempo, erzwingeDeutsch = s.deutsch,
                                        onPlaybackStart = {}, onComplete = fertig, onError = fehler,
                                        onAudioReady = { it.copyTo(tmp, overwrite = true) },
                                    )
                                }
                            } finally {
                                player.shutdown()
                            }
                        }
                    }
                }
            }
            check(gueltig(tmp)) { "Der Sprachdienst hat kein Audio geliefert." }
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(tmp.absolutePath)
                check(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "Der Sprachdienst hat keine gültige Audiodatei geliefert." }
            } finally {
                runCatching { mmr.release() }
            }
            check(tmp.renameTo(ziel)) { "Die Sprachdatei konnte nicht gespeichert werden." }
        } finally {
            tmp.delete()
        }
    }

    /** Wartet auf das Ende eines Player-Auftrags (fertig oder Fehler). */
    private suspend fun warte(start: (fertig: () -> Unit, fehler: (Exception) -> Unit) -> Unit) =
        suspendCancellableCoroutine<Unit> { k ->
            start({ if (k.isActive) k.resume(Unit) }, { if (k.isActive) k.resumeWithException(it) })
        }

    /** Löscht Fassungen, die keine kommende Erinnerung mehr braucht (mit Schonfrist für „wieder offen“). */
    private fun aufraeumen(app: Context, offen: List<Aufgabe>) {
        val e = Einstellungen.get(app)
        val s = gewaehlt(e)
        val n = edge(e)
        val behalten = offen.flatMap { a ->
            val t = text(a)
            (0 until ANZAHL).flatMap { i -> listOf(datei(app, s, i, t).name, datei(app, n, i, t).name) }
        }.toSet() + listOf(datei(app, s, 0, Fokus.TEXT).name, datei(app, n, 0, Fokus.TEXT).name)
        val jetzt = System.currentTimeMillis()
        ordner(app).listFiles()?.forEach { f ->
            val alt = jetzt - f.lastModified()
            val weg = if (f.name.endsWith(".tmp")) alt > 60 * 60_000L else f.name !in behalten && alt > 2 * 24 * 60 * 60_000L
            if (weg) f.delete()
        }
    }

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }.take(32)
}

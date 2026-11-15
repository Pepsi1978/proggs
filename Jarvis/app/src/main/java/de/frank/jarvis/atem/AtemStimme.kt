package de.frank.jarvis.atem

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.speech.Synthese
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Stimme der Atemübungen: dieselbe Vorlesestimme wie in Jarvis, aber vorab erzeugt und dauerhaft in files/atem/stimme abgelegt.
 * So kommt jedes Stichwort pünktlich ohne Netz-Wartezeit, und die Satzlänge ist vor dem Start bekannt.
 * Der Schlüssel enthält Anbieter, Stimmen und Tempo: Ein Stimmwechsel in Jarvis erzeugt die Clips neu.
 */
object AtemStimme {
    data class Clip(val datei: File, val ms: Long)

    const val EINATMEN = "Einatmen."
    const val HALTEN = "Halten."
    const val AUSATMEN = "Ausatmen."
    const val INTRO = "Mach es dir bequem. Setz oder leg dich entspannt hin, lass die Schultern sinken und atme ganz natürlich. Gleich geht es los."
    const val OUTRO = "Sehr gut gemacht. Atme jetzt wieder ganz natürlich und spür noch einen Moment nach, wie ruhig dein Körper geworden ist."

    private const val TAG = "AtemStimme"
    private val sperre = Mutex()
    private val dauern = HashMap<String, Long>()

    private fun einstellungen(context: Context) = SecureSettings.get(context.applicationContext)

    private fun stimmKennung(s: SecureSettings): String {
        val a = s.ttsProvider
        return listOf(a, s.ttsTempo(a), s.edgeTtsVoice, s.googleTtsVoice, s.azureTtsVoice, s.geminiFlashTtsVoice, s.geminiLiteTtsVoice, s.qwenStandardVoice, s.qwenTtsVoiceId).joinToString("|")
    }

    private fun schluessel(text: String, kennung: String): String =
        MessageDigest.getInstance("SHA-1").digest("$kennung#$text".toByteArray()).joinToString("") { "%02x".format(it) }

    private fun ordner(context: Context) = File(context.filesDir, "atem/stimme").apply { mkdirs() }

    /** Schon erzeugter Clip, ohne Netz; null, wenn er noch fehlt. */
    fun vorhanden(context: Context, text: String): Clip? {
        val s = einstellungen(context)
        val k = schluessel(text, stimmKennung(s))
        val d = ordner(context).listFiles()?.firstOrNull { it.name.startsWith("$k.") && !it.name.endsWith(".tmp") && it.length() > 0 } ?: return null
        return Clip(d, dauer(d))
    }

    /** Holt oder erzeugt den Clip. Scheitert die Stimme (offline, Kontingent), kommt null: Die Übung läuft dann ohne. */
    suspend fun hole(context: Context, text: String): Clip? {
        val app = context.applicationContext
        withContext(Dispatchers.IO) { vorhanden(app, text) }?.let { return it }
        return sperre.withLock {
            withContext(Dispatchers.IO) { vorhanden(app, text) } ?: erzeuge(app, text)
        }
    }

    /**
     * Frisch erzeugte Fassung eines Satzes, nicht aus dem Zwischenspeicher: Die Stimme betont ihn so jedes Mal ein wenig anders.
     * Liegt unter files/atem/frisch und wird nach dem Abspielen gelöscht.
     */
    suspend fun frisch(context: Context, text: String): Clip? {
        val app = context.applicationContext
        val dir = File(app.filesDir, "atem/frisch").apply { mkdirs() }
        return erzeuge(app, text) { roh -> File(dir, java.util.UUID.randomUUID().toString() + "." + roh.extension.ifBlank { "mp3" }) }
    }

    fun frischAufraeumen(context: Context) {
        runCatching { File(context.applicationContext.filesDir, "atem/frisch").listFiles()?.forEach(File::delete) }
    }

    private suspend fun erzeuge(app: Context, text: String, zielFuer: ((File) -> File)? = null): Clip? {
        val s = einstellungen(app)
        // Zwei Versuche: Räumt der Vorleser seinen Zwischenordner genau zwischen Erzeugen und Kopieren leer, fehlt die Datei.
        repeat(2) { versuch ->
            val ergebnis = runCatching {
                val roh = Synthese(app, s).synthetisiere(text)
                withContext(Dispatchers.IO) {
                    val ziel = zielFuer?.invoke(roh) ?: File(ordner(app), schluessel(text, stimmKennung(s)) + "." + roh.extension.ifBlank { "mp3" })
                    // Erst unter Zwischennamen, dann umbenennen: Ein abgebrochenes Kopieren hinterlässt nie einen halben Clip.
                    val tmp = File(ziel.parentFile, ziel.name + ".tmp")
                    roh.copyTo(tmp, overwrite = true); roh.delete()
                    if (!tmp.renameTo(ziel)) { ziel.delete(); check(tmp.renameTo(ziel)) { "Clip nicht ablegbar" } }
                    Clip(ziel, dauer(ziel))
                }
            }
            ergebnis.getOrNull()?.let { return it }
            val fehler = ergebnis.exceptionOrNull()
            if (fehler is kotlinx.coroutines.CancellationException) throw fehler
            Log.w(TAG, "Stimme für „$text“ nicht erzeugt (Versuch ${versuch + 1})", fehler)
            if (fehler !is java.io.FileNotFoundException && fehler !is java.nio.file.NoSuchFileException) return null
        }
        return null
    }

    private fun dauer(d: File): Long = synchronized(dauern) { dauern[d.path] } ?: runCatching {
        MediaMetadataRetriever().run {
            try { setDataSource(d.path); extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L } finally { release() }
        }
    }.getOrDefault(0L).also { ms -> if (ms > 0) synchronized(dauern) { dauern[d.path] = ms } }

    /** Grobe Schätzung vor der Erzeugung: gut 14 Zeichen je Sekunde, im eingestellten Tempo. */
    fun schaetzeMs(context: Context, text: String): Long {
        val s = einstellungen(context)
        val tempo = s.ttsTempo(s.ttsProvider).coerceIn(0.5f, 2f)
        return (text.length / 14.5f / tempo * 1000f + 350f).toLong()
    }

    /**
     * Texte, die vor dem Start bereitliegen müssen: nur die kurzen Stichworte und die Begrüßung (einmal je Stimme erzeugt,
     * danach aus dem Speicher). Die Sätze der Übung entstehen erst während der Übung, jeweils eine Runde vor ihrem Einsatz,
     * und der Abschluss im Hintergrund: So beginnt auch eine 60-Minuten-Übung sofort.
     * Die Stichworte immer, damit das Häkchen „Vorlesen“ auch mitten in einer Übung sofort greift.
     */
    fun texte(p: AtemProfil): List<String> = buildList {
        add(EINATMEN); if (p.halt1 > 0 || p.halt2 > 0) add(HALTEN); add(AUSATMEN)
        if (p.intro) add(INTRO)
    }.distinct()

    /** Vorab-Texte, die das Profil mit seinen Schaltern wirklich spricht: Nur deren Fehlen ist einen Hinweis wert. */
    fun noetig(p: AtemProfil): List<String> =
        if (p.vorlesen) texte(p) else emptyList()

    /** Erzeugt alles vorab, z. B. gleich nach dem Speichern; meldet den Fortschritt (fertig, gesamt). */
    suspend fun vorbereiten(context: Context, p: AtemProfil, fortschritt: (Int, Int) -> Unit = { _, _ -> }): Map<String, Clip> {
        val t = texte(p)
        val ergebnis = HashMap<String, Clip>()
        t.forEachIndexed { i, text ->
            fortschritt(i, t.size)
            hole(context, text)?.let { ergebnis[text] = it }
        }
        fortschritt(t.size, t.size)
        return ergebnis
    }
}

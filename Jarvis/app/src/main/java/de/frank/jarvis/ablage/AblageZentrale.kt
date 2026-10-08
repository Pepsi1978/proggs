package de.frank.jarvis.ablage

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.auth.AuthErrorKind
import de.frank.jarvis.auth.CodexAuthException
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.dienst.JarvisDienst
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Markiert Werkzeugaufrufe aus Jarvis' eigener Agentenschleife (statt aus ChatGPT über den Server).
 * Dort darf ein Werkzeug auf lange Arbeit warten; [eintragTitel] ist der Ablage-Eintrag des laufenden Agenten,
 * in den erzeugte Dateien ohne eigenen Titel gehören.
 */
class AgentenKontext(val eintragTitel: String?, val langeWarten: Boolean) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<AgentenKontext>
}

/** Eine Arbeit, die gerade im Hintergrund für die Ablage läuft (Bilderzeugung). Downloads stehen in [Uebertragungen]. */
data class Hintergrundarbeit(val kennung: String, val art: String, val eintragTitel: String, val seit: Long, val fehler: String? = null)

/**
 * Verbindet die Ablage mit Android: ein Speicher je App, Übertragungen, Hintergrundarbeit mit Wachhalten und
 * Benachrichtigung, Bilderzeugung über Codex und Vorschaubilder.
 *
 * Grundsatz: Eine Fertigmeldung gibt es erst, wenn die Datei vollständig in der Ablage liegt. Dauert die Arbeit
 * länger als ein Werkzeugaufruf aus ChatGPT warten darf (der Server bricht nach 75 Sekunden ab), meldet das
 * Werkzeug „läuft noch“ und Jarvis schickt nach dem Speichern eine Benachrichtigung.
 */
object AblageZentrale {
    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val WARTEN_PLUGIN_MS = 40_000L
    private const val WARTEN_CHAT_MS = 90_000L
    private const val WARTEN_AGENT_MS = 10 * 60_000L

    @Volatile private var uebertragungenInstanz: Uebertragungen? = null
    private val laufend = HashMap<String, Deferred<String>>()
    private val nachzumelden: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val _arbeiten = MutableStateFlow<List<Hintergrundarbeit>>(emptyList())
    val arbeiten: StateFlow<List<Hintergrundarbeit>> = _arbeiten.asStateFlow()

    fun speicher(context: Context): AblageSpeicher = AblageSpeicher.fuer(context.applicationContext.filesDir)

    fun uebertragungen(context: Context): Uebertragungen = uebertragungenInstanz ?: synchronized(this) {
        uebertragungenInstanz ?: Uebertragungen(speicher(context), File(context.applicationContext.filesDir, "ablage-uebertragungen.json")).also { uebertragungenInstanz = it }
    }

    /** Beim Start des Dienstes: Reste aufräumen und unterbrochene Übertragungen einmal fortsetzen (temporäre Links verfallen). */
    fun beimStart(context: Context) {
        val app = context.applicationContext
        bereich.launch {
            val u = uebertragungen(app)
            runCatching { speicher(app).aufraeumen(u.offeneTeile()) }
            u.liste.value.filter { it.zustand == UebertragungsZustand.FEHLER && it.fehler?.startsWith("Unterbrochen") == true }.forEach { offen ->
                runCatching { starteDownload(app, offen.id, offen.name, offen.eintragTitel) }
            }
        }
    }

    /** Wie lange ein Werkzeug auf Hintergrundarbeit wartet, je nachdem wer es aufruft. */
    suspend fun wartezeit(): Long = currentCoroutineContext()[AgentenKontext]?.let { if (it.langeWarten) WARTEN_AGENT_MS else WARTEN_CHAT_MS } ?: WARTEN_PLUGIN_MS

    /** Der Eintrag des laufenden Agenten, falls der Aufruf aus einem Agentenlauf kommt. */
    suspend fun agentenEintrag(): String? = currentCoroutineContext()[AgentenKontext]?.eintragTitel

    /**
     * Startet [arbeit] im Hintergrund (oder hängt sich an eine laufende mit gleicher [kennung]) und wartet bis zu
     * [warteMs]. Rückgabe: der Fertigtext oder null, wenn sie noch läuft. Was danach fertig wird, meldet sich per
     * Benachrichtigung; Fehler werden geworfen bzw. gemeldet.
     */
    suspend fun imHintergrund(context: Context, kennung: String, art: String, eintragTitel: String, warteMs: Long, arbeit: suspend () -> String): String? {
        val app = context.applicationContext
        val job = synchronized(laufend) {
            laufend[kennung] ?: bereich.async(start = CoroutineStart.LAZY) {
                _arbeiten.value = _arbeiten.value.filter { it.kennung != kennung } + Hintergrundarbeit(kennung, art, eintragTitel, System.currentTimeMillis())
                val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:ablage")
                runCatching { wach.acquire(30 * 60_000L) }
                try {
                    arbeit().also { text ->
                        _arbeiten.value = _arbeiten.value.filter { it.kennung != kennung }
                        Protokoll.melde(Quelle.JARVIS, art, text)
                    }
                } catch (e: Exception) {
                    val text = fehlerText(e)
                    _arbeiten.value = _arbeiten.value.map { if (it.kennung == kennung) it.copy(fehler = text) else it }
                    Protokoll.melde(Quelle.JARVIS, art, text, ok = false)
                    throw AblageFehler(text)
                } finally {
                    runCatching { if (wach.isHeld) wach.release() }
                    synchronized(laufend) { laufend.remove(kennung) }
                }
            }.also { neu -> laufend[kennung] = neu; neu.start() }
        }
        val ergebnis = withTimeoutOrNull(warteMs) { job.await() }
        if (ergebnis == null && nachzumelden.add(kennung)) {
            // Der Aufrufer bekommt „läuft noch“; das Ergebnis meldet eine Benachrichtigung, sobald es feststeht.
            bereich.launch {
                val r = runCatching { job.await() }
                nachzumelden.remove(kennung)
                r.onSuccess { benachrichtige(app, "Jarvis: in der Ablage", it) }
                    .onFailure { benachrichtige(app, "Jarvis: $art fehlgeschlagen", fehlerText(it)) }
            }
        }
        return ergebnis
    }

    /** Startet bzw. wiederholt eine Übertragung im Hintergrund. Rückgabe wie [imHintergrund]. */
    suspend fun starteDownload(context: Context, id: String, name: String, eintragTitel: String, warteMs: Long = 0): String? =
        imHintergrund(context, "download:$id", "Datei übernehmen", eintragTitel, warteMs) {
            val e = uebertragungen(context).ausfuehren(id)
            nachDemSpeichern(context, e)
            meldung(e)
        }

    /** Fehlgeschlagene Bilderzeugung aus der Liste nehmen. */
    fun verwerfeArbeit(kennung: String) { _arbeiten.value = _arbeiten.value.filter { it.kennung != kennung } }

    /** Vorschaubild anlegen (Bild, Video, PDF), damit die Liste es sofort zeigt. */
    fun nachDemSpeichern(context: Context, u: Uebernahme) {
        val s = speicher(context)
        if (u.anhang.art in setOf(Art.BILD, Art.ANIMATION, Art.VIDEO, Art.PDF)) {
            runCatching { Medien.vorschaubild(s.datei(u.anhang), u.anhang.art, s.vorschauDatei(u.anhang))?.let { s.setzeVorschau(u.eintrag.id, u.anhang.id, it) } }
        }
    }

    fun meldung(u: Uebernahme): String =
        (if (u.neu) "Gespeichert" else "War schon gespeichert (keine Kopie angelegt)") +
            ": „${u.anhang.originalName}“ (${u.anhang.art.anzeige}, ${Dateityp.groesse(u.anhang.groesse)}) im Ablage-Eintrag „${u.eintrag.titel}“."

    // ---- Bilderzeugung ----

    enum class Bildformat(val groesse: String?, val hinweis: String) {
        QUADRAT("1024x1024", "Quadratisches Format."),
        HOCH("1024x1536", "Hochformat."),
        QUER("1536x1024", "Querformat."),
        DIN_A4("1024x1536", "Hochformat im Seitenverhältnis von DIN A4 (1 : 1,41), als druckfähige Seite gestaltet mit Rand."),
        DIN_A4_QUER("1536x1024", "Querformat im Seitenverhältnis von DIN A4 quer (1,41 : 1), als druckfähige Seite gestaltet mit Rand."),
    }

    /**
     * Erzeugt ein Bild über das Bildwerkzeug von Codex (wie in News Kompass) und legt es als PNG ab, auf Wunsch
     * zusätzlich als DIN-A4-PDF. Wirft einen verständlichen Fehler, wenn kein Bild kam. Rückgabe: Fertigtext.
     */
    suspend fun erzeugeBild(context: Context, beschreibung: String, eintragTitel: String, format: Bildformat, hohe: Boolean, auchPdf: Boolean, kennung: String): String {
        val app = context.applicationContext
        val auth = CodexAuthManager(app)
        if (!auth.isConnected) throw AblageFehler("Jarvis ist nicht mit ChatGPT verbunden; ohne Anmeldung gibt es kein Bildwerkzeug. Bitte in Jarvis unter Einstellungen anmelden.")
        val prompt = beschreibung.trim() + "\n\n" + format.hinweis +
            " Alle Texte im Bild auf Deutsch mit korrekter Rechtschreibung und echten Umlauten, groß und klar lesbar. Keine Wasserzeichen."
        val bilder = auth.generateImages(prompt, Einstellungen.get(app).modell, format.groesse, if (hohe) "high" else null)
        if (bilder.isEmpty()) throw AblageFehler("Das Bildwerkzeug hat kein Bild geliefert. Es wurde nichts gespeichert.")
        val s = speicher(app)
        val name = AblageSpeicher.dateiname(eintragTitel).replace(' ', '-').take(60)
        val teile = mutableListOf<String>()
        bilder.forEachIndexed { i, bytes ->
            val u = s.uebernimmBytes(eintragTitel, bytes, if (bilder.size == 1) "$name.png" else "$name-${i + 1}.png", "image/png", "Bildwerkzeug (Codex)", beschreibung.take(500), "$kennung-$i")
            nachDemSpeichern(app, u)
            teile += meldung(u)
            val masse = Medien.bildMasse(s.datei(u.anhang))
            masse?.let { (b, h) -> teile += "Bildgröße: $b × $h Pixel." }
            if (auchPdf) {
                val teil = s.neueTeilDatei()
                try {
                    Medien.bildAlsA4Pdf(s.datei(u.anhang), teil)
                    val p = s.uebernimm(eintragTitel, teil, "$name.pdf", "application/pdf", "Jarvis (PDF aus Bild)", "DIN-A4-PDF mit dem Bild", "$kennung-$i-pdf")
                    nachDemSpeichern(app, p)
                    teile += meldung(p)
                } catch (e: Exception) {
                    teile += "Das zusätzliche PDF ließ sich nicht erzeugen (${e.message}); das PNG ist gespeichert."
                } finally { teil.delete() }
            }
        }
        return teile.joinToString(" ")
    }

    fun fehlerText(e: Throwable): String = when {
        e is AblageFehler -> e.message.orEmpty()
        e is CodexAuthException && e.kind == AuthErrorKind.QUOTA -> "Das ChatGPT-/Codex-Kontingent ist ausgeschöpft; es wurde kein Bild erzeugt."
        e is CodexAuthException && e.kind == AuthErrorKind.REAUTH -> "Die ChatGPT-Anmeldung von Jarvis ist abgelaufen; bitte in Jarvis neu anmelden. Es wurde nichts erzeugt."
        e is CodexAuthException && e.message.orEmpty().contains("image_generation", ignoreCase = true) ->
            "Das Bildwerkzeug ist für dieses Konto oder Modell nicht verfügbar (${e.message?.take(160)}). Es wurde nichts erzeugt."
        e is Medien.PdfFehler -> e.message.orEmpty()
        else -> (e.message ?: e.javaClass.simpleName).take(240)
    }

    fun benachrichtige(context: Context, titel: String, text: String) {
        runCatching {
            JarvisDienst.kanalAnlegen(context)
            context.getSystemService(NotificationManager::class.java).notify(
                (System.currentTimeMillis() % 100_000).toInt() + 200_000,
                NotificationCompat.Builder(context, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle(titel)
                    .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                    .build(),
            )
        }
    }
}

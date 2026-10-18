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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Markiert Werkzeugaufrufe aus Jarvis' eigener Agentenschleife (statt aus ChatGPT über den Server).
 * Dort darf ein Werkzeug auf lange Arbeit warten; [eintragTitel] ist der Ablage-Eintrag des laufenden Agenten,
 * in den erzeugte Dateien ohne eigenen Titel gehören.
 */
class AgentenKontext(val eintragTitel: String?, val langeWarten: Boolean, val dauerhaft: Boolean = true) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<AgentenKontext>
}

/**
 * Liegt um den Lauf eines Agenten: [erlaubt] = Frank hat für diesen Auftrag ausdrücklich ein PDF verlangt. Ohne diesen Wunsch
 * erzeugen bild_erzeugen (auch_pdf) und ablage_schreiben (als_pdf) im Agenten kein PDF, auch wenn das Modell es versucht.
 * Fehlt das Element (ChatGPT, Jarvis-Chat), entscheidet wie bisher der Aufruf selbst.
 */
class PdfWunsch(val erlaubt: Boolean) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<PdfWunsch> {
        suspend fun zulaessig(): Boolean = currentCoroutineContext()[Key]?.erlaubt != false
    }
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
    // Ein Agent wartet höchstens so lange auf ein Bild oder einen Download; danach bekommt er „läuft noch“ und kann weiterarbeiten.
    private const val WARTEN_AGENT_MS = 4 * 60_000L

    @Volatile private var uebertragungenInstanz: Uebertragungen? = null
    private val laufend = HashMap<String, Deferred<String>>()
    private val nachzumelden: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val fuerPlugin = LinkedHashMap<String, String>()
    private val direktGeliefert = HashSet<String>()
    private val _arbeiten = MutableStateFlow<List<Hintergrundarbeit>>(emptyList())
    val arbeiten: StateFlow<List<Hintergrundarbeit>> = _arbeiten.asStateFlow()

    fun speicher(context: Context): AblageSpeicher = AblageSpeicher.fuer(context.applicationContext.filesDir)

    /**
     * Wohin ein Agentenlauf seine Dateien legt: in die Ablage nur, wenn Frank sie ausdrücklich verlangt hat (agent_starten
     * mit ablegen), sonst in den vorläufigen Speicher ([Vorlaeufig]). Außerhalb eines Agentenlaufs die Ablage.
     */
    suspend fun agentenSpeicher(context: Context): AblageSpeicher =
        currentCoroutineContext()[AgentenKontext]?.takeIf { it.eintragTitel != null && !it.dauerhaft }?.let { Vorlaeufig.speicher(context) } ?: speicher(context)

    /** Läuft gerade ein Agent, der NICHT in die Ablage speichern soll? */
    suspend fun agentOhneAblage(): Boolean = currentCoroutineContext()[AgentenKontext]?.let { it.eintragTitel != null && !it.dauerhaft } == true

    fun istVorlaeufig(context: Context, s: AblageSpeicher): Boolean = s === Vorlaeufig.speicher(context)

    fun uebertragungen(context: Context): Uebertragungen = uebertragungenInstanz ?: synchronized(this) {
        uebertragungenInstanz ?: Uebertragungen(speicher(context), File(context.applicationContext.filesDir, "ablage-uebertragungen.json")).also { uebertragungenInstanz = it }
    }

    /** Beim Start des Dienstes: Reste aufräumen und unterbrochene Übertragungen einmal fortsetzen (temporäre Links verfallen). */
    fun beimStart(context: Context) {
        val app = context.applicationContext
        bereich.launch {
            val u = uebertragungen(app)
            runCatching { speicher(app).aufraeumen(u.offeneTeile()) }
            // Gerenderte PDF-Seiten nicht endlos sammeln (je Seite mehrere MB im Zwischenspeicher).
            runCatching { Medien.pdfSeitenAufraeumen(File(app.cacheDir, "pdf-seiten")) }
            // Vorläufige Chat-Ergebnisse, die Frank nicht in die Ablage übernommen hat, nach 30 Tagen entfernen.
            runCatching { Vorlaeufig.aufraeumen(app) }
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
    suspend fun imHintergrund(context: Context, kennung: String, art: String, eintragTitel: String, warteMs: Long, fertigTitel: String = "Jarvis: in der Ablage", arbeit: suspend () -> String): String? {
        val app = context.applicationContext
        val job = synchronized(laufend) {
            laufend[kennung] ?: bereich.async(start = CoroutineStart.LAZY) {
                _arbeiten.update { l -> l.filter { it.kennung != kennung } + Hintergrundarbeit(kennung, art, eintragTitel, System.currentTimeMillis()) }
                val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:ablage")
                runCatching { wach.acquire(30 * 60_000L) }
                try {
                    arbeit().also { text ->
                        _arbeiten.update { l -> l.filter { it.kennung != kennung } }
                        Protokoll.melde(Quelle.JARVIS, art, text)
                    }
                } catch (e: Exception) {
                    val text = fehlerText(e)
                    _arbeiten.update { l -> l.map { if (it.kennung == kennung) it.copy(fehler = text) else it } }
                    Protokoll.melde(Quelle.JARVIS, art, text, ok = false)
                    throw AblageFehler(text)
                } finally {
                    runCatching { if (wach.isHeld) wach.release() }
                    synchronized(laufend) { laufend.remove(kennung) }
                }
            }.also { neu -> laufend[kennung] = neu; neu.start() }
        }
        val ausPlugin = currentCoroutineContext()[AgentenKontext] == null
        val ergebnis = withTimeoutOrNull(warteMs) { job.await() }
        if (ergebnis != null) {
            // Der Aufrufer hat das Ergebnis selbst in der Hand: nicht noch einmal nachreichen.
            synchronized(fuerPlugin) { fuerPlugin.remove(kennung); if (kennung in nachzumelden) direktGeliefert += kennung }
        } else if (nachzumelden.add(kennung)) {
            // Der Aufrufer bekommt „läuft noch“; das Ergebnis meldet eine Benachrichtigung, sobald es feststeht.
            bereich.launch {
                val r = runCatching { job.await() }
                val text = r.fold({ it }, { "$art fehlgeschlagen („$eintragTitel“): " + fehlerText(it) })
                synchronized(fuerPlugin) {
                    nachzumelden.remove(kennung)
                    if (!direktGeliefert.remove(kennung) && ausPlugin) fuerPlugin[kennung] = text
                }
                r.onSuccess { benachrichtige(app, fertigTitel, it) }
                    .onFailure { benachrichtige(app, "Jarvis: $art fehlgeschlagen", fehlerText(it)) }
            }
        }
        return ergebnis
    }

    /**
     * Was fertig wurde, nachdem das Plugin (ChatGPT) nur „läuft noch“ bekam. Das Handy kann ChatGPT nicht von sich aus
     * ansprechen; die Meldung hängt deshalb am nächsten Werkzeug-Ergebnis. Jede Meldung wird genau einmal geliefert.
     */
    fun fertigFuerPlugin(): List<String> = synchronized(fuerPlugin) { fuerPlugin.values.toList().also { fuerPlugin.clear() } }

    /** Startet bzw. wiederholt eine Übertragung im Hintergrund. Rückgabe wie [imHintergrund]. */
    suspend fun starteDownload(context: Context, id: String, name: String, eintragTitel: String, warteMs: Long = 0): String? =
        imHintergrund(context, "download:$id", "Datei übernehmen", eintragTitel, warteMs) {
            val e = uebertragungen(context).ausfuehren(id)
            nachDemSpeichern(context, e)
            meldung(e)
        }

    /** Fehlgeschlagene Bilderzeugung aus der Liste nehmen. */
    fun verwerfeArbeit(kennung: String) = _arbeiten.update { l -> l.filter { it.kennung != kennung } }

    /** Vorschaubild anlegen (Bild, Video, PDF), damit die Liste es sofort zeigt. */
    fun nachDemSpeichern(context: Context, u: Uebernahme) {
        val s = speicher(context)
        if (u.anhang.art in setOf(Art.BILD, Art.ANIMATION, Art.VIDEO, Art.PDF)) {
            runCatching { Medien.vorschaubild(s.datei(u.anhang), u.anhang.art, s.vorschauDatei(u.anhang))?.let { s.setzeVorschau(u.eintrag.id, u.anhang.id, it) } }
        }
    }

    fun meldung(u: Uebernahme, vorlaeufig: Boolean = false): String =
        if (vorlaeufig) (if (u.neu) "Erzeugt" else "War schon erzeugt (keine Kopie angelegt)") +
            ": „${u.anhang.originalName}“ (${u.anhang.art.anzeige}, ${Dateityp.groesse(u.anhang.groesse)}), VORLÄUFIG zum Ergebnis „${u.eintrag.titel}“ – NICHT in der Ablage."
        else (if (u.neu) "Gespeichert" else "War schon gespeichert (keine Kopie angelegt)") +
            ": „${u.anhang.originalName}“ (${u.anhang.art.anzeige}, ${Dateityp.groesse(u.anhang.groesse)}) im Ablage-Eintrag „${u.eintrag.titel}“."

    // ---- Bilderzeugung ----

    /** Herkunft des PDFs, das [erzeugeBild] aus einem Bild baut; daran erkennt der Agent, dass kein Bericht-PDF mehr nötig ist. */
    const val HERKUNFT_BILD_PDF = "Jarvis (PDF aus Bild)"

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
    suspend fun erzeugeBild(
        context: Context, beschreibung: String, eintragTitel: String, format: Bildformat, hohe: Boolean, auchPdf: Boolean, kennung: String,
        s: AblageSpeicher = speicher(context), neueBilder: (List<File>) -> Unit = {},
    ): String {
        val app = context.applicationContext
        val vorlaeufig = istVorlaeufig(app, s)
        val vorhanden = s.findeGenau(eintragTitel)
        // Wiederholung nach dem Fertigwerden (der laufende Auftrag ist schon weg): kein zweites Motiv erzeugen, das gespeicherte melden
        // und ein fehlendes PDF desselben Bildes nachreichen.
        val schon = vorhanden?.anhaenge.orEmpty().filter { it.art == Art.BILD && it.kennung?.startsWith("$kennung-") == true }
        if (schon.isNotEmpty()) {
            val teile = mutableListOf((if (vorlaeufig) "War schon erzeugt (kein zweites Bild), vorläufig zu „${vorhanden!!.titel}“: " else "War schon gespeichert (kein zweites Bild erzeugt) im Ablage-Eintrag „${vorhanden!!.titel}“: ") +
                schon.joinToString { "„${it.originalName}“" } + ".")
            if (auchPdf) schon.filter { b -> vorhanden.anhaenge.none { it.kennung == "${b.kennung}-pdf" } }.forEach { b ->
                teile += bildPdfAblegen(app, s, eintragTitel, b, b.originalName.substringBeforeLast('.'), "${b.kennung}-pdf")
            }
            teile += "Für eine ausdrücklich gewünschte Variante die Beschreibung ändern."
            return teile.joinToString(" ")
        }
        val auth = CodexAuthManager(app)
        if (!auth.isConnected) throw AblageFehler("Jarvis ist nicht mit ChatGPT verbunden; ohne Anmeldung gibt es kein Bildwerkzeug. Bitte in Jarvis unter Einstellungen anmelden.")
        val prompt = beschreibung.trim() + "\n\n" + format.hinweis +
            " Alle Texte im Bild auf Deutsch mit korrekter Rechtschreibung und echten Umlauten, groß und klar lesbar. Keine Wasserzeichen."
        val bilder = auth.generateImages(prompt, Einstellungen.get(app).modell, format.groesse, if (hohe) "high" else null)
        if (bilder.isEmpty()) throw AblageFehler("Das Bildwerkzeug hat kein Bild geliefert. Es wurde nichts gespeichert.")
        val basis = AblageSpeicher.dateiname(eintragTitel).replace(' ', '-').take(60)
        // Jedes Bild eines Eintrags bekommt einen eigenen Namen, damit ein Bericht es mit ![…](name.png) eindeutig einbinden kann.
        val vergeben = vorhanden?.anhaenge?.map { it.originalName.lowercase() }.orEmpty().toMutableSet()
        val teile = mutableListOf<String>()
        val neu = mutableListOf<File>()
        bilder.forEachIndexed { i, bytes ->
            var n = 1
            var name = basis
            while ("$name.png".lowercase() in vergeben) { n++; name = "$basis-$n" }
            vergeben += "$name.png".lowercase()
            val u = s.uebernimmBytes(eintragTitel, bytes, "$name.png", "image/png", "Bildwerkzeug (Codex)", beschreibung.take(500), "$kennung-$i")
            nachDemSpeichern(app, u)
            teile += meldung(u, vorlaeufig)
            if (u.neu) neu += s.datei(u.anhang)
            val masse = Medien.bildMasse(s.datei(u.anhang))
            masse?.let { (b, h) -> teile += "Bildgröße: $b × $h Pixel." }
            if (auchPdf) teile += bildPdfAblegen(app, s, eintragTitel, u.anhang, name, "$kennung-$i-pdf")
        }
        if (neu.isNotEmpty()) runCatching { neueBilder(neu) }
        return teile.joinToString(" ")
    }

    /** Legt [bild] als einseitiges DIN-A4-PDF in den Eintrag. Rückgabe: Meldung; ein Fehler lässt das Bild unberührt. */
    private fun bildPdfAblegen(app: Context, s: AblageSpeicher, eintragTitel: String, bild: Anhang, name: String, kennung: String): String {
        val teil = s.neueTeilDatei()
        return try {
            Medien.bildAlsA4Pdf(s.datei(bild), teil)
            val p = s.uebernimm(eintragTitel, teil, "$name.pdf", "application/pdf", HERKUNFT_BILD_PDF, "DIN-A4-PDF mit dem Bild", kennung)
            nachDemSpeichern(app, p)
            meldung(p, istVorlaeufig(app, s))
        } catch (e: Exception) {
            "Das zusätzliche PDF ließ sich nicht erzeugen (${e.message}); das PNG ist gespeichert."
        } finally { teil.delete() }
    }

    /**
     * Legt [text] als DIN-A4-PDF in den Eintrag [eintragTitel] und bindet dessen Bilder ein (siehe [Medien.textAlsPdf]).
     * Rückgabe: die Meldung „Gespeichert …“; wirft bei Fehlern.
     */
    fun textAlsPdfAblegen(context: Context, eintragTitel: String, text: String, s: AblageSpeicher = speicher(context), untertitel: String? = null): String {
        val bilder = bilderVon(s, eintragTitel)
        val teil = s.neueTeilDatei()
        try {
            Medien.textAlsPdf(eintragTitel, text, teil, bilder, untertitel)
            val u = s.uebernimm(eintragTitel, teil, AblageSpeicher.dateiname(eintragTitel) + ".pdf", "application/pdf",
                if (bilder.isEmpty()) "Jarvis (PDF aus Text)" else "Jarvis (PDF aus Text mit ${bilder.size} Bild(ern))")
            nachDemSpeichern(context, u)
            return meldung(u, istVorlaeufig(context, s)) + if (bilder.isEmpty()) "" else " Darin ${bilder.size} Bild(er)."
        } finally {
            teil.delete()
        }
    }

    /** Die Bilder eines Eintrags für das PDF: Dateiname klein geschrieben → Datei. */
    fun bilderVon(s: AblageSpeicher, eintragTitel: String): Map<String, File> = s.findeGenau(eintragTitel)?.anhaenge.orEmpty().filter { it.art == Art.BILD }
        .associate { it.originalName.lowercase() to s.datei(it) }.filterValues { it.isFile }

    /**
     * Setzt [text] probeweise als PDF (Zwischenspeicher, nichts wird abgelegt) mit den Bildern von [eintragTitel] und
     * liefert die Prüfung: Seiten, gefundene und fehlende Abbildungen, Hinweise zum Layout.
     */
    fun pdfEntwurfPruefen(context: Context, eintragTitel: String, text: String, s: AblageSpeicher): PdfPruefung {
        val probe = File(context.cacheDir, "pdf-entwurf-${System.nanoTime()}.pdf")
        try {
            return Medien.textAlsPdf(eintragTitel, text, probe, bilderVon(s, eintragTitel))
        } finally {
            probe.delete()
        }
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
            // Im Gespräch kein Hinweiston (Meldungen.zeige): Er liefe über Jarvis' Stimme bzw. hielte den Vorleser an.
            de.frank.jarvis.meldung.Meldungen.zeige(
                context, de.frank.jarvis.meldung.Meldungsarten.ABLAGE,
                (System.currentTimeMillis() % 100_000).toInt() + 200_000,
                NotificationCompat.Builder(context, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle(titel)
                    .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)),
                titel, "$titel. $text",
            )
        }
    }
}

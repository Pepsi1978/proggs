package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.ablage.AblageSpeicher
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.gedaechtnis.FunktionsStand
import de.frank.jarvis.gedaechtnis.Funktionsdatei
import de.frank.jarvis.gedaechtnis.Versionseintrag
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Die Funktionsübersicht: was Jarvis in der installierten Version tatsächlich kann, knapp nach Bereichen. Sie liegt auf
 * dem Handy (filesDir/funktionen), ist ohne GitHub lesbar und steht bewusst NICHT im Live-Gedächtnis: Jarvis liest sie
 * nur bei Bedarf mit funktionen_lesen. Die Erstbefüllung kommt aus der APK (assets/funktionen.md); danach pflegt Jarvis
 * sie selbst, sobald eine neuere Version installiert ist ([pflege]), und Frank bearbeitet sie in der App.
 */
object Funktionen {
    private const val ORDNER = "funktionen"
    private const val TEXT = "funktionen.md"
    private const val META = "funktionen.json"
    private const val VORHER = "funktionen_vorher.md"
    private const val PFLEGE_MS = 4 * 60_000L

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sperre = Mutex()
    private val schreiben = Any()

    private val _stand = MutableStateFlow<FunktionsStand?>(null)
    /** Der aktuelle Stand für die Oberfläche (null = noch nicht geladen). */
    val stand: StateFlow<FunktionsStand?> = _stand.asStateFlow()
    private val _pflege = MutableStateFlow("")
    /** Was die Pflege gerade tut oder zuletzt ergab, ein Satz für die Oberfläche. */
    val pflegeText: StateFlow<String> = _pflege.asStateFlow()
    val pflegtGerade: Boolean get() = sperre.isLocked
    private val _laeuft = MutableStateFlow(false)
    /** Läuft die Pflege gerade (für die Oberfläche beobachtbar)? */
    val laeuft: StateFlow<Boolean> = _laeuft.asStateFlow()
    @Volatile private var log: List<Versionseintrag>? = null

    private fun ordner(c: Context) = File(c.applicationContext.filesDir, ORDNER)

    val installiertCode: Int get() = BuildConfig.VERSION_CODE
    val installiertName: String get() = BuildConfig.VERSION_NAME

    fun versionslog(c: Context): List<Versionseintrag> = log ?: runCatching {
        Funktionsdatei.versionslog(c.applicationContext.assets.open("versionslog.json").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList()).also { log = it }

    /** Lädt den Stand; fehlt die Datei, kommt die Erstbefüllung aus der APK (einmalig, danach nie wieder überschrieben). */
    fun lade(c: Context): FunktionsStand = synchronized(schreiben) {
        val d = ordner(c)
        val text = File(d, TEXT)
        val geladen = if (text.exists()) {
            val meta = runCatching { JSONObject(File(d, META).readText()) }.getOrNull()
            if (meta != null) FunktionsStand.aus(text.readText(), meta)
            else {
                // Verwaltung fehlt oder ist kaputt (etwa Abbruch zwischen den beiden Schreibvorgängen): Text behalten, als geprüften
                // Stand den der Erstbefüllung annehmen, statt bei 0 alle Versionen erneut einzuarbeiten.
                val (code, name, _) = Funktionsdatei.erstbefuellung(erstbefuellung(c))
                FunktionsStand(text.readText(), 1, code, name, 0L, 0L, emptySet(), emptyList(), "Verwaltung neu angelegt").also {
                    AblageSpeicher.schreibeAtomar(File(d, META), it.metaJson().toString().toByteArray())
                    Logbuch.gedaechtnis("Funktionsübersicht: Verwaltung neu angelegt", "geprüft bis versionCode $code angenommen")
                }
            }
        } else {
            val (code, name, inhalt) = Funktionsdatei.erstbefuellung(erstbefuellung(c))
            val neu = FunktionsStand(inhalt.ifEmpty { "## Funktionen\n\n- (noch leer)" } + "\n", 1, code, name, System.currentTimeMillis(), 0L, emptySet(), emptyList(),
                "Erstbefüllung aus dem Code")
            schreibe(c, neu)
            Logbuch.gedaechtnis("Funktionsübersicht angelegt", "Erstbefüllung, geprüft bis versionCode $code")
            neu
        }
        _stand.value = geladen
        geladen
    }

    private fun erstbefuellung(c: Context): String =
        runCatching { c.applicationContext.assets.open("funktionen.md").bufferedReader().use { it.readText() } }.getOrDefault("")

    private fun schreibe(c: Context, s: FunktionsStand) {
        val d = ordner(c)
        AblageSpeicher.schreibeAtomar(File(d, TEXT), s.text.toByteArray())
        AblageSpeicher.schreibeAtomar(File(d, META), s.metaJson().toString().toByteArray())
        _stand.value = s
    }

    private fun sichere(c: Context, alt: FunktionsStand) = AblageSpeicher.schreibeAtomar(File(ordner(c), VORHER), alt.text.toByteArray())

    fun hatVorfassung(c: Context): Boolean = File(ordner(c), VORHER).exists()

    /**
     * Franks Bearbeitung von Hand. Gelingt nur, wenn seit dem Öffnen niemand (auch nicht die Pflege) gespeichert hat;
     * sonst false und nichts wird überschrieben. Neu geschriebene Zeilen merkt sich Jarvis als Franks eigene.
     */
    fun speichereVonHand(c: Context, text: String, ausRevision: Int): Boolean = synchronized(schreiben) {
        val alt = lade(c)
        if (alt.revision != ausRevision) return false
        val sauber = text.replace("\r\n", "\n").trim() + "\n"
        if (sauber == alt.text) return true
        sichere(c, alt)
        schreibe(c, alt.copy(text = sauber, revision = alt.revision + 1, manuell = System.currentTimeMillis(),
            manuelleZeilen = Funktionsdatei.manuelleZeilenNach(alt.text, sauber, alt.manuelleZeilen), hinweise = emptyList()))
        Logbuch.gedaechtnis("Funktionsübersicht von Hand bearbeitet", "${sauber.length} Zeichen")
        true
    }

    /** Holt die Fassung vor der letzten Änderung zurück (von Hand oder durch die Pflege). */
    fun rueckgaengig(c: Context): Boolean = synchronized(schreiben) {
        val vorher = File(ordner(c), VORHER)
        if (!vorher.exists()) return false
        val alt = lade(c)
        val text = vorher.readText()
        sichere(c, alt)
        // Zurückgeholte Zeilen werden nicht zu Franks eigenen: Was die Pflege geschrieben hatte, darf sie weiter pflegen.
        schreibe(c, alt.copy(text = text, revision = alt.revision + 1, manuell = System.currentTimeMillis(),
            manuelleZeilen = Funktionsdatei.manuelleZeilenNach(text, text, alt.manuelleZeilen)))
        true
    }

    /** Die Übersicht zum Lesen, mit dem stets frisch berechneten Kopf (Stand, Lücken, Grenzen). */
    fun lesen(c: Context, bereich: String = ""): String {
        val s = lade(c)
        val offen = Funktionsdatei.offeneVersionen(versionslog(c), s.geprueftCode, installiertCode)
        val kopf = Funktionsdatei.kopf(s, installiertCode, installiertName, offen, ::zeit)
        if (bereich.isBlank()) return kopf + "\n" + s.text
        val teil = Funktionsdatei.bereiche(s.text, bereich)
            ?: return kopf + "\nKein Bereich passt zu „$bereich“. Vorhandene Bereiche: " + s.text.lines().filter { it.startsWith("## ") }.joinToString(", ") { it.removePrefix("## ") }
        return kopf + "\n" + teil + "\n"
    }

    fun zeit(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.GERMAN))

    // ---------------------------------------------------------------- Selbstpflege

    /** Ist eine neuere Version installiert, als die Übersicht kennt? */
    fun pflegeFaellig(c: Context): Boolean = Funktionsdatei.pflegeFaellig(lade(c).geprueftCode, installiertCode)

    /** Für Dienststart, Tagesauswertung und den Knopf: pflegt im Hintergrund, wenn fällig; kehrt sofort zurück. */
    fun anstossen(c: Context, ausloeser: String) {
        val app = c.applicationContext
        if (pflegtGerade) return
        bereich.launch {
            runCatching { if (pflegeFaellig(app)) pflege(app, ausloeser) }.onFailure { if (it !is CancellationException) Logbuch.fehler("Funktionsübersicht pflegen", it) }
        }
    }

    /** Der Knopf „Jetzt einarbeiten“: läuft in Jarvis' eigenem Bereich weiter, auch wenn Frank den Bildschirm verlässt. */
    fun pflegeVonHand(c: Context) {
        val app = c.applicationContext
        bereich.launch { runCatching { pflege(app, "von Hand") }.onFailure { if (it !is CancellationException) Logbuch.fehler("Funktionsübersicht pflegen", it) } }
    }

    private fun hindernis(app: Context): String? {
        if (de.frank.jarvis.live.LiveGespraech.aktiv) return "ein Live-Gespräch läuft"
        if (!JarvisAgent(app).verbunden) return "Jarvis ist nicht mit ChatGPT angemeldet"
        val netz = runCatching {
            val cm = app.getSystemService(ConnectivityManager::class.java)
            cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }.getOrDefault(true)
        return if (!netz) "kein Netz" else null
    }

    private val ANWEISUNG = """
        Du pflegst die Funktionsübersicht der Android-App Jarvis: eine knappe, nach Bereichen geordnete Liste dessen, was die INSTALLIERTE App wirklich kann.
        Du bekommst die aktuelle Übersicht, die Notizen des Versionslogs der neu installierten Versionen (das ist der Beleg) und als Zusatzinfo fertig gemeldete Programmieraufträge an Jarvis (kein Beleg).
        Antworte NUR mit JSON: {"operationen":[{"aktion":"neu|ersetzen|entfernen","bereich":"Überschrift ohne ##","alt":"die betroffene Zeile wörtlich aus der Übersicht","neu":"neue Zeile","beleg":"Version 1.8.x"}]}
        Regeln:
        - Nur was eine Versionslog-Notiz belegt. Ein Auftrag ohne passende Versionslog-Notiz ändert nichts; nichts Geplantes, nichts Vermutetes.
        - Neue Fähigkeit: neu, im passenden vorhandenen Bereich. Geänderte Fähigkeit: ersetzen (alt = die vorhandene Zeile wörtlich), nie zusätzlich anhängen. Entfernte Fähigkeit: entfernen.
        - Fehlerbehebungen, Feinschliff, Optik und Tempo ohne neue oder geänderte Fähigkeit: keine Operation. Nichts doppelt, keine Versionsnummern in den Zeilen.
        - Zeilen im Stil der Übersicht: ein Satz oder Stichpunkt, höchstens etwa 200 Zeichen, mit Werkzeugnamen in Backticks, wo es sie gibt, wichtige Grenzen knapp dazu.
        - Zeilen unter FRANKS EIGENE ZEILEN hat Frank selbst geschrieben; ändere sie nur, wenn eine Notiz ihnen klar widerspricht.
        - Keine Zugangsdaten, Schlüssel, Server-Adressen oder Pfade zu Geheimnissen.
        - Ist nichts zu tun: {"operationen":[]}
    """.trimIndent()

    /**
     * Arbeitet die neu installierten Versionen ein. Das Modell schlägt nur Operationen vor; [Funktionsdatei.wende]
     * setzt sie verankert um. Hat Frank währenddessen gespeichert, werden die Operationen auf seine Fassung angewandt
     * (eine verschwundene Ankerzeile greift dann nicht). Ohne Anmeldung, Netz oder während eines Live-Gesprächs bleibt
     * es fällig; der Kopf von funktionen_lesen zeigt solange die offenen Versionen.
     */
    suspend fun pflege(c: Context, ausloeser: String): String = withContext(Dispatchers.IO) {
        val app = c.applicationContext
        if (!sperre.tryLock()) return@withContext "Die Pflege läuft schon."
        _laeuft.value = true
        // Eigene Wachsperre: Der Auslöser (Dienst, Tagesauswertung) wartet nicht auf die Pflege.
        val wach = app.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:funktionen")
        runCatching { wach.acquire(PFLEGE_MS + 60_000L) }
        try {
            val s = lade(app)
            val offen = Funktionsdatei.offeneVersionen(versionslog(app), s.geprueftCode, installiertCode)
            if (!Funktionsdatei.pflegeFaellig(s.geprueftCode, installiertCode)) return@withContext melde("Die Übersicht ist auf dem Stand der installierten Version ${installiertName}.")
            hindernis(app)?.let { return@withContext melde("Pflege verschoben: $it. Bis dahin zeigt die Übersicht die noch nicht eingearbeiteten Versionen.") }
            if (offen.isEmpty()) return@withContext melde(abschliessen(app, installiertCode, emptyList(), emptyList(), "Pflege: keine neuen Versionslog-Einträge"))
            _pflege.value = "Jarvis arbeitet ${offen.size} neue Version(en) ein …"
            val auftraege = runCatching { ProgrammierFaehigkeit.fertigeAuftraege(app, "jarvis", s.gepflegt - 86_400_000L) }.getOrDefault(emptyList())
            val eingabe = buildString {
                append("AKTUELLE ÜBERSICHT:\n").append(s.text.take(16_000)).append("\n\n")
                append("NEU INSTALLIERTE VERSIONEN (Versionslog, Beleg):\n")
                if (offen.size > 30) append("(").append(offen.size - 30).append(" ältere Versionen ausgelassen)\n")
                offen.takeLast(30).forEach { append("- ").append(it.name).append(" (").append(it.stand).append("): ").append(it.notiz).append('\n') }
                append("\nFERTIG GEMELDETE PROGRAMMIERAUFTRÄGE AN JARVIS (nur Zusatzinfo, kein Beleg):\n").append(auftraege.joinToString("\n").take(6_000).ifEmpty { "(keine)" })
                if (s.manuelleZeilen.isNotEmpty()) append("\n\nFRANKS EIGENE ZEILEN:\n").append(s.manuelleZeilen.joinToString("\n") { "- $it" }.take(3_000))
            }
            val e = Einstellungen.get(app)
            // Das Live-Gespräch hat Vorrang: Beginnt eines, bricht die Pflege ab und bleibt fällig.
            var live = false
            val roh = withTimeoutOrNull(PFLEGE_MS) {
                coroutineScope {
                    val anfrage = async { JarvisAgent(app).auth.streamChat(ANWEISUNG, listOf(ChatTurn("user", eingabe)), e.modell, e.denkstufe) }
                    while (!anfrage.isCompleted) {
                        if (de.frank.jarvis.live.LiveGespraech.aktiv) { live = true; anfrage.cancel(); break }
                        delay(1_000L)
                    }
                    if (live) null else anfrage.await()
                }
            }
            if (live) return@withContext melde("Pflege abgebrochen: ein Live-Gespräch hat begonnen; der nächste Anlauf holt es nach.")
            if (roh == null) return@withContext melde("Pflege nicht rechtzeitig fertig; der nächste Anlauf holt es nach.")
            val antwort = jsonAus(roh) ?: return@withContext melde("Pflege lieferte kein lesbares Ergebnis; die Übersicht bleibt unverändert.")
            val ops = Funktionsdatei.operationen(antwort)
            melde(abschliessen(app, installiertCode, ops, offen, "Pflege ($ausloeser)"))
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            Logbuch.fehler("Funktionsübersicht pflegen", ex)
            melde("Pflege gescheitert: ${ex.message ?: ex.javaClass.simpleName}")
        } finally {
            runCatching { if (wach.isHeld) wach.release() }
            _laeuft.value = false
            sperre.unlock()
        }
    }

    /** Wendet [ops] auf die neueste Fassung an und hebt den geprüften Stand an. */
    private fun abschliessen(app: Context, bisCode: Int, ops: List<de.frank.jarvis.gedaechtnis.FunktionsOperation>, offen: List<Versionseintrag>, anlass: String): String = synchronized(schreiben) {
        val s = lade(app)
        val r = Funktionsdatei.wende(s.text, ops, s.manuelleZeilen)
        val name = versionslog(app).firstOrNull { it.code == bisCode }?.name ?: installiertName
        val geaendert = r.text != s.text
        if (geaendert) sichere(app, s)
        // Die Revision steigt nur mit dem Text: Eine Pflege ohne Änderung stört Franks offene Bearbeitung nicht.
        schreibe(app, s.copy(text = r.text, revision = if (geaendert) s.revision + 1 else s.revision, geprueftCode = maxOf(s.geprueftCode, bisCode), geprueftName = name, gepflegt = System.currentTimeMillis(),
            hinweise = (s.hinweise + r.hinweise).distinct().takeLast(Funktionsdatei.HINWEISE_HOECHSTENS), herkunft = "selbst gepflegt nach Versionslog"))
        val zeile = "geprüft bis $name: ${r.angewandt.size} Änderung(en)" + (if (r.hinweise.isNotEmpty()) ", ${r.hinweise.size} Hinweis(e) zu deinen Zeilen" else "") +
            (if (offen.isNotEmpty()) " aus ${offen.size} Version(en)" else "")
        Logbuch.gedaechtnis("Funktionsübersicht $anlass", zeile + (if (r.uebersprungen.isNotEmpty()) " · übersprungen: " + r.uebersprungen.joinToString(" | ").take(600) else ""))
        if (r.angewandt.isNotEmpty() || r.hinweise.isNotEmpty())
            Protokoll.melde(Quelle.JARVIS, "Funktionsübersicht gepflegt", (listOf(zeile) + r.angewandt + r.hinweise.map { "Hinweis: $it" }).take(12).joinToString(" · "))
        "Funktionsübersicht $zeile."
    }

    private fun melde(text: String): String { _pflege.value = text; return text }

    private fun jsonAus(roh: String): JSONObject? {
        val t = roh.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val a = t.indexOf('{')
        val z = t.lastIndexOf('}')
        if (a < 0 || z <= a) return null
        return runCatching { JSONObject(t.substring(a, z + 1)) }.getOrNull()
    }
}

/**
 * Das Werkzeug zur Funktionsübersicht: Jarvis, seine Agenten und ChatGPT lesen sie bei Bedarf. Bewusst kein Teil von
 * jarvis_kontext und Live-Gedächtnis.
 */
class FunktionenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "funktionen"
    override val name = "Funktionsübersicht"
    override val beschreibung = "Was Jarvis in der installierten Version kann, knapp nach Bereichen; lokal, ohne GitHub."
    override val hinweise =
        "Bei Fragen, was Jarvis kann, ob eine Funktion fehlt oder was neu dazukommen könnte, zuerst funktionen_lesen (auch wenn GitHub gesperrt ist). " +
            "Was dort fehlt, ist nicht sicher fehlend; Details oder unsicheren Stand zusätzlich mit repo_lesen prüfen."

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "funktionen_lesen",
            titel = "Funktionsübersicht lesen",
            beschreibung = "Jarvis: liest die Funktionsübersicht von Jarvis: was die installierte App tatsächlich kann, knapp nach Bereichen (Werkzeuge, angebundene Apps, Sprache, " +
                "Live-Gespräch, Agenten, Gedächtnis, Programmierübergabe, Oberfläche) mit wichtigen Grenzen. Liegt auf dem Handy und geht auch ohne GitHub. Der Kopf nennt den " +
                "geprüften Versionsstand, die installierte Version und installierte, aber noch nicht eingearbeitete Versionen. Nutze es ZUERST bei „Kann Jarvis …?“, " +
                "„Welche Funktionen hast du?“, „Was fehlt dir noch?“, „Gibt es das schon?“ und bevor du einen Programmierauftrag für eine neue Funktion vorschlägst oder GitHub " +
                "nach vorhandenen Funktionen durchsuchst. Was hier nicht steht, ist nicht nachweislich fehlend; geplante oder nur beauftragte Funktionen stehen nicht drin. " +
                "Für Details oder bei unsicherem Stand zusätzlich repo_lesen.",
            schema = schema(
                "bereich" to text("Optional: nur Bereiche, deren Überschrift dieses Wort enthält (zum Beispiel „Sprache“, „Gedächtnis“, „Programmieren“). Weglassen = die ganze Übersicht."),
            ),
            nurLesen = true,
        ) { a -> withContext(Dispatchers.IO) { Ergebnis(Funktionen.lesen(context, a.optString("bereich"))) } },
    )
}

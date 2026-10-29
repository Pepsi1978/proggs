package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.ablage.AblageSpeicher
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.ReasoningEffort
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
    private const val NACHGETRAGEN = "funktionen_nachgetragen.txt"
    /** Neue Bereiche, die [nachtragen] einmalig aus der APK übernimmt. */
    private val NEUE_BEREICHE = listOf("Geniale Ideen")
    private const val PFLEGE_MS = 4 * 60_000L
    /** So viele Versionen gehen höchstens in einen Modellaufruf, so viele Portionen höchstens in einen Lauf. */
    private const val PORTION = 30
    private const val PORTIONEN = 4

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

    /** Jarvis' Versionslog aus der APK; nur ein gelesenes Log wird gemerkt, ein Lesefehler wird beim nächsten Mal neu versucht. */
    fun versionslog(c: Context): List<Versionseintrag> = log ?: runCatching {
        Funktionsdatei.versionslog(c.applicationContext.assets.open("versionslog.json").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyList()).also { if (it.isNotEmpty()) log = it }

    /**
     * Der Stand; beim ersten Aufruf von der Platte, danach aus dem Speicher (alle Änderungen laufen über [schreibe]).
     * Fehlt die Datei, kommt die Erstbefüllung aus der APK (einmalig, danach nie wieder überschrieben). Der geprüfte Stand
     * steht als Marke vorn im Text selbst: So passen Text und Stand immer zusammen, auch wenn die Verwaltung verloren geht.
     */
    fun lade(c: Context): FunktionsStand = synchronized(schreiben) {
        _stand.value?.let { return it }
        val d = ordner(c)
        val text = File(d, TEXT)
        val geladen = if (text.exists()) {
            val (code, name, inhalt) = Funktionsdatei.erstbefuellung(text.readText())
            val meta = runCatching { JSONObject(File(d, META).readText()) }.getOrNull()
            if (meta != null) FunktionsStand.aus(inhalt + "\n", meta).let { if (code > 0) it.copy(geprueftCode = code, geprueftName = name) else it }
            else {
                // Verwaltung fehlt oder ist kaputt: Text und Stand aus der Datei behalten; eine neue Revision, die kein offener Entwurf trägt.
                val (asset, assetName, _) = Funktionsdatei.erstbefuellung(erstbefuellung(c))
                FunktionsStand(inhalt + "\n", (System.currentTimeMillis() / 1000L).toInt(), if (code > 0) code else asset, if (code > 0) name else assetName,
                    0L, 0L, emptySet(), emptyList(), "Verwaltung neu angelegt").also {
                    AblageSpeicher.schreibeAtomar(File(d, META), it.metaJson().toString().toByteArray())
                    Logbuch.gedaechtnis("Funktionsübersicht: Verwaltung neu angelegt", "geprüft bis versionCode ${it.geprueftCode}")
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
        nachtragen(c, geladen).also { _stand.value = it }
    }

    /**
     * Bereiche, die eine Version neu mitbringt. Die Pflege ergänzt nur Zeilen in vorhandenen Bereichen, deshalb kommt ein
     * neuer Bereich einmalig aus der APK (hinter „Angebundene Apps“). Gemerkt wird das in einer eigenen Datei: Löscht Frank
     * den Bereich danach, bleibt er weg.
     */
    private fun nachtragen(c: Context, s: FunktionsStand): FunktionsStand {
        val datei = File(ordner(c), NACHGETRAGEN)
        val erledigt = runCatching { datei.readLines().filter { it.isNotBlank() }.toSet() }.getOrDefault(emptySet())
        val offen = NEUE_BEREICHE.filter { it !in erledigt }
        if (offen.isEmpty()) return s
        val vorlage = Funktionsdatei.erstbefuellung(erstbefuellung(c)).third
        var text = s.text
        for (name in offen) {
            if (text.lines().any { it.trim() == "## $name" }) continue
            val teil = Funktionsdatei.bereiche(vorlage, name) ?: continue
            val zeilen = text.trimEnd().lines().toMutableList()
            val anker = zeilen.indexOfFirst { it.trim() == "## Angebundene Apps" }
            val danach = if (anker < 0) -1 else (anker + 1 until zeilen.size).firstOrNull { zeilen[it].startsWith("## ") } ?: -1
            if (danach < 0) zeilen += listOf("", teil) else zeilen.addAll(danach, listOf(teil, ""))
            text = zeilen.joinToString("\n") + "\n"
        }
        AblageSpeicher.schreibeAtomar(datei, (erledigt + offen).joinToString("\n").toByteArray())
        if (text == s.text) return s
        sichere(c, s)
        val neu = s.copy(text = text, revision = s.revision + 1)
        schreibe(c, neu)
        Logbuch.gedaechtnis("Funktionsübersicht ergänzt", "neue Bereiche: " + offen.joinToString(", "))
        return neu
    }

    private fun erstbefuellung(c: Context): String =
        runCatching { c.applicationContext.assets.open("funktionen.md").bufferedReader().use { it.readText() } }.getOrDefault("")

    private fun schreibe(c: Context, s: FunktionsStand) {
        val d = ordner(c)
        AblageSpeicher.schreibeAtomar(File(d, TEXT), Funktionsdatei.mitMarke(s.text, s.geprueftCode, s.geprueftName).toByteArray())
        AblageSpeicher.schreibeAtomar(File(d, META), s.metaJson().toString().toByteArray())
        _stand.value = s
    }

    /** Die Fassung vor einer Änderung, mit ihrem geprüften Stand: Rückgängig holt beides zurück. */
    private fun sichere(c: Context, alt: FunktionsStand) =
        AblageSpeicher.schreibeAtomar(File(ordner(c), VORHER), Funktionsdatei.mitMarke(alt.text, alt.geprueftCode, alt.geprueftName).toByteArray())

    fun hatVorfassung(c: Context): Boolean = File(ordner(c), VORHER).exists()

    private fun nameVon(c: Context, code: Int, sonst: String) = versionslog(c).firstOrNull { it.code == code }?.name ?: sonst

    /**
     * Franks Bearbeitung von Hand. [ausRevision], [basis] und [ausGeprueft] beschreiben die Fassung, von der der Entwurf
     * ausging. Hat seitdem jemand gespeichert (auch die Pflege), kommt false und nichts wird überschrieben. Speichert Frank
     * danach bewusst trotzdem, fällt der geprüfte Stand auf den seines Entwurfs zurück: Was die Pflege inzwischen eingearbeitet
     * hatte, arbeitet sie erneut ein, statt es zu verlieren. Als Franks eigene Zeilen zählt nur, was er gegenüber [basis] geändert hat.
     */
    fun speichereVonHand(c: Context, text: String, ausRevision: Int, basis: String, ausGeprueft: Int): Boolean = synchronized(schreiben) {
        val alt = lade(c)
        if (alt.revision != ausRevision) return false
        val sauber = text.replace("\r\n", "\n").trim() + "\n"
        if (sauber == alt.text) return true
        sichere(c, alt)
        val geprueft = minOf(alt.geprueftCode, ausGeprueft)
        schreibe(c, alt.copy(text = sauber, revision = alt.revision + 1, manuell = System.currentTimeMillis(),
            geprueftCode = geprueft, geprueftName = if (geprueft == alt.geprueftCode) alt.geprueftName else nameVon(c, geprueft, alt.geprueftName),
            manuelleZeilen = Funktionsdatei.manuelleZeilenNach(basis, sauber, alt.manuelleZeilen), hinweise = emptyList()))
        Logbuch.gedaechtnis("Funktionsübersicht von Hand bearbeitet", "${sauber.length} Zeichen" + if (geprueft < alt.geprueftCode) ", geprüfter Stand zurück auf $geprueft" else "")
        if (geprueft < alt.geprueftCode) anstossen(c, "nach dem Überschreiben")
        true
    }

    /** Holt die Fassung vor der letzten Änderung zurück, samt ihrem geprüften Stand (eine zurückgenommene Pflege wird so wieder fällig). */
    fun rueckgaengig(c: Context): Boolean = synchronized(schreiben) {
        val vorher = File(ordner(c), VORHER)
        if (!vorher.exists()) return false
        val alt = lade(c)
        val (code, name, text) = Funktionsdatei.erstbefuellung(vorher.readText())
        val inhalt = text + "\n"
        sichere(c, alt)
        // Zurückgeholte Zeilen werden nicht zu Franks eigenen: Was die Pflege geschrieben hatte, darf sie weiter pflegen.
        schreibe(c, alt.copy(text = inhalt, revision = alt.revision + 1, manuell = System.currentTimeMillis(),
            geprueftCode = if (code > 0) code else alt.geprueftCode, geprueftName = if (code > 0) name else alt.geprueftName,
            manuelleZeilen = Funktionsdatei.manuelleZeilenNach(inhalt, inhalt, alt.manuelleZeilen)))
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

    private fun hindernis(app: Context, agent: JarvisAgent): String? {
        if (de.frank.jarvis.live.LiveGespraech.aktiv) return "ein Live-Gespräch läuft"
        if (!agent.verbunden) return "Jarvis ist nicht mit ChatGPT angemeldet"
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
            val agent = JarvisAgent(app)
            var ergebnis = ""
            // Viele offene Versionen (lange nicht gepflegt) in Portionen, älteste zuerst; jede Portion hebt den Stand nur bis zu ihrer letzten Version.
            for (portion in 1..PORTIONEN) {
                val s = lade(app)
                if (!Funktionsdatei.pflegeFaellig(s.geprueftCode, installiertCode)) return@withContext melde(ergebnis.ifEmpty { "Die Übersicht ist auf dem Stand der installierten Version ${installiertName}." })
                hindernis(app, agent)?.let { return@withContext melde("Pflege verschoben: $it. Bis dahin zeigt die Übersicht die noch nicht eingearbeiteten Versionen.") }
                val log = versionslog(app)
                // Ohne lesbares Versionslog gibt es keinen Beleg: nichts als geprüft markieren.
                if (log.isEmpty()) return@withContext melde("Das Versionslog ist nicht lesbar; die Pflege wartet, die Übersicht bleibt unverändert.")
                val offen = Funktionsdatei.offeneVersionen(log, s.geprueftCode, installiertCode)
                if (offen.isEmpty()) return@withContext melde(abschliessen(app, installiertCode, emptyList(), emptyList(), "Pflege: keine neuen Versionslog-Einträge"))
                val teil = offen.take(PORTION)
                val bis = if (teil.size == offen.size) installiertCode else teil.last().code
                _pflege.value = "Jarvis arbeitet ${teil.size} neue Version(en) ein" + (if (offen.size > teil.size) " (von ${offen.size})" else "") + " …"
                // Fertige Aufträge ab dem zuletzt geprüften Stand (Zeit aus dem Versionslog), neueste zuerst.
                val seit = (log.firstOrNull { it.code == s.geprueftCode }?.let { Funktionsdatei.standZeit(it.stand) } ?: (System.currentTimeMillis() - 14 * 86_400_000L)) - 3_600_000L
                val auftraege = runCatching { ProgrammierFaehigkeit.fertigeAuftraege(app, "jarvis", seit) }.getOrDefault(emptyList()).asReversed()
                val eingabe = buildString {
                    append("AKTUELLE ÜBERSICHT:\n").append(s.text.take(16_000)).append("\n\n")
                    append("NEU INSTALLIERTE VERSIONEN (Versionslog, Beleg):\n")
                    teil.forEach { append("- ").append(it.name).append(" (").append(it.stand).append("): ").append(it.notiz.take(800)).append('\n') }
                    append("\nFERTIG GEMELDETE PROGRAMMIERAUFTRÄGE AN JARVIS (neueste zuerst, nur Zusatzinfo, kein Beleg):\n").append(auftraege.joinToString("\n").take(6_000).ifEmpty { "(keine)" })
                    if (s.manuelleZeilen.isNotEmpty()) append("\n\nFRANKS EIGENE ZEILEN:\n").append(s.manuelleZeilen.joinToString("\n") { "- $it" }.take(3_000))
                }
                val e = Einstellungen.get(app)
                // Feste, niedrige Denkstufe: Die Aufgabe ist klein, und Franks Chat-Einstellung soll sie nicht über das Zeitlimit treiben.
                val stufe = e.modell.normalizeEffort(ReasoningEffort.LOW)
                // Das Live-Gespräch hat Vorrang: Beginnt eines, bricht die Pflege ab und bleibt fällig.
                var live = false
                val roh = withTimeoutOrNull(PFLEGE_MS) {
                    coroutineScope {
                        val anfrage = async { agent.auth.streamChat(ANWEISUNG, listOf(ChatTurn("user", eingabe)), e.modell, stufe) }
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
                ergebnis = melde(abschliessen(app, bis, Funktionsdatei.operationen(antwort), teil, "Pflege ($ausloeser)"))
            }
            ergebnis
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
        val name = nameVon(app, bisCode, installiertName)
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

package de.frank.jarvis.gedaechtnis

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Ein Eintrag aus Jarvis' eigenem Versionslog (assets/versionslog.json): Beleg, dass diese Version gebaut und installiert wurde. */
data class Versionseintrag(val code: Int, val name: String, val stand: String, val notiz: String)

/** Eine Pflege-Änderung an der Funktionsübersicht: neu, ersetzen oder entfernen, verankert an einer wörtlichen Zeile. */
data class FunktionsOperation(val aktion: String, val bereich: String, val alt: String, val neu: String, val beleg: String)

/** Was das Anwenden ergab: der neue Text, was geändert wurde, was übersprungen wurde und Hinweise zu Franks eigenen Zeilen. */
data class PflegeErgebnis(val text: String, val angewandt: List<String>, val uebersprungen: List<String>, val hinweise: List<String>)

/** Der gespeicherte Stand der Funktionsübersicht (Text und Verwaltung). */
data class FunktionsStand(
    val text: String,
    /** Zählt jede Speicherung hoch; Speichern gelingt nur mit dem Stand, von dem die Bearbeitung ausging. */
    val revision: Int,
    /** Bis zu dieser installierten Jarvis-Version ist die Übersicht geprüft. */
    val geprueftCode: Int,
    val geprueftName: String,
    val gepflegt: Long,
    /** Wann Frank zuletzt von Hand bearbeitet hat (0 = nie). */
    val manuell: Long,
    /** Zeilen, die Frank selbst geschrieben oder geändert hat (normalisiert); die Pflege fasst sie nicht an. */
    val manuelleZeilen: Set<String>,
    /** Was die Pflege an Franks eigenen Zeilen ändern wollte, aber ihm überlässt. */
    val hinweise: List<String>,
    val herkunft: String,
) {
    fun metaJson(): JSONObject = JSONObject().put("revision", revision).put("geprueftCode", geprueftCode).put("geprueftName", geprueftName)
        .put("gepflegt", gepflegt).put("manuell", manuell).put("manuelleZeilen", JSONArray(manuelleZeilen.toList()))
        .put("hinweise", JSONArray(hinweise)).put("herkunft", herkunft)

    companion object {
        fun aus(text: String, meta: JSONObject) = FunktionsStand(
            text = text,
            revision = meta.optInt("revision", 0),
            geprueftCode = meta.optInt("geprueftCode", 0),
            geprueftName = meta.optString("geprueftName"),
            gepflegt = meta.optLong("gepflegt", 0L),
            manuell = meta.optLong("manuell", 0L),
            manuelleZeilen = meta.optJSONArray("manuelleZeilen")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }.toSet() }.orEmpty(),
            hinweise = meta.optJSONArray("hinweise")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() } }.orEmpty(),
            herkunft = meta.optString("herkunft"),
        )
    }
}

/**
 * Die Regeln der Funktionsübersicht, ohne Android: Erstbefüllung lesen, Versionsspanne bestimmen, Pflege-Operationen
 * deterministisch anwenden und Franks eigene Zeilen schützen. Das Modell schlägt nur Operationen vor; was davon gilt,
 * entscheidet dieser Code.
 */
object Funktionsdatei {
    /** Höchstlänge einer Zeile der Übersicht; die Pflege bleibt knapp. */
    const val ZEILE_HOECHSTENS = 320
    /** So viele Pflegehinweise werden aufgehoben. */
    const val HINWEISE_HOECHSTENS = 12
    private val STAND_MARKE = Regex("^<!--\\s*stand:\\s*(\\d+)\\s+([^\\s>]+)\\s*-->\\s*$")

    /** Kopfzeile der Erstbefüllung `<!-- stand: 197 1.8.186 -->`: welcher Code-Stand geprüft wurde, und der Text ohne sie. */
    fun erstbefuellung(roh: String): Triple<Int, String, String> {
        val zeilen = roh.replace("\r\n", "\n").lines()
        val m = zeilen.firstOrNull()?.let { STAND_MARKE.find(it.trim()) }
        return if (m == null) Triple(0, "", roh.trim()) else Triple(m.groupValues[1].toInt(), m.groupValues[2], zeilen.drop(1).joinToString("\n").trim())
    }

    /** Der Text, wie er auf dem Handy liegt: mit der Stand-Marke vorn, damit Text und geprüfter Stand zusammen gespeichert werden. */
    fun mitMarke(text: String, code: Int, name: String): String = "<!-- stand: $code ${name.ifBlank { "?" }} -->\n" + text

    private val STAND_ZEIT = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMAN)

    /** Zeitpunkt einer Versionslog-Angabe „18.10.2026, 19:46 Uhr“ in Millisekunden, sonst null. */
    fun standZeit(stand: String, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Long? = runCatching {
        java.time.LocalDateTime.parse(stand.removeSuffix(" Uhr").trim(), STAND_ZEIT).atZone(zone).toInstant().toEpochMilli()
    }.getOrNull()

    /** Die App eines Auftrags heißt [name], gern mit Ordner in Klammern („Jarvis (Ordner Jarvis)“), aber nicht „JarvisWatch“ oder „Jarvis-Server“. */
    fun appPasst(appFeld: String, name: String): Boolean =
        Regex("^\\s*" + Regex.escape(name) + "(?![\\w-])(\\s*\\(.*\\))?\\s*$", RegexOption.IGNORE_CASE).matches(appFeld.trim())

    /** Liest Jarvis' Versionslog; ein kaputtes Log ergibt eine leere Liste. */
    fun versionslog(json: String): List<Versionseintrag> = runCatching {
        val a = JSONObject(json).getJSONArray("eintraege")
        (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { Versionseintrag(it.optInt("versionCode"), it.optString("versionName"), it.optString("stand"), it.optString("notiz")) } }
            .filter { it.code > 0 }
    }.getOrDefault(emptyList())

    /** Die installierten Versionen, die in der Übersicht noch nicht eingearbeitet sind (älteste zuerst). */
    fun offeneVersionen(log: List<Versionseintrag>, geprueft: Int, installiert: Int): List<Versionseintrag> =
        log.filter { it.code > geprueft && it.code <= installiert }.sortedBy { it.code }

    /**
     * Nur eine neuere installierte Version löst die Pflege aus. Ein angenommener oder als fertig gemeldeter Auftrag
     * allein ändert nichts: Erst die installierte App mit ihrem Versionslog belegt, was es wirklich gibt.
     */
    fun pflegeFaellig(geprueft: Int, installiert: Int): Boolean = installiert > geprueft

    // ---------------------------------------------------------------- Zeilen

    fun normal(zeile: String): String = zeile.trim().removePrefix("- ").removePrefix("* ").trim().lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ")
        .trimEnd('.', ';')

    private fun istPunkt(zeile: String) = zeile.trimStart().let { it.startsWith("- ") || it.startsWith("* ") }
    private fun istBereich(zeile: String) = zeile.startsWith("## ")
    private fun bereichsName(zeile: String) = zeile.removePrefix("## ").trim()

    /** Die inhaltlichen Zeilen (Punkte und Absätze, keine Überschriften, keine Leerzeilen), normalisiert. */
    private fun inhalt(text: String): Set<String> = text.lines().filter { it.isNotBlank() && !it.startsWith("#") }.map { normal(it) }.filter { it.isNotEmpty() }.toSet()

    /**
     * Nach einer Bearbeitung von Hand: Was Frank neu geschrieben oder geändert hat, kommt zu seinen eigenen Zeilen;
     * was nicht mehr im Text steht, fällt heraus.
     */
    fun manuelleZeilenNach(alt: String, neu: String, bisher: Set<String>): Set<String> {
        val vorher = inhalt(alt)
        val jetzt = inhalt(neu)
        return (bisher + (jetzt - vorher)).filter { it in jetzt }.toSet()
    }

    // ---------------------------------------------------------------- Operationen

    /** Die Operationen aus der Antwort des Modells; alles Unlesbare fällt weg (dann bleibt die Übersicht unverändert). */
    fun operationen(antwort: JSONObject?): List<FunktionsOperation> {
        val a = antwort?.optJSONArray("operationen") ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }.mapNotNull { o ->
            val aktion = o.optString("aktion").trim().lowercase(Locale.GERMAN)
            if (aktion !in listOf("neu", "ersetzen", "entfernen")) return@mapNotNull null
            FunktionsOperation(aktion, einzeilig(o.optString("bereich")).trimStart('#', ' ').trim().take(80), einzeilig(o.optString("alt")), einzeilig(o.optString("neu")), o.optString("beleg").trim().take(200))
        }.take(40)
    }

    private fun einzeilig(t: String) = t.replace(Regex("\\s*\\n\\s*"), " ").trim()

    private fun alsPunkt(t: String): String = "- " + t.trim().removePrefix("- ").removePrefix("* ").trim().take(ZEILE_HOECHSTENS)

    /**
     * Wendet die Operationen der Reihe nach an. Verankert wird an der wörtlichen Zeile (Leerzeichen und Groß-/Kleinschreibung
     * egal): Hat Frank sie inzwischen geändert, greift die Operation nicht. Zeilen in [manuelle] bleiben unangetastet; was die
     * Pflege daran ändern wollte, steht als Hinweis für Frank im Ergebnis.
     */
    fun wende(text: String, ops: List<FunktionsOperation>, manuelle: Set<String> = emptySet()): PflegeErgebnis {
        val zeilen = text.replace("\r\n", "\n").lines().toMutableList()
        val angewandt = mutableListOf<String>()
        val uebersprungen = mutableListOf<String>()
        val hinweise = mutableListOf<String>()
        // Steht dieselbe Zeile in mehreren Bereichen, gilt die im genannten Bereich; sonst die erste.
        fun finde(alt: String, bereich: String): Int {
            val n = normal(alt)
            if (n.isEmpty()) return -1
            val treffer = zeilen.indices.filter { !istBereich(zeilen[it]) && !zeilen[it].startsWith("#") && normal(zeilen[it]) == n }
            if (treffer.size < 2 || bereich.isBlank()) return treffer.firstOrNull() ?: -1
            val kopf = passenderBereich(zeilen, bereich)
            if (kopf < 0) return treffer.first()
            val ende = (kopf + 1 until zeilen.size).firstOrNull { istBereich(zeilen[it]) } ?: zeilen.size
            return treffer.firstOrNull { it in kopf + 1 until ende } ?: treffer.first()
        }
        fun vorhanden(neu: String): Boolean = normal(neu).let { n -> zeilen.any { !istBereich(it) && normal(it) == n } }

        for (op in ops) {
            when (op.aktion) {
                "neu" -> {
                    if (op.neu.isBlank()) { uebersprungen += "Neu ohne Text"; continue }
                    if (vorhanden(op.neu)) { uebersprungen += "Steht schon da: ${op.neu}"; continue }
                    val bereich = op.bereich.trimStart('#', ' ').trim().ifBlank { "Sonstiges" }
                    val kopf = passenderBereich(zeilen, bereich)
                    if (kopf < 0) {
                        while (zeilen.isNotEmpty() && zeilen.last().isBlank()) zeilen.removeAt(zeilen.size - 1)
                        zeilen += listOf("", "## $bereich", "", alsPunkt(op.neu))
                    } else {
                        // Ans Ende des Bereichs, direkt hinter seinen letzten Inhalt.
                        var ende = kopf + 1
                        while (ende < zeilen.size && !istBereich(zeilen[ende])) ende++
                        var nach = ende
                        while (nach > kopf + 1 && zeilen[nach - 1].isBlank()) nach--
                        if (nach == kopf + 1) { zeilen.add(nach, ""); nach++ }
                        zeilen.add(nach, alsPunkt(op.neu))
                    }
                    angewandt += "Neu ($bereich): ${op.neu}"
                }
                "ersetzen", "entfernen" -> {
                    val i = finde(op.alt, op.bereich)
                    if (i < 0) { uebersprungen += "Nicht gefunden (vielleicht von Hand geändert): ${op.alt.ifBlank { "(leer)" }}"; continue }
                    if (normal(zeilen[i]) in manuelle) {
                        hinweise += (if (op.aktion == "entfernen") "Vielleicht veraltet: „${zeilen[i].trim().removePrefix("- ")}“" else "Vorschlag: „${zeilen[i].trim().removePrefix("- ")}“ → „${op.neu}“") +
                            (if (op.beleg.isNotEmpty()) " (${op.beleg})" else "")
                        uebersprungen += "Von Frank geschrieben, nicht angetastet: ${zeilen[i].trim()}"
                        continue
                    }
                    if (op.aktion == "entfernen") {
                        angewandt += "Entfernt: ${zeilen[i].trim().removePrefix("- ")}"
                        zeilen.removeAt(i)
                    } else {
                        if (op.neu.isBlank()) { uebersprungen += "Ersetzen ohne neuen Text: ${op.alt}"; continue }
                        val doppelt = normal(op.neu).let { n -> zeilen.indices.any { it != i && !istBereich(zeilen[it]) && normal(zeilen[it]) == n } }
                        angewandt += "Ersetzt: ${zeilen[i].trim().removePrefix("- ")} → ${op.neu}"
                        // Steht die neue Fassung schon da, fällt die alte nur weg: keine Dopplung.
                        if (doppelt) zeilen.removeAt(i) else zeilen[i] = if (istPunkt(zeilen[i])) zeilen[i].takeWhile { it == ' ' || it == '\t' } + alsPunkt(op.neu) else op.neu.take(ZEILE_HOECHSTENS)
                    }
                }
            }
        }
        return PflegeErgebnis(aufgeraeumt(zeilen), angewandt, uebersprungen, hinweise)
    }

    /**
     * Der vorhandene Bereich zu einem Namen des Modells: gleich (ohne Groß-/Kleinschreibung), sonst der einzige, dessen Name
     * im anderen steckt („Sprache und Stimme“ → „Sprache“). Sonst -1, dann entsteht ein neuer Bereich.
     */
    private fun passenderBereich(zeilen: List<String>, name: String): Int {
        val n = name.lowercase(Locale.GERMAN)
        val koepfe = zeilen.indices.filter { istBereich(zeilen[it]) }
        koepfe.firstOrNull { bereichsName(zeilen[it]).lowercase(Locale.GERMAN) == n }?.let { return it }
        val aehnlich = koepfe.filter { bereichsName(zeilen[it]).lowercase(Locale.GERMAN).let { b -> b.length >= 4 && n.length >= 4 && (b.contains(n) || n.contains(b)) } }
        return aehnlich.singleOrNull() ?: -1
    }

    /** Leere Bereiche fallen weg, mehrfache Leerzeilen werden zu einer. */
    private fun aufgeraeumt(zeilen: List<String>): String {
        val ohneLeere = mutableListOf<String>()
        var i = 0
        while (i < zeilen.size) {
            val z = zeilen[i]
            if (istBereich(z)) {
                var ende = i + 1
                while (ende < zeilen.size && !istBereich(zeilen[ende])) ende++
                if (zeilen.subList(i + 1, ende).all { it.isBlank() }) { i = ende; continue }
            }
            ohneLeere += z
            i++
        }
        val ergebnis = mutableListOf<String>()
        ohneLeere.forEach { if (!(it.isBlank() && ergebnis.lastOrNull()?.isBlank() == true)) ergebnis += it }
        return ergebnis.joinToString("\n").trim() + "\n"
    }

    // ---------------------------------------------------------------- Lesen

    /** Nur die Bereiche, deren Überschrift [suche] enthält (für gezieltes Lesen); ohne Treffer null. */
    fun bereiche(text: String, suche: String): String? {
        val s = suche.trim().lowercase(Locale.GERMAN)
        if (s.isEmpty()) return text
        val treffer = mutableListOf<String>()
        var drin = false
        text.lines().forEach { z ->
            if (istBereich(z)) drin = bereichsName(z).lowercase(Locale.GERMAN).contains(s)
            if (drin) treffer += z
        }
        return treffer.joinToString("\n").trim().ifEmpty { null }
    }

    /**
     * Der Kopf, den jedes Lesen voranstellt: geprüfter Stand, installierte Version, noch nicht eingearbeitete Versionen
     * (mit ihren Versionslog-Notizen) und die Grenzen der Aussagekraft. Er wird immer neu berechnet und braucht kein Modell.
     */
    fun kopf(stand: FunktionsStand, installiertCode: Int, installiertName: String, offen: List<Versionseintrag>, zeit: (Long) -> String): String = buildString {
        append("FUNKTIONSÜBERSICHT JARVIS (auf dem Handy gespeichert, ohne GitHub lesbar)\n")
        append("Geprüfter Stand: Jarvis ").append(stand.geprueftName.ifEmpty { "?" }).append(" (versionCode ").append(stand.geprueftCode).append(")")
        if (stand.gepflegt > 0) append(", zuletzt gepflegt ").append(zeit(stand.gepflegt))
        if (stand.herkunft.isNotEmpty()) append(" · ").append(stand.herkunft)
        append(".\n")
        append("Installiert: Jarvis ").append(installiertName).append(" (versionCode ").append(installiertCode).append(").")
        if (stand.manuell > 0) append(" Von Frank bearbeitet ").append(zeit(stand.manuell)).append(".")
        append('\n')
        if (offen.isNotEmpty()) {
            append("NOCH NICHT EINGEARBEITET (installiert, laut Versionslog vorhanden; die Übersicht unten kennt es noch nicht):\n")
            offen.takeLast(25).forEach { append("- ").append(it.name).append(": ").append(it.notiz.take(400)).append('\n') }
            if (offen.size > 25) append("- … und ").append(offen.size - 25).append(" ältere Versionen\n")
        } else if (installiertCode < stand.geprueftCode) {
            append("ACHTUNG: Die Übersicht ist auf einem neueren Stand geprüft als die installierte App; Funktionen ab versionCode ").append(installiertCode + 1).append(" sind hier vielleicht noch nicht installiert.\n")
        }
        if (stand.hinweise.isNotEmpty()) {
            append("PFLEGEHINWEISE ZU FRANKS EIGENEN ZEILEN (nicht automatisch geändert):\n")
            stand.hinweise.forEach { append("- ").append(it).append('\n') }
        }
        append("Grenzen: Nur installierte Funktionen stehen hier, keine geplanten oder bloß beauftragten. Was hier fehlt, ist nicht nachweislich fehlend; ")
        append("bei Detailfragen oder unsicherem Stand zusätzlich im Code prüfen (repo_lesen).\n")
    }
}

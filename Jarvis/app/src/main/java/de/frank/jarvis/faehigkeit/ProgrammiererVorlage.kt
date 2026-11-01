package de.frank.jarvis.faehigkeit

/**
 * Auftragseinstieg in der Eingabeblase; Frank ergänzt die Aufgabe und sendet selbst. Über den Programmierbutton wählt er vorher
 * das Modell ([VORSPANN_OPUS], [VORSPANN_SOL]), das dann für diesen Auftrag verbindlich gilt ([modell]); der Senden-Knopf eines
 * Prompts nimmt [VORSPANN] und die Modellfrage kommt nach dem Absenden.
 * Gesendet geht der Text den normalen Weg zu Jarvis, der ihn mit [hinweis] als neuen Auftrag über `programmieren` übergibt
 * (App-Erkennung, Rückfragen und Review wie bei jedem Programmierauftrag). Rein und ohne Android (ProgrammiererVorlageTest).
 */
object ProgrammiererVorlage {
    const val VORSPANN = "Übergib bitte folgende Aufgabe an den Programmierer:"
    const val VORSPANN_OPUS = "Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell Opus 5.5:"
    const val VORSPANN_SOL = "Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell Sol 6.1:"
    const val VORSPANN_KIMI = "Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell Kimi K3.1 Pro:"
    const val VORSPANN_ARGON = "Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell Gemini 4 Argon:"

    const val MODELL_OPUS = "opus"
    const val MODELL_SOL = "sol"
    const val MODELL_KIMI = "kimi"
    const val MODELL_ARGON = "argon"
    /** Fünfter Knopf der Vorwahl: öffnet die Vollbildauswahl; kein Modell und deshalb nicht in [MODELL_OPTIONEN] (alte Karten fallen darauf zurück). */
    const val OPENLAUNCHER = "openlauncher"
    /** Steht im Einstieg, wenn OpenLauncher dem Modell keine Effort-Stufe anbietet (Wortlaut wie im OpenLauncher). */
    const val OHNE_EFFORT = "ohne Stufe"

    /** Ein Modellknopf. [katalogName]: startet über das OpenLauncher-Angebot (Minimalprofil, Schnellmodus, Effort High) mit [cli] (Franks Standard vom 31.10.2026) oder einem der [clis], wenn Frank es nennt. */
    private data class KnopfModell(val wert: String, val label: String, val vorspann: String, val katalogName: String? = null, val cli: String? = null, val clis: List<String>)
    private val KNOPF_MODELLE = listOf(
        KnopfModell(MODELL_OPUS, "Opus 5.5", VORSPANN_OPUS, clis = listOf("claude")),
        KnopfModell(MODELL_SOL, "Sol 6.1", VORSPANN_SOL, clis = listOf("codex", "opencode")),
        KnopfModell(MODELL_KIMI, "Kimi K3.1 Pro", VORSPANN_KIMI, "Kimi K3.1 Pro", "opencode", clis = listOf("opencode", "kimi")),
        KnopfModell(MODELL_ARGON, "Gemini 4 Argon", VORSPANN_ARGON, "Gemini 4 Argon", "antigravity", clis = listOf("antigravity", "opencode")),
    )

    /** Wert und Beschriftung der Modellknöpfe, für Vorwahl und Karte dieselbe Reihenfolge. */
    val MODELL_OPTIONEN = KNOPF_MODELLE.map { it.wert to it.label }

    /** Die Knöpfe des Programmierbuttons: die vier Modelle und danach die Vollbildauswahl. */
    val VORWAHL_OPTIONEN = MODELL_OPTIONEN + (OPENLAUNCHER to "OpenLauncher")

    private val CLI_KNOPF = mapOf("claude" to "Claude Code", "codex" to "Codex CLI", "opencode" to "OpenCode CLI", "kimi" to "Kimi Code CLI", "antigravity" to "Antigravity CLI")
    private fun cliEinstieg(k: KnopfModell, cli: String) =
        k.vorspann.removeSuffix(":") + (if (cli == "claude") " in " else " im ") + CLI_KNOPF.getValue(cli) + ":"

    /** Wert und Beschriftung der CLIs, die OpenLauncher dem Knopf [modell] anbietet; der erste ist der Standard. */
    fun cliOptionen(modell: String): List<Pair<String, String>> =
        KNOPF_MODELLE.firstOrNull { it.wert == modell }?.clis.orEmpty().map { it to CLI_KNOPF.getValue(it) }

    /** Einstieg je Modell und gewähltem CLI → (Modell, CLI). */
    private val CLI_EINSTIEGE: Map<String, Pair<String, String>> =
        KNOPF_MODELLE.flatMap { k -> k.clis.map { cliEinstieg(k, it) to (k.wert to it) } }.toMap()

    /** Der Einstieg zum Knopf [modell] mit gewähltem [cli]. */
    fun vorspannFuer(modell: String, cli: String): String {
        val k = KNOPF_MODELLE.firstOrNull { it.wert == modell } ?: throw IllegalArgumentException("Unbekanntes Modell: $modell")
        require(cli in k.clis) { "CLI $cli passt nicht zu $modell" }
        return cliEinstieg(k, cli)
    }

    /** Alles, was Frank in der Vollbildauswahl gewählt hat, in den Worten von OpenLauncher. */
    data class FreieWahl(val modell: String, val anbieter: String, val effort: String, val profil: String, val modus: String, val cli: String)

    private const val EINSTIEG_MODELL = "Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell "
    private val FREI = Regex("^" + Regex.escape(EINSTIEG_MODELL) + "(.+?) von ([^,\\n]+), Effort ([^,\\n]+), Profil ([^,\\n]+), Modus ([^,\\n]+), CLI ([^:\\n]+):")

    /** Der Einstieg der Vollbildauswahl; [freieWahl] liest ihn beim Senden wieder aus. */
    fun vorspannFrei(w: FreieWahl) = "$EINSTIEG_MODELL${w.modell} von ${w.anbieter}, Effort ${w.effort}, Profil ${w.profil}, Modus ${w.modus}, CLI ${w.cli}:"

    /** Die Angaben aus dem Einstieg der Vollbildauswahl; null bei jedem anderen Text. */
    fun freieWahl(text: String): FreieWahl? = FREI.find(text.trimStart())?.groupValues?.let { FreieWahl(it[1], it[2], it[3], it[4], it[5], it[6]) }

    /** Das im Einstieg gewählte CLI eines Modellknopfs; null ohne CLI im Einstieg. */
    fun cli(text: String): String? = vorspannIn(text)?.let { CLI_EINSTIEGE[it]?.second }

    fun katalogName(modell: String): String? = KNOPF_MODELLE.firstOrNull { it.wert == modell }?.katalogName
    fun katalogCli(modell: String): String? = KNOPF_MODELLE.firstOrNull { it.wert == modell }?.cli

    /** Das CLI zum Knopf: Ein anderes angebotenes nur, wenn [gegeben] es nennt und Franks [worte] es ausdrücklich enthalten; der Modellname allein („Kimi“) zählt nicht. */
    fun katalogCliFuer(modell: String, gegeben: String, worte: String): String? {
        val knopf = KNOPF_MODELLE.firstOrNull { it.wert == modell } ?: return null
        val wunsch = OpenLauncherKatalog.cliAus(gegeben)
        val genannt = when (wunsch) {
            "kimi" -> Regex("(?i)kimi\\s*(code\\s*)?cli")
            "opencode" -> Regex("(?i)open\\s*code")
            "antigravity" -> Regex("(?i)antigravity|\\bagy\\b")
            else -> null
        }
        return if (wunsch != null && wunsch in knopf.clis && genannt?.containsMatchIn(worte) == true) wunsch else knopf.cli
    }

    private val ALLE = listOf(VORSPANN) + KNOPF_MODELLE.map { it.vorspann } + CLI_EINSTIEGE.keys

    /** Der Einstieg, mit dem [text] beginnt; null, wenn keiner. */
    fun vorspannIn(text: String): String? = text.trimStart().let { t -> ALLE.firstOrNull { t.startsWith(it) } ?: FREI.find(t)?.value }

    /** Beginnt [text] mit einem Auftragseinstieg? */
    fun aktiv(text: String): Boolean = vorspannIn(text) != null

    /** Der Einstieg zum per Knopf gewählten [modell] (siehe [MODELL_OPTIONEN]); ein unbekannter Wert darf nie still zu einem verbindlichen Modell werden. */
    fun vorspannFuer(modell: String): String =
        KNOPF_MODELLE.firstOrNull { it.wert == modell }?.vorspann ?: throw IllegalArgumentException("Unbekanntes Modell: $modell")

    /** Das verbindlich gewählte Modell aus dem Einstieg von [text]; null ohne Modell im Einstieg. */
    fun modell(text: String): String? = vorspannIn(text).let { v ->
        KNOPF_MODELLE.firstOrNull { it.vorspann == v }?.wert ?: CLI_EINSTIEGE[v]?.first ?: freieWahl(text)?.modell
    }

    /** Der Entwurf mit [vorspann] vorn und Leerzeichen dahinter; schon Getipptes bleibt als Aufgabe dahinter stehen, ein anderer Einstieg weicht. */
    fun einsetzen(entwurf: String, vorspann: String = VORSPANN): String {
        if (entwurf.trimStart().startsWith(vorspann)) return entwurf
        val rest = entfernen(entwurf)
        return if (rest.isBlank()) "$vorspann " else "$vorspann ${rest.trim()}"
    }

    /** Nimmt genau den Einstieg vorn wieder heraus; Getipptes dahinter bleibt unverändert. */
    fun entfernen(entwurf: String): String =
        vorspannIn(entwurf)?.let { entwurf.trimStart().removePrefix(it).trimStart() } ?: entwurf

    /** Die Aufgabe hinter dem Einstieg (getrimmt, leer = noch keine); null, wenn [text] nicht mit einem Einstieg beginnt. */
    fun aufgabe(text: String): String? = vorspannIn(text)?.let { text.trimStart().removePrefix(it).trim() }

    /** Steht hinter dem Einstieg nichts Inhaltliches (nur Leerzeichen oder Satzzeichen), startet kein Auftrag. */
    fun ohneAufgabe(text: String): Boolean = aufgabe(text)?.none { it.isLetterOrDigit() } == true

    /** Geht zusätzlich ans Modell, damit der Wunsch zuverlässig als Programmierauftrag übergeben und nicht im Chat beantwortet wird. */
    const val HINWEIS = "\n\n(Frank hat das über den Menüpunkt „Programmierer“ gestellt: Der Text nach dem Doppelpunkt ist ausdrücklich ein NEUER " +
        "Programmierauftrag. Übergib ihn mit programmieren, vollständig ausformuliert mit allem Wissen aus dem Gespräch; beantworte oder " +
        "programmiere ihn nicht selbst und nimm nicht programmier_nachtrag, außer Frank verlangt darin ausdrücklich einen Nachtrag. " +
        "Leite die App aus Auftrag und Gespräch ab; nennt Frank keine, lass app weg und frag nicht danach: Der Auftrag geht trotzdem sofort raus. " +
        "Nennt Frank ein Modell (auch gesprochen, etwa „Chemica 3.1 Pro“ für Kimi K3.1 Pro), Effort, Profil, Modus oder CLI, gib genau das in modell, " +
        "effort, profil, modus und cli mit; dann fragt Jarvis nicht nach Opus oder Sol.)"

    /** [HINWEIS] für [text]; steht das Modell im Einstieg, gilt es verbindlich und jede Frage danach entfällt. */
    fun hinweis(text: String): String = HINWEIS + (freieWahl(text)?.let {
        "\n\n(Frank hat in der OpenLauncher-Auswahl alles schon gewählt, es gilt verbindlich für diesen Auftrag: modell=${it.modell}, effort=${it.effort}, profil=${it.profil}, modus=${it.modus}, cli=${it.cli}. " +
            "Das geht der Regel oben vor: Jarvis übernimmt diese Angaben selbst aus dem Einstieg. Frag nichts davon nach und nimm nichts anderes.)"
    } ?: modell(text)?.let { m ->
        val gewaehlt = cli(text)
        "\n\n(Frank hat das Modell schon per Knopf gewählt, es gilt verbindlich für diesen Auftrag: modell=${katalogName(m) ?: m}" + (gewaehlt?.let { ", cli=$it" }.orEmpty()) +
            ". Das geht der Regel oben zum genannten Modell vor: Frag nicht nach dem Modell" + (if (gewaehlt != null) " oder dem CLI" else "") + " und nimm kein anderes; Effort, Profil und Modus nur, wenn Frank sie nennt.)"
    }.orEmpty())
}

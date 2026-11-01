package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.data.Einstellungen
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Das Angebot von OpenLauncher für Programmieraufträge mit frei gewähltem Modell (Franks Wahl vom 25.10.2026: „Starte
 * KimiK3 in OpenCode mit Effort High“). Die Modelle kommen aus dem Repo (OpenLauncher/models.json, über GitHub gelesen), Profile, Modi und die CLIs je Anbieter sind wie im OpenLauncherMac fest
 * (MainViewModel: profiles, workModes, fillCliTargets). Franks Wahl vom 25.10.2026: Fehlen Profil, Modus oder Effort, gelten
 * minimal, schnell und high; bietet OpenLauncher dem Modell mehrere CLIs an und nennt Frank keins, fragt Jarvis nach. Ist eine
 * Angabe mehrdeutig oder nicht im Angebot, fragt Jarvis ebenfalls nach ([Aufloesung.Frage]) statt zu raten.
 */
object OpenLauncherKatalog {

    data class Modell(val anbieter: String, val slug: String, val name: String, val anbieterName: String = "") {
        /** Die CLIs, die OpenLauncher diesem Modell anbietet (bei mehreren fragt Jarvis, wenn Frank keins nennt). */
        val clis: List<String> get() = clisFuer(anbieter).filterNot {
            // Probelauf am Windows-PC vom 01.11.2026: Googles Schlüssel in OpenCode hat für Argon ein Kontingent von 0, startbar ist es nur in Antigravity.
            it == "opencode" && anbieter.equals(GOOGLE, true) && slug.startsWith("gemini-4-argon")
        }

        /** Die CLIs, die mit [effort] wirklich starten: OpenAI weist Ultra über OpenCode ab, nur das Codex CLI kann es. */
        fun clisMit(effort: String): List<String> = clis.filterNot { it == "opencode" && effort == "ultra" }

        /** Wie ModelEntry.ModelString: Trägt der Slug den Anbieter schon vorn, kommt er nicht doppelt (außer bei NVIDIA). */
        val modellString: String get() = if (!anbieter.equals("nvidia", true) && slug.startsWith("$anbieter/", true)) slug else "$anbieter/$slug"
    }

    /** Ein Anbieterbereich der Modellliste von OpenLauncher (eine Gruppe aus models.json), nur sichtbare Modelle. */
    data class Gruppe(val id: String, val titel: String, val anbieter: String, val modelle: List<Modell>)

    /** Das geladene Angebot: Gruppen und die am PC abweichend gemerkten Effort-Stufen (effort-stufen.json). */
    class Angebot(alleGruppen: List<Gruppe>, val gemerkt: Map<String, List<String>>) {
        // Was OpenCode am Windows-PC gar nicht kennt, ließe sich nur auswählen, aber nicht starten.
        private val nichtStartbar = gemerkt[NICHT_STARTBAR].orEmpty().toSet()
        val gruppen: List<Gruppe> = alleGruppen.map { g -> g.copy(modelle = g.modelle.filter { it.modellString !in nichtStartbar }) }.filter { it.modelle.isNotEmpty() }
        val modelle: List<Modell> = gruppen.flatMap { it.modelle }

        /**
         * Die Effort-Stufen, die OpenLauncher dem Modell anbietet (LoadThinkingOptionsAsync): der Stand vom PC, sonst die feste Tabelle;
         * die aktuellen OpenAI-Modelle immer aus der Tabelle. Effort kommt hier vor dem CLI, deshalb gilt der erste gemerkte Stand seiner CLIs.
         */
        fun efforts(m: Modell): List<String> {
            if (OpenLauncherEfforts.immerFest(m.anbieter, m.slug)) return OpenLauncherEfforts.stufen(m.anbieter, m.slug)
            val schluessel = m.modellString
            val gemerkteStufen = m.clis.firstNotNullOfOrNull { gemerkt["$schluessel [${if (it == "claude") "claude-code" else it}]"] } ?: gemerkt[schluessel]
            return gemerkteStufen ?: OpenLauncherEfforts.stufen(m.anbieter, m.slug)
        }

        /** Prüft den Einstieg der Vollbildauswahl genau gegen das Angebot; null, wenn etwas nicht (mehr) passt. */
        fun wahlAus(f: ProgrammiererVorlage.FreieWahl): Wahl? {
            // Über den Slug: Anzeigenamen können sich beim Abgleich ändern oder doppelt vorkommen. Gleiche Slugs gibt es nur bei verschiedenen Anbietern.
            val m = modelle.firstOrNull { it.slug == f.slug && it.anbieterName == f.anbieter } ?: return null
            val stufen = efforts(m)
            val effort = if (f.effort == ProgrammiererVorlage.OHNE_EFFORT) "".takeIf { stufen.isEmpty() } else stufen.firstOrNull { OpenLauncherEfforts.name(it) == f.effort }
            val profil = OpenLauncherEfforts.PROFILE.firstOrNull { it.name == f.profil }?.wert
            val modus = OpenLauncherEfforts.MODI.firstOrNull { it.name == f.modus }?.wert
            val cli = m.clisMit(effort.orEmpty()).firstOrNull { CLI_NAMEN[it] == f.cli }
            return Wahl(m, effort ?: return null, profil ?: return null, modus ?: return null, cli ?: return null)
        }
    }

    /** Die Gruppen in Franks Reihenfolge: Anthropic, OpenAI, Moonshot, Google, danach die übrigen wie im OpenLauncher. */
    fun sortiert(gruppen: List<Gruppe>) = gruppen.sortedBy { g -> VORRANG.indexOf(g.anbieter.lowercase(Locale.ROOT)).let { if (it < 0) VORRANG.size else it } }
    fun anfangsOffen(g: Gruppe) = g.anbieter.lowercase(Locale.ROOT) in VORRANG

    data class Wahl(val modell: Modell, val effort: String, val profil: String, val modus: String, val cli: String) {
        /** Für Franks Ohren: „Kimi K3 (1M) in OpenCode, Effort High, Minimalprofil, Schnellmodus“. */
        fun beschreibung() = "${modell.name} in ${CLI_NAMEN[cli]}, " + (if (effort.isEmpty()) "ohne Effort" else "Effort ${OpenLauncherEfforts.name(effort)}") + ", ${PROFIL_NAMEN[profil]}, ${MODUS_NAMEN[modus]}"
    }

    sealed class Aufloesung {
        data class Start(val wahl: Wahl) : Aufloesung()
        /** [modell]: das schon eindeutige Modell, wenn nur noch eine Angabe fehlt (etwa das CLI). */
        data class Frage(val text: String, val modell: Modell? = null) : Aufloesung()
    }

    const val ANTHROPIC = "anthropic"
    /** Eintrag in effort-stufen.json: Modelle, die OpenCode am Windows-PC nicht kennt (siehe tools/effort-stufen-exportieren.ps1). */
    private const val NICHT_STARTBAR = "nicht-startbar"
    private const val MOONSHOT = "kimi-code-plan-global"
    private const val GOOGLE = "google"
    private val VORRANG = listOf(ANTHROPIC, "openai", MOONSHOT, GOOGLE)

    val CLI_NAMEN = linkedMapOf("claude" to "Claude Code", "codex" to "Codex CLI", "opencode" to "OpenCode", "kimi" to "Kimi Code CLI", "antigravity" to "Antigravity CLI")
    val PROFIL_NAMEN = linkedMapOf("minimal" to "Minimalprofil", "standard" to "Standardprofil", "strict" to "Striktprofil")
    val MODUS_NAMEN = OpenLauncherEfforts.MODI.associate { it.wert to it.name }
    /** Effort-Stufen, die OpenLauncher je CLI weitergibt; nur für Modelle, zu denen [OpenLauncherEfforts.stufen] nichts kennt. */
    private val EFFORTS = mapOf(
        "claude" to listOf("low", "medium", "high", "xhigh", "max"),
        "codex" to listOf("low", "medium", "high", "xhigh", "max", "ultra"),
        "opencode" to listOf("none", "minimal", "low", "medium", "high", "xhigh", "max", "ultra", "thinking"),
        "kimi" to listOf("low", "high", "max"),
        "antigravity" to listOf("low", "medium", "high", "xhigh", "max"),
    )

    /** Wie fillCliTargets im OpenLauncherMac: Claude-Modelle nur in Claude Code, sonst OpenCode plus das eigene CLI des Anbieters. */
    fun clisFuer(anbieter: String): List<String> = when (anbieter.lowercase(Locale.ROOT)) {
        ANTHROPIC -> listOf("claude")
        "openai" -> listOf("opencode", "codex")
        MOONSHOT -> listOf("opencode", "kimi")
        GOOGLE -> listOf("antigravity", "opencode")
        else -> listOf("opencode")
    }

    /** Franks Standards ohne Angabe (wie Opus und Sol über die PC-Brücke). */
    const val PROFIL_STANDARD = "minimal"
    const val MODUS_STANDARD = "schnell"
    const val EFFORT_STANDARD = "high"

    private fun effortName(e: String) = OpenLauncherEfforts.name(e)

    // ---- Laden (GitHub, zehn Minuten im Speicher) ----

    private val HTTP = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    @Volatile private var zwischen: Pair<Long, Angebot>? = null
    private const val HALTEN_MS = 10 * 60_000L

    suspend fun modelle(context: Context): List<Modell> = angebot(context).modelle

    suspend fun angebot(context: Context): Angebot = withContext(Dispatchers.IO) {
        zwischen?.takeIf { System.currentTimeMillis() - it.first < HALTEN_MS }?.let { return@withContext it.second }
        val e = Einstellungen.get(context.applicationContext)
        fun lies(pfad: String): String {
            val anfrage = Request.Builder().url("https://api.github.com/repos/${e.repoName}/contents/$pfad?ref=main")
                .header("Accept", "application/vnd.github.raw+json").header("X-GitHub-Api-Version", "2022-11-28").header("User-Agent", "Jarvis")
                .apply { if (e.githubToken.isNotBlank()) header("Authorization", "Bearer ${e.githubToken}") }.get().build()
            HTTP.newCall(anfrage).execute().use { a ->
                if (!a.isSuccessful) throw IllegalStateException("GitHub antwortet mit HTTP ${a.code} für $pfad")
                return a.body?.string().orEmpty()
            }
        }
        val (neueGruppen, neueStufen) = kotlinx.coroutines.coroutineScope {
            val g = async { runCatching { gruppenAus(lies("OpenLauncher/models.json")) } }
            val s = async { runCatching { stufenAus(lies("OpenLauncher/effort-stufen.json")) } }
            g.await() to s.await()
        }
        val alt = zwischen?.second
        val gruppen = neueGruppen.getOrElse { fehler -> alt?.gruppen ?: throw IllegalStateException("Das Angebot von OpenLauncher ist gerade nicht lesbar (${fehler.message}).") }
        // Ohne Datei gilt der letzte Stand, sonst die feste Tabelle von OpenLauncher.
        val gemerkt = neueStufen.getOrElse { alt?.gemerkt ?: emptyMap() }
        // Ein Teilausfall hält nicht zehn Minuten: In einer Minute wird neu gelesen.
        val zeit = System.currentTimeMillis() - if (neueGruppen.isSuccess && neueStufen.isSuccess) 0L else HALTEN_MS - 60_000L
        Angebot(gruppen, gemerkt).also { zwischen = zeit to it }
    }

    /** models.json (Gruppen mit Models); Profil, Modus und Effort sind Franks Standards, nicht model-defaults.json. */
    internal fun auslesen(modelsJson: String): List<Modell> = gruppenAus(modelsJson).flatMap { it.modelle }

    internal fun gruppenAus(modelsJson: String): List<Gruppe> {
        val gruppen = JSONArray(modelsJson)
        val liste = mutableListOf<Gruppe>()
        for (g in 0 until gruppen.length()) {
            val gruppe = gruppen.optJSONObject(g) ?: continue
            val modelle = gruppe.optJSONArray("Models") ?: continue
            val sichtbar = LinkedHashMap<String, Modell>()
            for (i in 0 until modelle.length()) {
                val m = modelle.optJSONObject(i) ?: continue
                if (m.optBoolean("IsHidden")) continue
                val anbieter = m.optString("ProviderId")
                val slug = m.optString("Slug")
                if (anbieter.isBlank() || slug.isBlank()) continue
                // Doppelte Slugs oder Gruppen ließen das Raster der Auswahl abstürzen.
                sichtbar.getOrPut(slug) { Modell(anbieter, slug, m.optString("DisplayName").ifBlank { slug }, m.optString("ProviderName").ifBlank { anbieter }) }
            }
            val erstes = sichtbar.values.firstOrNull() ?: continue
            liste += Gruppe(gruppe.optString("Id").ifBlank { "gruppe$g" }, gruppe.optString("Title").ifBlank { erstes.anbieterName }, erstes.anbieter, sichtbar.values.toList())
        }
        return liste.distinctBy { it.id }
    }

    internal fun stufenAus(json: String): Map<String, List<String>> {
        val o = JSONObject(json)
        val stufen = LinkedHashMap<String, List<String>>()
        // Eine unlesbare Zeile kostet nur diesen Eintrag.
        o.keys().forEach { k -> o.optJSONArray(k)?.let { a -> stufen[k] = (0 until a.length()).mapNotNull { a.optString(it).takeIf(String::isNotBlank) }.distinct() } }
        return stufen
    }

    // ---- Auflösen ----

    /** Kleinbuchstaben und Ziffern, ohne Klammerzusatz und ohne „Anbieter: “ davor (OpenRouter). */
    private fun kern(s: String) = s.lowercase(Locale.ROOT).replace(Regex("\\(.*?\\)"), "").substringAfter(": ").filter { it.isLetterOrDigit() }
    private fun voll(s: String) = s.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    /** „GPT Astra 6“ → gpt, astra, 6 (Buchstaben- und Zifferngruppen getrennt). */
    private fun teile(s: String) = Regex("[a-z]+|[0-9]+").findAll(s.lowercase(Locale.ROOT)).map { it.value }.toList()

    /** Anbieter mit eigenem Zugang (nicht OpenRouter, OpenCode Zen, NVIDIA oder lokal): bei mehreren Treffern zuerst. */
    private fun direkt(m: Modell) = m.anbieter.lowercase(Locale.ROOT) in setOf(ANTHROPIC, "openai", MOONSHOT, GOOGLE)

    /**
     * Gesprochene Formen von Modellnamen in Schreibweise: Die Spracherkennung macht aus „Kimi K3.1 Pro“ gern „Chemica 3.1 Pro“
     * oder „Kimika 3.1 Pro“, aus dem Buchstaben K vor einer Zahl ein „Ka“ („Kimi Ka 3.1 Pro“).
     */
    fun gesprochen(s: String): String = s.replace(KIMI_K, "Kimi K").replace(KA, "k")
    private val KIMI_K = Regex("(?i)\\b(?:che|ki|ke|chi)m+i?[ck]a\\s*(?=[0-9])")
    private val KA = Regex("(?i)\\bka(?=\\s*[0-9])")
    /** Namensteile, die viele Modelle teilen oder in gewöhnlichen Wörtern stecken („pro“ in „Programmierer“): kein Beleg für ein Modell. */
    private val ALLGEMEIN = setOf("gpt", "claude", "free", "for", "coding", "preview", "ultra", "flash", "pro", "mini")

    /**
     * Das Modell zu Franks Worten [anfrage]: genaue Treffer (Name, Slug) vor Buchstabendrehern („GPT-Astra 6“) vor Teilen
     * („Astra“). Je Stufe gewinnen Anbieter mit eigenem Zugang; bleiben mehrere übrig, ist es mehrdeutig, außer ein Treffer ist
     * die Grundform der anderen („Kimi 3.1 Pro“ → Kimi K3.1 Pro statt Kimi K3.1 Pro HighSpeed). Ein gesprochenes „Ka“ vor einer
     * Zahl ist der Buchstabe K („Kimi Ka 3.1 Pro“ → Kimi K3.1 Pro).
     */
    fun findeModell(alle: List<Modell>, roh: String): List<Modell> {
        val anfrage = gesprochen(roh)
        val q = kern(anfrage)
        if (q.isEmpty()) return emptyList()
        val sortiert = q.toList().sorted()
        val qTeile = teile(anfrage)
        val stufen = listOf<(Modell) -> Boolean>(
            { m -> kern(m.name) == q || voll(m.name) == q || voll(m.slug) == q || voll(m.slug.substringAfterLast('/')) == q },
            { m -> kern(m.name).toList().sorted() == sortiert },
            { m -> qTeile.isNotEmpty() && qTeile.all { t -> voll(m.name).contains(t) || voll(m.slug).contains(t) } },
        )
        for (passt in stufen) {
            val treffer = alle.filter(passt).distinctBy { it.anbieter + "/" + it.slug }
            if (treffer.isEmpty()) continue
            val auswahl = treffer.filter(::direkt).ifEmpty { treffer }
            val grundform = auswahl.filter { g -> auswahl.all { m -> teile(m.name).containsAll(teile(g.name)) } }
            return if (auswahl.size > 1 && grundform.size == 1) grundform else auswahl
        }
        return emptyList()
    }

    /** Ob Franks eigene Worte das Modell nennen (ein unterscheidender Namensteil wie „astra“, „kimi“, „fable“). */
    fun genannt(m: Modell, worte: String): Boolean {
        val g = gesprochen(worte)
        val w = voll(g)
        // Ganze Wortteile vergleichen: „soll“ nennt nicht Sol.
        val wt = teile(g).toSet()
        return teile(m.name).filter { it.length >= 3 && it !in ALLGEMEIN }.any { it in wt } ||
            voll(m.slug.substringAfterLast('/')).let { it.length >= 3 && w.contains(it) }
    }

    /** Ob Franks Worte den Modellnamen [modellText] enthalten (ein Namensteil ab drei Zeichen, etwa „kimi“, „astra“, „opus“, „sol“). */
    fun textGenannt(modellText: String, worte: String): Boolean {
        val g = gesprochen(worte)
        val wt = teile(g).toSet()
        val t = teile(gesprochen(modellText)).filter { it.length >= 3 && it !in ALLGEMEIN }
        return if (t.isEmpty()) voll(g).contains(voll(modellText)) else t.any { it in wt }
    }

    fun cliAus(text: String): String? {
        val t = voll(text)
        return when {
            t.isEmpty() -> null
            t.startsWith("claude") -> "claude"
            t.startsWith("codex") -> "codex"
            t.startsWith("opencode") -> "opencode"
            t.startsWith("kimi") -> "kimi"
            t.startsWith("antigravity") || t.startsWith("agy") -> "antigravity"
            else -> ""
        }
    }

    /** Ob [text] irgendwo ein CLI nennt („In OpenCode bitte“). */
    fun cliErwaehnt(text: String): Boolean = voll(text).let { t -> listOf("claudecode", "codex", "opencode", "kimi", "antigravity").any { t.contains(it) } }

    fun effortAus(text: String): String? {
        val t = voll(text)
        return when (t) {
            "" -> null
            "none", "aus", "kein", "keiner" -> "none"
            "minimal", "minimum" -> "minimal"
            "low", "niedrig", "gering", "leicht" -> "low"
            "medium", "mittel", "mid" -> "medium"
            "high", "hoch" -> "high"
            "xhigh", "extrahigh", "extrahoch", "sehrhoch", "veryhigh" -> "xhigh"
            "max", "maximal", "maximum" -> "max"
            "ultra" -> "ultra"
            "thinking" -> "thinking"
            else -> ""
        }
    }

    fun profilAus(text: String): String? {
        val t = voll(text).removeSuffix("profil")
        return when {
            t.isEmpty() -> null
            t.startsWith("minimal") -> "minimal"
            t.startsWith("standard") -> "standard"
            t.startsWith("strikt") || t.startsWith("strict") || t.startsWith("streng") -> "strict"
            else -> ""
        }
    }

    fun modusAus(text: String): String? {
        val t = voll(text).removeSuffix("modus").removeSuffix("mode")
        return when {
            t.isEmpty() -> null
            t.startsWith("frei") || t == "free" -> "frei"
            t.startsWith("schnell") || t == "fast" -> "schnell"
            t.startsWith("normal") -> "normal"
            t.startsWith("grundlich") || t.startsWith("gruendlich") || t.startsWith("gründlich") || t.startsWith("thorough") -> "gruendlich"
            else -> ""
        }
    }

    /**
     * Prüft Franks Angaben gegen das Angebot. Leere Angaben (null) nehmen Franks Standards (minimal, schnell, high); ein fehlendes
     * CLI bei mehreren möglichen, eine nicht erkannte Angabe ("") oder ein mehrdeutiges Modell wird zur Rückfrage mit den möglichen Werten.
     */
    fun aufloesen(alle: List<Modell>, modellText: String, effortText: String, profilText: String, modusText: String, cliText: String): Aufloesung {
        val treffer = findeModell(alle, modellText)
        if (treffer.isEmpty()) return Aufloesung.Frage("Ein Modell „$modellText“ bietet OpenLauncher nicht an. Möglich sind zum Beispiel: " +
            alle.filter(::direkt).joinToString(", ") { it.name } + ".")
        if (treffer.size > 1) return Aufloesung.Frage("„$modellText“ ist nicht eindeutig. Gemeint sein kann: " + treffer.take(8).joinToString(", ") { it.name } + ".")
        val m = treffer.single()
        val cliGenannt = cliAus(cliText)
        if (cliGenannt == null && m.clis.size > 1) return Aufloesung.Frage("${m.name} bietet OpenLauncher in " +
            m.clis.joinToString(" und ") { CLI_NAMEN.getValue(it) } + " an. Frag Frank, in welchem CLI es starten soll, und gib seine Wahl in cli mit.", m)
        val cli = cliGenannt ?: m.clis.single()
        if (cli !in m.clis) return Aufloesung.Frage("${m.name} läuft bei OpenLauncher nur in " + m.clis.joinToString(" oder ") { CLI_NAMEN.getValue(it) } + ".")
        val erlaubt = OpenLauncherEfforts.stufen(m.anbieter, m.slug).ifEmpty { EFFORTS.getValue(cli) }
        val effortGenannt = effortAus(effortText)
        val effort = effortGenannt ?: EFFORT_STANDARD.takeIf { it in erlaubt } ?: erlaubt.last()
        if (effort !in erlaubt) return Aufloesung.Frage("Den Effort „$effortText“ gibt es für ${m.name} in ${CLI_NAMEN[cli]} nicht. Möglich: " +
            erlaubt.joinToString(", ") { effortName(it) } + ".")
        if (cli !in m.clisMit(effort)) return Aufloesung.Frage("Effort ${effortName(effort)} läuft bei ${m.name} nur in " + m.clisMit(effort).joinToString(" oder ") { CLI_NAMEN.getValue(it) } + ".")
        val profil = profilAus(profilText) ?: PROFIL_STANDARD
        if (profil !in PROFIL_NAMEN) return Aufloesung.Frage("Das Profil „$profilText“ gibt es bei OpenLauncher nicht. Möglich: " + PROFIL_NAMEN.values.joinToString(", ") + ".")
        val modus = modusAus(modusText) ?: MODUS_STANDARD
        if (modus !in MODUS_NAMEN) return Aufloesung.Frage("Den Modus „$modusText“ gibt es bei OpenLauncher nicht. Möglich: " + MODUS_NAMEN.values.joinToString(", ") + ".")
        return Aufloesung.Start(Wahl(m, effort, profil, modus, cli))
    }
}

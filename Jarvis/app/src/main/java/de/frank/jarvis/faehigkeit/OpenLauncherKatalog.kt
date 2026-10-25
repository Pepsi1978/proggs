package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.data.Einstellungen
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Das Angebot von OpenLauncher für Programmieraufträge mit frei gewähltem Modell (Franks Wahl vom 25.10.2026: „Starte
 * KimiK3 in OpenCode mit Effort High“). Modelle und ihre Standards kommen aus dem Repo (OpenLauncher/models.json und
 * model-defaults.json, über GitHub gelesen), Profile, Modi und die CLIs je Anbieter sind wie im OpenLauncherMac fest
 * (MainViewModel: profiles, workModes, fillCliTargets). Franks Wahl vom 25.10.2026: Fehlen Profil, Modus oder Effort, gelten
 * minimal, schnell und high; bietet OpenLauncher dem Modell mehrere CLIs an und nennt Frank keins, fragt Jarvis nach. Ist eine
 * Angabe mehrdeutig oder nicht im Angebot, fragt Jarvis ebenfalls nach ([Aufloesung.Frage]) statt zu raten.
 */
object OpenLauncherKatalog {

    data class Modell(val anbieter: String, val slug: String, val name: String,
                      val profil: String, val modus: String, val effort: String, val cli: String) {
        /** Die CLIs, die OpenLauncher diesem Modell anbietet (erstes = Standard ohne gespeicherte Wahl). */
        val clis: List<String> get() = clisFuer(anbieter)
    }

    data class Wahl(val modell: Modell, val effort: String, val profil: String, val modus: String, val cli: String) {
        /** Für Franks Ohren: „Kimi K3 (1M) in OpenCode, Effort High, Minimalprofil, Schnellmodus“. */
        fun beschreibung() = "${modell.name} in ${CLI_NAMEN[cli]}, Effort ${effortName(effort)}, ${PROFIL_NAMEN[profil]}, ${MODUS_NAMEN[modus]}"
    }

    sealed class Aufloesung {
        data class Start(val wahl: Wahl) : Aufloesung()
        data class Frage(val text: String) : Aufloesung()
    }

    const val ANTHROPIC = "anthropic"
    private const val MOONSHOT = "kimi-code-plan-global"
    private const val GOOGLE = "google"

    val CLI_NAMEN = linkedMapOf("claude" to "Claude Code", "codex" to "Codex CLI", "opencode" to "OpenCode", "kimi" to "Kimi Code CLI", "antigravity" to "Antigravity CLI")
    val PROFIL_NAMEN = linkedMapOf("minimal" to "Minimalprofil", "standard" to "Standardprofil", "strict" to "Striktprofil")
    val MODUS_NAMEN = linkedMapOf("frei" to "Freimodus", "schnell" to "Schnellmodus", "normal" to "Normalmodus", "gruendlich" to "Gründlichkeitsmodus")
    /** Effort-Stufen, die OpenLauncher je CLI weitergibt (Kimi kennt nur drei, alles andere ließe der Launcher still fallen). */
    private val EFFORTS = mapOf(
        "claude" to listOf("low", "medium", "high", "xhigh", "max"),
        "codex" to listOf("minimal", "low", "medium", "high", "xhigh"),
        "opencode" to listOf("none", "minimal", "low", "medium", "high", "xhigh", "max"),
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

    private fun effortName(e: String) = when (e) { "xhigh" -> "Extra High"; else -> e.replaceFirstChar { it.uppercase() } }

    // ---- Laden (GitHub, zehn Minuten im Speicher) ----

    private val HTTP = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    @Volatile private var zwischen: Pair<Long, List<Modell>>? = null
    private const val HALTEN_MS = 10 * 60_000L

    suspend fun modelle(context: Context): List<Modell> = withContext(Dispatchers.IO) {
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
        val liste = runCatching { auslesen(lies("OpenLauncher/models.json"), runCatching { lies("OpenLauncher/model-defaults.json") }.getOrDefault("{}")) }
            .getOrElse { fehler -> zwischen?.second ?: throw IllegalStateException("Das Angebot von OpenLauncher ist gerade nicht lesbar (${fehler.message}).") }
        zwischen = System.currentTimeMillis() to liste
        liste
    }

    /** models.json (Gruppen mit Models) und model-defaults.json („anbieter/slug“ → ProfileId, WorkModeId, ThinkingValue, CliTargetId). */
    internal fun auslesen(modelsJson: String, standardsJson: String): List<Modell> {
        val standards = runCatching { JSONObject(standardsJson) }.getOrDefault(JSONObject())
        val gruppen = JSONArray(modelsJson)
        val liste = mutableListOf<Modell>()
        for (g in 0 until gruppen.length()) {
            val modelle = gruppen.optJSONObject(g)?.optJSONArray("Models") ?: continue
            for (i in 0 until modelle.length()) {
                val m = modelle.optJSONObject(i) ?: continue
                if (m.optBoolean("IsHidden")) continue
                val anbieter = m.optString("ProviderId")
                val slug = m.optString("Slug")
                if (anbieter.isBlank() || slug.isBlank()) continue
                val s = standards.optJSONObject("$anbieter/$slug")
                val clis = clisFuer(anbieter)
                val cli = s?.optString("CliTargetId")?.lowercase(Locale.ROOT)?.takeIf { it in clis } ?: clis.first()
                liste += Modell(anbieter, slug, m.optString("DisplayName").ifBlank { slug },
                    s?.optString("ProfileId")?.takeIf { it in PROFIL_NAMEN } ?: "minimal",
                    s?.optString("WorkModeId")?.takeIf { it in MODUS_NAMEN } ?: "schnell",
                    s?.optString("ThinkingValue")?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "high", cli)
            }
        }
        return liste
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
    fun gesprochen(s: String): String = s
        .replace(Regex("(?i)\\b(?:che|ki|ke|chi)m+i?[ck]a\\s*(?=[0-9])"), "Kimi K")
        .replace(Regex("(?i)\\bka(?=\\s*[0-9])"), "k")

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
        val w = voll(gesprochen(worte))
        val allgemein = setOf("gpt", "claude", "free", "for", "coding", "preview", "ultra", "flash", "pro", "mini")
        return teile(m.name).filter { it.length >= 3 && it !in allgemein }.any { w.contains(it) } ||
            voll(m.slug.substringAfterLast('/')).let { it.length >= 3 && w.contains(it) }
    }

    /** Ob Franks Worte den Modellnamen [modellText] enthalten (ein Namensteil ab drei Zeichen, etwa „kimi“, „astra“, „opus“, „sol“). */
    fun textGenannt(modellText: String, worte: String): Boolean {
        val w = voll(gesprochen(worte))
        val t = teile(gesprochen(modellText)).filter { it.length >= 3 && it !in setOf("gpt", "claude", "free") }
        return if (t.isEmpty()) w.contains(voll(modellText)) else t.any { w.contains(it) }
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
            m.clis.joinToString(" und ") { CLI_NAMEN.getValue(it) } + " an. Frag Frank, in welchem CLI es starten soll, und gib seine Wahl in cli mit.")
        val cli = cliGenannt ?: m.clis.single()
        if (cli !in m.clis) return Aufloesung.Frage("${m.name} läuft bei OpenLauncher nur in " + m.clis.joinToString(" oder ") { CLI_NAMEN.getValue(it) } + ".")
        val erlaubt = EFFORTS.getValue(cli)
        val effortGenannt = effortAus(effortText)
        val effort = effortGenannt ?: EFFORT_STANDARD.takeIf { it in erlaubt } ?: erlaubt.last()
        if (effort !in erlaubt) return Aufloesung.Frage("Den Effort „$effortText“ gibt es für ${m.name} in ${CLI_NAMEN[cli]} nicht. Möglich: " +
            erlaubt.joinToString(", ") { effortName(it) } + ".")
        val profil = profilAus(profilText) ?: PROFIL_STANDARD
        if (profil !in PROFIL_NAMEN) return Aufloesung.Frage("Das Profil „$profilText“ gibt es bei OpenLauncher nicht. Möglich: " + PROFIL_NAMEN.values.joinToString(", ") + ".")
        val modus = modusAus(modusText) ?: MODUS_STANDARD
        if (modus !in MODUS_NAMEN) return Aufloesung.Frage("Den Modus „$modusText“ gibt es bei OpenLauncher nicht. Möglich: " + MODUS_NAMEN.values.joinToString(", ") + ".")
        return Aufloesung.Start(Wahl(m, effort, profil, modus, cli))
    }
}

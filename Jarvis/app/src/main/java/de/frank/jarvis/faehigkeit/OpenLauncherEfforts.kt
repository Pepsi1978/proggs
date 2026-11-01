package de.frank.jarvis.faehigkeit

import java.util.Locale

/**
 * Was der Windows-OpenLauncher zur Wahl stellt. [stufen] folgt OpenCodeVariantCatalog.GetLauncherLevels, [name] und
 * [beschreibung] folgen MainViewModel.ToThinkingOption, [PROFILE] und [MODI] den Listen im Konstruktor von MainViewModel.
 * Ändert sich dort etwas, muss es hier mit. Einzige Abweichung: Claude Opus 5.5 kennt Windows nur aus seinem gemerkten Stand. Rein und ohne Android.
 */
object OpenLauncherEfforts {
    data class Eintrag(val wert: String, val name: String, val beschreibung: String)

    val PROFILE = listOf(
        Eintrag("minimal", "Minimal", "Frisches, leeres Profil zum Ausbauen"),
        Eintrag("standard", "Standard", "Bewährte globale und Projektregeln"),
        Eintrag("strict", "Strikt", "Mehr Kontrolle und Absicherung"),
    )
    val MODI = listOf(
        Eintrag("frei", "Freimodus", "Kein zusätzlicher Modus-Prompt"),
        Eintrag("schnell", "Schnellmodus", "Kleinster korrekter Eingriff"),
        Eintrag("normal", "Normalmodus", "Passend zu Risiko und Umfang"),
        Eintrag("gruendlich", "Gründlichkeitsmodus", "Randfälle und Härtung mitprüfen"),
    )

    /** Anzeigename wie im OpenLauncher, auch das kleingeschriebene „ultra“. */
    fun name(stufe: String) = when (stufe) {
        "xhigh" -> "X High"; "max" -> "Max"; "thinking" -> "Thinking"; "none" -> "None"; "minimal" -> "Minimal"
        "low" -> "Low"; "medium" -> "Medium"; "high" -> "High"; else -> stufe
    }

    fun beschreibung(stufe: String) = when (stufe) {
        "none" -> "aus"; "minimal" -> "sehr knapp"; "low" -> "leicht"; "medium" -> "empfohlen"; "high" -> "gründlich"
        "xhigh" -> "maximal hoch"; "max" -> "Maximum"; "thinking" -> "aktiviert"; else -> ""
    }

    private val BREIT = listOf("low", "medium", "high")
    private val GPT52 = listOf("none", "low", "medium", "high", "xhigh")
    private val AKTUELL = listOf("low", "medium", "high", "xhigh", "max", "ultra")
    private val LUNA = listOf("low", "medium", "high", "xhigh", "max")
    private val SOL_FAMILIE = setOf("gpt-6-astra", "gpt-6.1-sol", "gpt-6.1-terra", "gpt-5.6-terra")

    /** Die aktuellen OpenAI-Modelle zeigt OpenLauncher immer aus der festen Tabelle, nie aus einem gemerkten Stand. */
    fun immerFest(anbieter: String, slug: String) = anbieter.equals("openai", true) && (slug in SOL_FAMILIE || slug == "gpt-6-luna")

    fun stufen(anbieter: String, slugRoh: String): List<String> {
        val slug = slugRoh.trim().lowercase(Locale.ROOT).removePrefix("openrouter/")
        return when (anbieter.lowercase(Locale.ROOT)) {
            "openai" -> openAi(slug)
            "opencode" -> zen(slug)
            "nvidia" -> nvidia(slug)
            "anthropic" -> anthropic(slug)
            "kimi-code-plan-global" -> if (slug in setOf("k3.1-pro", "k3.1-pro-highspeed", "k3", "k3-256k", "kimi-for-coding")) listOf("low", "high", "max") else emptyList()
            "google" -> if (slug.startsWith("gemini-4-argon")) listOf("medium", "high") else BREIT
            "openrouter" -> openRouter(slug)
            else -> emptyList()
        }
    }

    private fun anthropic(roh: String): List<String> = when (roh.removeSuffix("[1m]")) {
        "claude-opus-5-5", "claude-opus-5", "claude-fable-5-1", "claude-fable-5", "claude-opus-4-8", "claude-opus-4-7", "claude-sonnet-5-5", "claude-sonnet-5", "claude-haiku-5-5", "claude-haiku-4-5" ->
            listOf("low", "medium", "high", "xhigh", "max")
        "claude-opus-4-6", "claude-sonnet-4-6" -> listOf("low", "medium", "high", "max")
        "claude-opus-4-5" -> BREIT
        else -> emptyList()
    }

    private fun openAi(s: String): List<String> = when {
        s in SOL_FAMILIE -> AKTUELL
        s == "gpt-6-luna" -> LUNA
        "-mini" in s || "-nano" in s -> emptyList()
        "-chat" in s -> listOf("medium")
        "pro" in s -> if ("gpt-5." in s) listOf("medium", "high", "xhigh") else listOf("high")
        "codex" in s -> if ("gpt-5.3" in s || "gpt-5.4" in s || "gpt-5.5" in s) GPT52 else BREIT
        s.startsWith("gpt-6") -> GPT52
        s.startsWith("gpt-5.1") -> listOf("none", "low", "medium", "high")
        s.startsWith("gpt-5.") -> GPT52
        s.startsWith("gpt-5") -> listOf("minimal", "low", "medium", "high")
        else -> emptyList()
    }

    private fun zen(s: String): List<String> = when (s) {
        "deepseek-v4-flash-free" -> listOf("low", "medium", "high", "max")
        "mimo-v2.5-free", "nemotron-3-ultra-free" -> BREIT
        "north-mini-code-free" -> listOf("none", "high")
        else -> emptyList()
    }

    private fun nvidia(s: String): List<String> = when (s) {
        "stepfun-ai/step-3.7-flash" -> listOf("minimal", "low", "medium", "high", "xhigh", "max")
        "openai/gpt-oss-120b", "openai/gpt-oss-20b" -> BREIT
        "mistralai/mistral-medium-3.5-128b" -> listOf("none", "high")
        "z-ai/glm-5.2", "minimaxai/minimax-m3", "google/gemma-4-31b-it", "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning", "nvidia/nemotron-3-nano-30b-a3b",
        "nvidia/llama-3.1-nemotron-ultra-253b-v1", "nvidia/llama-3.3-nemotron-super-49b-v1.5", "nvidia/llama-3.3-nemotron-super-49b-v1",
        "nvidia/nvidia-nemotron-nano-9b-v2", "nvidia/llama-3.1-nemotron-nano-8b-v1" -> listOf("none", "thinking")
        else -> emptyList()
    }

    private fun glm52(s: String) = "glm-5.2" in s || "glm-5-2" in s || "glm-5p2" in s

    private fun openRouter(s: String): List<String> {
        val bekannt = glm52(s) || s.startsWith("openai/gpt-6") || s.startsWith("openai/gpt-oss") || s.startsWith("openai/gpt-5") ||
            s.startsWith("deepseek/deepseek-v4") || s.startsWith("xiaomi/mimo-v2.5") || "nemotron-3-ultra" in s || "nemotron-3-nano-omni" in s ||
            "nemotron-3-super" in s || "laguna-m" in s || "lfm-2.5-1.2b-thinking" in s || "gemma-4-31b" in s
        if (!bekannt) return emptyList()
        if (glm52(s)) return listOf("high", "xhigh")
        if ("grok-3-mini" in s) return listOf("low", "high")
        if (s.startsWith("openai/") || "gpt" in s) return openAiKompatibel(s)
        if ("minimax" in s || "grok" in s) return emptyList()
        if ("deepseek-chat" in s || "deepseek-reasoner" in s || "deepseek-r1" in s || "deepseek-v3" in s || "glm" in s || "kimi" in s || "k2p" in s || "qwen" in s || "big-pickle" in s) return emptyList()
        return BREIT
    }

    private fun openAiKompatibel(id: String): List<String> {
        val s = id.removePrefix("openai/")
        return when {
            s in SOL_FAMILIE -> AKTUELL
            s == "gpt-6-luna" -> LUNA
            "-chat" in s -> listOf("medium")
            "pro" in s -> if ("gpt-5." in s) listOf("medium", "high", "xhigh") else listOf("high")
            "codex" in s -> if ("gpt-5.3" in s || "gpt-5.4" in s || "gpt-5.5" in s) GPT52 else listOf("low", "medium", "high", "xhigh")
            s.startsWith("gpt-6") -> GPT52
            s.startsWith("gpt-5.1") -> listOf("none", "low", "medium", "high")
            s.startsWith("gpt-5.") -> GPT52
            else -> listOf("none", "minimal", "low", "medium", "high", "xhigh")
        }
    }
}

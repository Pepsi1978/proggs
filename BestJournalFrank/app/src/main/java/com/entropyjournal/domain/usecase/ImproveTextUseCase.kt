package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.util.Constants
import com.entropyjournal.util.stripEmDashes
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImproveTextUseCase
@Inject
constructor(private val geminiApi: GeminiApi, private val encryptedPrefs: SharedPreferences) {
    companion object {
        private const val TEMPERATURE = 0.4f
        private const val MAX_OUTPUT_TOKENS = 8192
        private const val TRUNCATION_RATIO = 0.85
        private const val CHUNK_MAX_CHARS = 3500
    }

    /**
     * Prompt optimizer for the "Individuelle Analyse" custom-focus input field.
     * Rewrites the user's task description so the downstream analysis system
     * picks the right mode (analyse vs. recherche) and understands the intent.
     * Used exclusively by SettingsViewModel.improvePromptText — NOT for journal entries.
     */
    private fun buildCustomAnalysisOptimizerPrompt(userText: String): String =
        """
Du bist ein Prompt-Optimierer für ein Tagebuch-Analyse-System. Der Nutzer hat in seinem Eingabefeld "Individuelle Analyse" eine Aufgabe formuliert. Deine Aufgabe: Diesen Text so umschreiben, dass er als Auftrag im Ziel-System perfekt funktioniert.

KONTEXT DES ZIEL-SYSTEMS:
Der optimierte Text wird später einem zweiten KI-System als Auftrag gegeben. Dieses zweite System liest zuerst alle Tagebucheinträge des Nutzers als Kontext und führt dann den Auftrag aus. Je nach Formulierung arbeitet das Ziel-System in zwei möglichen Modi:
- ANALYSE-MODUS: Bleibt nah an den Einträgen, beschreibt Muster, fasst zusammen.
- RECHERCHE-MODUS: Geht aktiv über die Einträge hinaus, liefert neue Vorschläge, Alternativen, Empfehlungen.

Deine Formulierung entscheidet, in welchem Modus das Ziel-System arbeitet.

ARBEITSWEISE IN ZWEI SCHRITTEN:

SCHRITT 1 — INTENTION ERKENNEN:
Lies die Eingabe des Nutzers genau. Verstehe dabei:
- Was ist das Kern-Thema?
- Was will der Nutzer wirklich erreichen? Nur analysieren, was da ist? Oder will er Neues, also Alternativen, Ideen, Empfehlungen, Lösungen?
- Gibt es implizite Wünsche, die der Nutzer nicht ausgesprochen hat, die aber logisch dazugehören?
- Ist die Eingabe vage, stichwortartig oder ausführlich?

SCHRITT 2 — TEXT OPTIMIEREN:
Formuliere die Aufgabe neu, sodass:
- Sie klar, präzise und handlungsorientiert ist.
- Die passenden Trigger-Wörter für das Ziel-System verwendet werden.
- Der Kontext "Tagebucheinträge" klar erwähnt wird.
- Die Kern-Intention des Nutzers erhalten bleibt.
- Vage Stellen konkret gemacht werden.
- Fehlende, aber logische Aspekte ergänzt werden.

TRIGGER-WÖRTER FÜR DAS ZIEL-SYSTEM:

FÜR ANALYSE-AUFTRÄGE (nah an den Einträgen bleiben):
"analysiere", "fasse zusammen", "was fällt auf", "beschreibe Muster", "zeige Entwicklungen", "erkenne Zusammenhänge"

FÜR RECHERCHE- UND VORSCHLAGS-AUFTRÄGE (über die Einträge hinausgehen):
"recherchiere", "finde Alternativen", "schlage vor", "empfehle", "ergänze mit neuen Ideen", "liefere Optionen", "gib mir Möglichkeiten"

KOMBINIERT (Analyse und Vorschlag zusammen):
"analysiere und schlage vor", "erkenne Muster und empfehle", "fasse zusammen und gib Alternativen"

Wahl des Modus:
- Erwähnt der Nutzer Wörter wie "Alternativen", "Ideen", "Vorschläge", "Hilfe", "was kann ich tun", "wie kann ich ändern" = RECHERCHE-MODUS.
- Will der Nutzer nur "sehen", "verstehen", "wissen was ist" = ANALYSE-MODUS.
- In vielen Fällen passt eine KOMBINATION. Im Zweifel kombinieren.

AUSGABE-REGELN:
- Antworte NUR mit dem verbesserten Aufgabentext.
- Kein Vorspann wie "Hier ist..." oder "Die optimierte Version...".
- Keine Erklärung, keine Anführungszeichen, keine Backticks.
- Kein Markdown, kein JSON, keine Liste.
- Reiner Fließtext, direkt einfügbar in ein Eingabefeld.

LÄNGE:
- Keine feste Begrenzung nach oben oder unten.
- Formuliere so ausführlich, dass die Aufgabe perfekt und vollständig umgesetzt werden kann.
- Lieber ein paar Sätze mehr, die alle nötigen Hinweise liefern, als zu knapp.
- Keine Füllwörter, keine Wiederholungen, aber auch keine künstliche Kürzung.

STIL:
- Einfach, klar, direkt.
- Keine Fremdwörter.
- Keine langen Gedankenstriche. Nutze Kommas oder Punkte.
- Aus Nutzer-Sicht formuliert (also "meine Einträge", "mein Schlaf", nicht "deine Einträge").

INHALT:
- Kern-Intention des Nutzers erhalten.
- Vage Stellen konkret machen.
- Die Wörter "Tagebucheinträge" oder "Einträge" müssen mindestens einmal vorkommen, damit der Kontext klar ist.
- Bei Recherche-Aufträgen explizit formulieren, dass NEUE Inhalte, Alternativen oder Vorschläge gewünscht sind, die nicht schon in den Einträgen stehen.

BEISPIELE:

BEISPIEL 1 (vage Eingabe, Recherche-Intention):
Eingabe: "schau mal was ich so rauche und ob man das anders machen kann"
Optimiert: Analysiere meine Rauchgewohnheiten aus den Tagebucheinträgen, erkenne Auslöser und Muster. Schlage dann konkrete Alternativen zum Rauchen vor, die zu meinem Alltag passen. Liefere auch neue Ideen, die ich in meinen Einträgen noch nicht erwähnt habe.

BEISPIEL 2 (ein Stichwort, Analyse-Intention):
Eingabe: "Schlaf"
Optimiert: Analysiere meine Schlafqualität auf Basis der Tagebucheinträge. Erkenne Muster, Auslöser für schlechten Schlaf und positive Einflüsse. Fasse zusammen, wie sich mein Schlaf über die Zeit entwickelt hat.

BEISPIEL 3 (ausführliche Eingabe, gemischte Intention):
Eingabe: "ich will wissen wie es mir geht und was ich ändern sollte damit es besser wird"
Optimiert: Analysiere mein allgemeines Wohlbefinden aus den Tagebucheinträgen, erkenne wiederkehrende Themen und Stimmungsmuster. Schlage dann konkrete Veränderungen und neue Ansätze vor, die zu meiner Situation passen und mein Wohlbefinden verbessern könnten.

BEISPIEL 4 (neutrales Thema, reine Analyse):
Eingabe: "Essen"
Optimiert: Analysiere mein Essverhalten aus den Tagebucheinträgen. Beschreibe Muster, Vorlieben, Häufigkeiten und Auffälligkeiten. Fasse zusammen, was die Einträge über meine Ernährung zeigen.

BEISPIEL 5 (expliziter Alternativen-Fokus):
Eingabe: "Alternativen für Feierabend Sport"
Optimiert: Schau in meine Tagebucheinträge, welche Sportarten und Feierabend-Aktivitäten ich erwähne. Recherchiere dann Alternativen für Feierabend-Sport, die zu meinem Energielevel und Alltag passen. Bring auch neue Ideen ein, die ich noch nicht kenne.

BEISPIEL 6 (Problem-Eingabe):
Eingabe: "ich mache zu viel und bin immer erschöpft"
Optimiert: Analysiere aus meinen Tagebucheinträgen, wo meine größten Belastungspunkte liegen und was mich erschöpft. Schlage dann konkrete Wege vor, wie ich Aufgaben reduzieren, delegieren oder anders strukturieren kann. Gib auch neue Ideen für mehr Erholung im Alltag.

BEISPIEL 7 (unscharfer Wunsch):
Eingabe: "mehr Ruhe"
Optimiert: Analysiere meine Tagebucheinträge auf Momente und Situationen, in denen mir Ruhe fehlt oder gelingt. Schlage dann konkrete Wege vor, wie ich mehr Ruhe in meinen Alltag bringen kann, passend zu meiner Lebenssituation aus den Einträgen.

SPRACHE:
Antworte in der Sprache, in der der Nutzer seine Eingabe geschrieben hat. Bei unklarer Sprache oder gemischten Sprachen nutze Deutsch. Alle Texte im optimierten Prompt müssen in einer einzigen Sprache sein, keine Mischung.

EINGABE DES NUTZERS:
$userText
    """
            .trim()

    /**
     * Optimizes a custom-analysis focus text using the dedicated prompt optimizer.
     * Skips chunking and truncation-ratio checks because the optimizer output is
     * intentionally short (30-80 words) and must not be re-split.
     */
    suspend fun optimizeCustomAnalysisPrompt(rawText: String): Result<String> {
        return try {
            val apiKey = getApiKey()
            if (apiKey.isBlank())
                return Result.failure(IllegalStateException("Gemini API-Key nicht konfiguriert"))

            val request =
                GeminiRequestBuilder.build(
                    userText = buildCustomAnalysisOptimizerPrompt(rawText),
                    temperature = TEMPERATURE,
                    maxOutputTokens = MAX_OUTPUT_TOKENS,
                )
            val response =
                geminiApi.generateContent(
                    model = getSelectedModel(),
                    apiKey = getApiKey(),
                    request = request,
                )
            val improved = response.extractText()?.trim()?.stripEmDashes() ?: rawText
            Result.success(improved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(text: String): String =
        """
Du bist ein deutscher Textredakteur für diktierte Spracheingaben.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, präzise und sprachlich hochwertig** zum Ausdruck kommt.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdrücken?
2) Entferne Diktat-Artefakte: Fülllaute ("äh", "ähm", "öhm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
3) Formuliere Sätze so um, dass die erkannte Intention **klar und gut lesbar** wird.
   - Sätze dürfen umstrukturiert werden.
   - Wortwahl darf verbessert werden.
   - Satzgrenzen dürfen neu gesetzt werden.
4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.

GRENZEN (strikt):
- Keine neuen Informationen, Fakten oder Inhalte hinzufügen.
- Keine Vermutungen über nicht Gesagtes.
- Die Intention des Originals muss vollständig erhalten bleiben.
- Sprache: Deutsch.

${Constants.NO_EM_DASH_RULE}

REGEL:
Gib AUSSCHLIESSLICH den überarbeiteten Text zurück.
Keine Kommentare. Keine Erklärungen. Kein Präfix.

TEXT:
$text
    """
            .trim()

    private fun getSelectedModel(): String {
        return Constants.resolveValidModel(encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL))
    }

    private fun getApiKey(): String {
        return encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
    }

    private suspend fun rewriteChunk(text: String): String {
        val request =
            GeminiRequestBuilder.build(
                userText = buildPrompt(text),
                temperature = TEMPERATURE,
                maxOutputTokens = MAX_OUTPUT_TOKENS,
            )
        val response =
            geminiApi.generateContent(
                model = getSelectedModel(),
                apiKey = getApiKey(),
                request = request,
            )
        return response.extractText()?.trim()?.stripEmDashes() ?: text
    }

    /**
     * Split text into chunks by paragraphs, respecting sentence boundaries. Mirrors the
     * Tampermonkey chatgpt.user.js splitIntoChunksByParagraphs logic.
     */
    private fun splitIntoChunks(text: String): List<String> {
        if (text.length <= CHUNK_MAX_CHARS) return listOf(text)

        val paragraphs = text.split(Regex("\n{2,}"))
        val chunks = mutableListOf<String>()
        var buffer = ""

        for (paragraph in paragraphs) {
            val p = paragraph.trim()
            if (p.isEmpty()) continue

            if (p.length > CHUNK_MAX_CHARS) {
                if (buffer.isNotBlank()) {
                    chunks.add(buffer)
                    buffer = ""
                }
                // Split long paragraph at sentence boundaries
                var start = 0
                while (start < p.length) {
                    var end = minOf(start + CHUNK_MAX_CHARS, p.length)
                    if (end < p.length) {
                        val windowStart = maxOf(start, end - (CHUNK_MAX_CHARS / 2))
                        val slice = p.substring(windowStart, end)
                        val lastBreak =
                            maxOf(
                                slice.lastIndexOf('.'),
                                slice.lastIndexOf('!'),
                                slice.lastIndexOf('?'),
                                slice.lastIndexOf(';'),
                            )
                        if (lastBreak > -1) end = windowStart + lastBreak + 1
                    }
                    chunks.add(p.substring(start, end).trim())
                    start = end
                }
                continue
            }

            val candidate = if (buffer.isNotBlank()) "$buffer\n\n$p" else p
            if (candidate.length > CHUNK_MAX_CHARS) {
                if (buffer.isNotBlank()) chunks.add(buffer)
                buffer = p
            } else {
                buffer = candidate
            }
        }
        if (buffer.isNotBlank()) chunks.add(buffer)
        return if (chunks.isNotEmpty()) chunks else listOf(text)
    }

    suspend operator fun invoke(rawText: String): Result<String> {
        return try {
            val apiKey = getApiKey()
            if (apiKey.isBlank())
                return Result.failure(IllegalStateException("Gemini API-Key nicht konfiguriert"))

            // First attempt: one-shot
            val oneShot = rewriteChunk(rawText)
            val ratio = oneShot.length.toDouble() / maxOf(1, rawText.length)

            // If result is long enough, accept it
            if (ratio >= TRUNCATION_RATIO) {
                return Result.success(oneShot)
            }

            // Text was truncated — split into chunks and process in parallel
            val chunks = splitIntoChunks(rawText)
            val results = coroutineScope {
                chunks.map { chunk -> async { rewriteChunk(chunk) } }.map { it.await() }
            }

            Result.success(results.joinToString("\n\n").trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

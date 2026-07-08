package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.InsightRepository
import de.frank.entropyreducer.data.repository.MemoryRepository
import de.frank.entropyreducer.data.repository.PromptRepository
import de.frank.entropyreducer.data.safety.PhoneContentGuard
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.model.priorityBucketForScore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

/**
 * Wandelt einen rohen Transkript-Text in einen strukturierten EntropyEntry um.
 * Spec §9.1.
 */
class ProcessEntryUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val memories: MemoryRepository,
    private val prompts: PromptRepository,
    private val entries: EntryRepository,
    private val insights: InsightRepository,
    private val systemPromptBuilder: SystemPromptBuilder,
    private val json: Json,
    // Frank-Wunsch 2026-06-19: Prioritaets-Gedaechtnis fuer den Abgleich beim Einsprechen.
    private val priorityMemoryRepository:
        de.frank.entropyreducer.data.repository.PriorityMemoryRepository,
) {

    suspend operator fun invoke(
        rawTranscript: String,
        source: EntrySource,
        // Frank-Wunsch 2026-05-31: optionaler manuell eingetippter Titel aus dem
        // Review-Fenster. Wenn gesetzt (nicht leer), hat er Vorrang vor dem
        // KI-Titel und wird NICHT auf 3 Woerter gekappt — Frank bestimmt selbst.
        manualTitle: String? = null,
        // ID-Architektur Etappe 2d (Frank-Wunsch 2026-06-19): Herkunft, falls diese Aufgabe aus einem
        // Vorgaenger entsteht (z. B. einem angenommenen Aufgaben-Vorschlag). Default null = direkt
        // eingesprochene Aufgabe ohne Vorgaenger -> alle bisherigen Aufrufer bleiben unveraendert.
        originId: String? = null,
        originType: String? = null,
        rootId: String? = null,
    ): Result<EntropyEntryEntity> {
        val cleanManualTitle = manualTitle?.trim()?.takeIf { it.isNotBlank() }
        if (PhoneContentGuard.isSecondBrainWorkArtifact(cleanManualTitle, rawTranscript)) {
            return Result.failure(
                IllegalArgumentException(
                    "Interner Second-Brain-Arbeitsinhalt wird nicht als Aufgabe gespeichert."
                )
            )
        }
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()
        val activePrompts = prompts.getActiveByCategory(de.frank.entropyreducer.domain.model.PromptCategory.AUFGABEN).first()
        // Frank-Wunsch 2026-05-09: bestätigte Methoden des Insight Boards sollen
        // die Prio-Berechnung neuer Aufgaben beeinflussen — z.B. Laufen aktiviert
        // BDNF/NGF, baut mentale + körperliche + Schmerz-Entropie ab. Eine
        // Aufgabe die durch eine bestätigte Methode lösbar ist bekommt höhere Prio.
        val confirmed = insights.observeConfirmed().first()
        // Frank-Wunsch 2026-06-19: Prioritaets-Gedaechtnis laden (nur wenn aktiviert) — die neuesten
        // N Eintraege (einstellbares Limit). Gemini gleicht die neue Aufgabe spezifisch dagegen ab.
        val priorityMemories =
            if (settings.priorityMemoryEnabledFlow.first()) {
                priorityMemoryRepository.getNewest(settings.priorityMemoryLimitFlow.first())
            } else {
                emptyList()
            }

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = null,
            calendarToday = null,
            calendarTomorrow = null,
            userPrompts = activePrompts,
            tail = TAIL_INSTRUCTION,
            confirmedInsights = confirmed,
            priorityMemories = priorityMemories,
        )

        // Frank-Wunsch 2026-05-22 (dritte Iteration): KI lernt aus historischen
        // Manual-Werten. Bis zu 8 Beispiele werden als Kontext mitgeschickt,
        // damit aehnliche Aufgaben aehnliche Dauer bekommen.
        val durationSamples = runCatching {
            entries.getRecentManualDurationSamples(limit = 8)
        }.getOrDefault(emptyList())
        val userMessage = buildString {
            append("Hier ist meine gesprochene Notiz, transkribiert: ")
            append(rawTranscript)
            if (durationSamples.isNotEmpty()) {
                appendLine()
                appendLine()
                appendLine(formatDurationSamples(durationSamples))
            }
        }

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(userMessage)),
                        ),
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4,
                        responseMimeType = "application/json",
                    ),
                ),
            )

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.let { stripMarkdownCodeFence(it) }

            val parsed = rawJson?.let {
                runCatching {
                    json.decodeFromString(StructuredEntryDto.serializer(), it)
                }.getOrNull()
            }

            val entry = if (parsed != null) {
                val now = System.currentTimeMillis()
                EntropyEntryEntity(
                    id = UUID.randomUUID().toString(),
                    rawTranscript = rawTranscript,
                    title = cleanManualTitle ?: limitToThreeWords(parsed.title),
                    description = parsed.description,
                    category = runCatching { EntropyCategory.valueOf(parsed.category) }
                        .getOrDefault(EntropyCategory.SONSTIGES),
                    severity = parsed.severity.coerceIn(1, 10),
                    priorityScore = parsed.priorityScore.coerceIn(0.0, 100.0),
                    priorityReason = parsed.priorityReason,
                    status = EntryStatus.OFFEN,
                    timeBucket = priorityBucketForScore(parsed.priorityScore),
                    estimatedDurationMinutes = parsed.estimatedDurationMinutes,
                    createdAt = now,
                    updatedAt = now,
                    resolvedAt = null,
                    tags = parsed.tags,
                    aiNotes = parsed.aiNotes,
                    source = source,
                    biomarkerSnapshotId = null,
                    originId = originId,
                    originType = originType,
                    rootId = rootId,
                )
            } else {
                fallbackEntry(rawTranscript, source, cleanManualTitle, originId, originType, rootId)
            }
            if (
                PhoneContentGuard.isSecondBrainWorkArtifact(
                    entry.title,
                    entry.rawTranscript + "\n" + entry.description,
                )
            ) {
                return Result.failure(
                    IllegalArgumentException(
                        "Interner Second-Brain-Arbeitsinhalt wird nicht als Aufgabe gespeichert."
                    )
                )
            }
            entries.upsert(entry)
            // Frank-Wunsch 2026-06-19: Live-Logik-Checkpoint (Intent-Verifikation) — erwartet vs.
            // tatsaechlich: hat die KI eine Prio aus dem Gedaechtnis uebernommen?
            if (priorityMemories.isNotEmpty()) {
                val hit = entry.priorityReason.contains(
                    "aus frueherer aehnlicher Aufgabe",
                    ignoreCase = true,
                )
                Diag.i(
                    DiagnosticArea.APP,
                    "PrioMemory",
                    "CHECKPOINT match: neu='${entry.title.take(32)}' ok=$hit " +
                        "prio=${entry.priorityScore.toInt()} " +
                        (if (hit) "(uebernommen)" else "(kein Treffer)"),
                )
            }
            Result.success(entry)
        } catch (t: Throwable) {
            // Fallback-Eintrag mit OFFEN-Status speichern, damit der Nutzer ihn
            // später erneut bewerten lassen kann (Spec §19).
            val fallback =
                fallbackEntry(rawTranscript, source, cleanManualTitle, originId, originType, rootId)
            entries.upsert(fallback)
            Result.failure(t)
        }
    }

    private fun fallbackEntry(
        transcript: String,
        source: EntrySource,
        manualTitle: String? = null,
        originId: String? = null,
        originType: String? = null,
        rootId: String? = null,
    ): EntropyEntryEntity {
        val now = System.currentTimeMillis()
        return EntropyEntryEntity(
            id = UUID.randomUUID().toString(),
            rawTranscript = transcript,
            title = manualTitle ?: limitToThreeWords(transcript).ifBlank { "Eintrag" },
            description = transcript.ifBlank { "(leeres Transkript)" },
            category = EntropyCategory.SONSTIGES,
            severity = 5,
            priorityScore = 50.0,
            priorityReason = "KI-Verarbeitung fehlgeschlagen — bitte erneut bewerten lassen.",
            status = EntryStatus.OFFEN,
            timeBucket = priorityBucketForScore(50.0),
            estimatedDurationMinutes = null,
            createdAt = now,
            updatedAt = now,
            resolvedAt = null,
            tags = listOf("parse_fehler"),
            aiNotes = null,
            source = source,
            biomarkerSnapshotId = null,
            originId = originId,
            originType = originType,
            rootId = rootId,
        )
    }

    /**
     * Frank-Wunsch 2026-05-31: KI-Titel duerfen hoechstens 3 Woerter lang sein.
     * Vorher wurde nur auf 60 Zeichen gekappt, was die einzeilige Kachel mit
     * "fuenf Woerter + ..." abschnitt. Jetzt werden hart die ersten 3 Woerter
     * genommen (manuell eingegebene Titel werden NICHT hierdurch gekappt).
     */
    private fun limitToThreeWords(raw: String): String =
        raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" ")

    /**
     * Frank-Wunsch 2026-05-31: kuerzt einen bestehenden, zu langen Aufgaben-Titel
     * per KI auf maximal 3 praegnante Woerter (schoener als die mechanische
     * Erste-3-Woerter-Kappung). Wird von der einmaligen Titel-Migration
     * (TasksViewModel.maybeShortenLongTitles) genutzt. Gibt bei Erfolg den neuen
     * Titel zurueck; bei fehlendem Key/Netzfehler ein Result.failure — der Aufrufer
     * laesst den Titel dann unveraendert (kein mechanischer Fallback, Frank wollte KI).
     */
    suspend fun shortenTitle(entry: EntropyEntryEntity): Result<String> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val systemPrompt =
            "Du kuerzt Aufgaben-Titel auf das Wesentliche. Gib AUSSCHLIESSLICH einen neuen " +
                "Titel aus HOECHSTENS DREI praegnanten deutschen Woertern zurueck, der den Kern " +
                "der Aufgabe eindeutig zusammenfasst. Keine Fuellwoerter, kein Satz, keine " +
                "Anfuehrungszeichen, keine Erklaerung, kein Punkt am Ende — nur die Woerter."
        val userText = buildString {
            appendLine("Titel: ${entry.title}")
            if (entry.description.isNotBlank() && entry.description != entry.title) {
                appendLine("Beschreibung: ${entry.description.take(200)}")
            }
        }
        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(role = "user", parts = listOf(GeminiPart(userText))),
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.2),
                ),
            )
            val raw = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.let { stripMarkdownCodeFence(it) }
                ?.trim()
                ?.trim('"', '\'', '.', ' ')
            if (raw.isNullOrBlank()) {
                Result.failure(IllegalStateException("Leere KI-Antwort fuer Titel-Kuerzung"))
            } else {
                // Sicherheitsnetz: auch wenn die KI doch mehr als 3 Woerter liefert, hart kappen.
                Result.success(limitToThreeWords(raw))
            }
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Frank-Wunsch 2026-05-22 (dritte Iteration): formatiert eine Liste manuell
     * bestaetigter Dauer-Werte als Few-Shot fuer die Gemini-Anfrage. Die KI
     * sieht damit "Frank hat fuer XYZ 60 min angegeben" und nutzt das als
     * Referenzwert fuer aehnliche Aufgaben.
     */
    private fun formatDurationSamples(samples: List<EntropyEntryEntity>): String =
        buildString {
            appendLine("Bisherige manuell bestaetigte Zeitschaetzungen (aus Frank's Historie — nutze als Referenz fuer aehnliche Aufgaben):")
            samples.forEach { s ->
                val mins = s.estimatedDurationMinutes ?: return@forEach
                appendLine(
                    "- \"${s.title}\" (${s.category.name}): $mins min" +
                        if (s.description.isNotBlank() && s.description != s.title)
                            " — ${s.description.take(80)}"
                        else ""
                )
            }
        }

    private fun stripMarkdownCodeFence(s: String): String {
        val trimmed = s.trim()
        return when {
            trimmed.startsWith("```json") -> trimmed.removePrefix("```json").removeSuffix("```").trim()
            trimmed.startsWith("```") -> trimmed.removePrefix("```").removeSuffix("```").trim()
            else -> trimmed
        }
    }

    @Serializable
    data class StructuredEntryDto(
        val title: String,
        val description: String,
        val category: String,
        val severity: Int,
        val priorityScore: Double,
        val priorityReason: String,
        val timeBucket: String,
        val estimatedDurationMinutes: Int? = null,
        val tags: List<String> = emptyList(),
        val aiNotes: String? = null,
    )

    /**
     * Re-Bewertung eines bestehenden Eintrags mit der aktuellen priorityScore-Doktrin.
     * Frank-Wunsch 2026-05-09: nach Aenderung der Bewertungsregeln (Entropie-
     * Reduktions-Skala in 5 Farbbereichen) sollen alle bestehenden Aufgaben mit
     * der neuen Logik neu eingestuft werden, damit die farbige Prio-Zahl auch
     * bei alten Eintraegen stimmt. Es wird AUSSCHLIESSLICH priorityScore und
     * priorityReason aktualisiert — Title, Beschreibung, Kategorie, Tags und
     * timeBucket bleiben unveraendert (der Eintrag ist ja schon strukturiert).
     */
    suspend fun rescoreExisting(
        entry: EntropyEntryEntity,
        followupTexts: List<String> = emptyList(),
    ): Result<EntropyEntryEntity> {
        val key = secrets.geminiApiKey
            ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        val profile = settings.profileTextFlow.first()
        val activeMemories = memories.getActive().first()
        val activePrompts = prompts.getActiveByCategory(de.frank.entropyreducer.domain.model.PromptCategory.AUFGABEN).first()
        val confirmed = insights.observeConfirmed().first()

        val systemPrompt = systemPromptBuilder.build(
            basePrompt = RESCORE_BASE_PROMPT,
            profileText = profile,
            memories = activeMemories,
            biomarker = null,
            calendarToday = null,
            calendarTomorrow = null,
            userPrompts = activePrompts,
            tail = RESCORE_TAIL_INSTRUCTION,
            confirmedInsights = confirmed,
        )

        val now = System.currentTimeMillis()
        val entrySummary = buildString {
            appendLine("Bestehender Eintrag (zur Neubewertung):")
            appendLine("- Titel: ${entry.title}")
            appendLine("- Beschreibung: ${entry.description}")
            // Frank-Wunsch 2026-05-22 (vierte Iteration): Der vollstaendige
            // Originaltext fliesst in die Bewertung ein — die KI hat damit
            // mehr Kontext als nur die kurze Description.
            if (entry.rawTranscript.isNotBlank() && entry.rawTranscript != entry.description) {
                appendLine("- Vollstaendiger Text: ${entry.rawTranscript}")
            }
            appendLine("- Kategorie: ${entry.category.name}")
            appendLine("- Schwere (severity): ${entry.severity}/10")
            appendLine("- Bisheriger priorityScore: ${entry.priorityScore.toInt()}/100")
            appendLine("- Bisherige Begruendung: ${entry.priorityReason}")
            entry.estimatedDurationMinutes?.let {
                appendLine("- Geschaetzte Dauer: $it min")
            }
            // Frank-Wunsch 2026-05-22 (dritte Iteration): Frist (Deadline)
            // beeinflusst die Prio direkt. Kurze Restzeit = hoehere Prio.
            entry.dueAtMs?.let { due ->
                val df = java.text.SimpleDateFormat("d. MMM yyyy HH:mm", java.util.Locale.GERMAN)
                val remainingMs = due - now
                val hours = remainingMs / (60L * 60 * 1000)
                val days = hours / 24
                val remainingHuman = when {
                    remainingMs < 0 -> "UEBERFAELLIG"
                    days >= 2 -> "noch ca. $days Tage"
                    hours >= 24 -> "noch ca. 1 Tag (${hours} h)"
                    hours >= 1 -> "nur noch $hours h"
                    else -> "WENIGER ALS 1 STUNDE"
                }
                appendLine("- Frist: ${df.format(java.util.Date(due))} ($remainingHuman)")
            }
            // Frank-Wunsch 2026-05-22: Nachtraege MUESSEN in die Neubewertung
            // einfliessen. Wenn Frank z.B. einen Nachtrag spricht "ist jetzt
            // dringender geworden", muss die Prio entsprechend hoch.
            if (followupTexts.isNotEmpty()) {
                appendLine()
                appendLine("Nachtraege zu diesem Eintrag (chronologisch):")
                followupTexts.forEachIndexed { idx, text ->
                    appendLine("- Nachtrag ${idx + 1}: $text")
                }
            }
            // Frank-Wunsch 2026-05-22 (dritte Iteration): Few-Shot aus
            // bisherigen MANUELL bestaetigten Dauer-Werten. Hilft der KI
            // beim Schaetzen aehnlicher Aufgaben (z.B. "Wohnung putzen"
            // immer ~60 min wenn Frank das so eingestellt hat).
            val samples = runCatching {
                entries.getRecentManualDurationSamples(limit = 8)
                    .filter { it.id != entry.id }
            }.getOrDefault(emptyList())
            if (samples.isNotEmpty()) {
                appendLine()
                appendLine(formatDurationSamples(samples))
            }
        }

        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(entrySummary)),
                        ),
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.2,
                        responseMimeType = "application/json",
                    ),
                ),
            )

            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.let { stripMarkdownCodeFence(it) }

            val parsed = rawJson?.let {
                runCatching {
                    json.decodeFromString(RescoreDto.serializer(), it)
                }.getOrNull()
            } ?: return Result.failure(IllegalStateException("Re-Score-Antwort nicht parsebar"))

            // Frank-Wunsch 2026-05-22 (zweite Iteration): KI darf den geschaetzten
            // Zeitaufwand bei Rescore ebenfalls aktualisieren — ABER nur wenn der
            // Wert noch nicht manuell gesetzt wurde (durationManuallySet = false).
            // Manuelle Setzungen haben immer Vorrang.
            val nextDuration: Int? =
                if (entry.durationManuallySet) entry.estimatedDurationMinutes
                else parsed.estimatedDurationMinutes ?: entry.estimatedDurationMinutes

            // Frank-Wunsch 2026-05-22 (dritte Iteration): Clientseitiger Frist-Floor.
            // Auch wenn die KI eine niedrige Prio liefert, ueberschreiben wir bei
            // sehr knapper Frist auf einen Mindestwert. Das macht die App robust
            // gegenueber KI-Ausreissern und gibt Frank eine berechenbare Garantie:
            // <24h zur Frist = Prio mindestens 95.
            val baseScore = parsed.priorityScore.coerceIn(0.0, 100.0)
            val finalScore = applyDeadlineFloor(baseScore, entry.dueAtMs)

            val updated = entry.copy(
                priorityScore = finalScore,
                priorityReason = parsed.priorityReason,
                estimatedDurationMinutes = nextDuration,
                timeBucket = priorityBucketForScore(entry.manualPriorityScore ?: finalScore),
                updatedAt = System.currentTimeMillis(),
            )
            entries.update(updated)
            Result.success(updated)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    @Serializable
    data class RescoreDto(
        val priorityScore: Double,
        val priorityReason: String,
        /**
         * Frank-Wunsch 2026-05-22: KI-Dauer-Schaetzung. null = KI weiss nicht, dann
         * bleibt der alte Wert stehen. Nur uebernommen wenn durationManuallySet=false.
         */
        val estimatedDurationMinutes: Int? = null,
    )

    /**
     * Frank-Wunsch 2026-05-22 (dritte Iteration): Frist-basierte Prio-Untergrenze.
     * Auch wenn die KI eine niedrige Prio liefert, wird sie bei kurzer Restzeit
     * angehoben — Frank's Vorgabe: "wenn nur noch ein Tag, fast 100%".
     *
     * Stufen:
     *  - ueberfaellig (dueAtMs < now)         → mindestens 98
     *  - weniger als 24h                       → mindestens 95
     *  - 1-2 Tage Restzeit                     → mindestens 85
     *  - 2-3 Tage Restzeit                     → mindestens 75
     *  - 3-7 Tage Restzeit                     → mindestens 65
     *  - mehr als 7 Tage / keine Frist         → kein Floor, KI-Wert gilt
     */
    private fun applyDeadlineFloor(baseScore: Double, dueAtMs: Long?): Double =
        de.frank.entropyreducer.domain.usecase.computeDeadlineFloor(baseScore, dueAtMs)

    companion object {
        private const val PRIORITY_DOCTRINE = """
priorityScore-Doktrin (Frank-Spezifikation 2026-05-09):

DEFINITION VON PERSOENLICHER ENTROPIE:
Persoenliche Entropie ist alles, was Franks Bewusstseinsraum verwaessert,
verlangsamt oder belastet — also alles, was innere Kohaerenz stoert.
Frank versteht sein gesamtes Dasein als Bewusstseinsraum; Entropie ist
jede Form von Information, die in diesem Raum als Last erscheint, ohne
zur Ordnung beizutragen. Konkret aeussert sich persoenliche Entropie
als:
  - mentale Unruhe: Gedankenkreisen, Sorgen, Entscheidungslast,
    Optionsueberflutung, fragmentierte Aufmerksamkeit
  - koerperlicher Schmerz, Dysfunktion, Muedigkeit, Erschoepfung
  - emotionale Reibung: ungeloeste Konflikte, Frust, Druck, Anspannung
  - Zeitlasten: aufgeschobene Aufgaben, "loose ends", vergessene
    Verpflichtungen
  - Inkohaerenz: Diskrepanz zwischen gefuehltem Ist und gewuenschtem
    Sein
  - Reizueberflutung, sensorische Belastung, vegetative Dysregulation

Entropie wird REDUZIERT, wenn ein bestehender belastender Zustand
aufgeloest wird (Schmerz weg, Sorge geklaert, Entscheidung getroffen,
Termin erledigt, Unklarheit beseitigt). Entropie wird NICHT reduziert,
wenn lediglich ein Wunschzustand zusaetzlich realisiert wird, ohne
dass etwas Belastendes verschwindet (z.B. neues Hobby starten, neue
Reise planen — das sind Lebensqualitaets-Boosts, kein Entropie-Abbau).

Wellbeing-First-Prinzip: mentales und psychisches Wohlbefinden hat
den hoechsten Hebel. Eine Aufgabe, die innere Ruhe, Klarheit oder
Sicherheit schafft, wirkt staerker auf den Score als eine Aufgabe,
die nur einen rein technischen Zustand verbessert.

WAS DER SCORE MISST:
Der priorityScore misst, wieviel persoenliche Entropie durch die
Erledigung dieser Aufgabe entfernt wuerde. Drei Achsen bestimmen den
Score gemeinsam:
  1. Schwere der entfernten Last (wie sehr belastet es jetzt?)
  2. Breite der Wirkung (wieviele Lebensbereiche profitieren?)
  3. Kohaerenz-Effekt (wird durch die Erledigung innerlich etwas
     freigesetzt — Energie, Klarheit, Ruhe, mentale Bandbreite?)

severity (1-10) ist die rohe Schwere des Problems isoliert betrachtet.
priorityScore (0-100) ergibt sich aus severity x Breite x Kohaerenz-
Effekt, gewichtet mit dem Wellbeing-First-Prinzip. Daher gilt:
  - hohe severity + abgegrenzter Mikro-Bereich  = mittlerer/niedriger Score
  - mittlere severity + Wirkung ueber mehrere Bereiche = hoher Score
  - hohe severity + breite Wirkung + Wellbeing-Hebel = sehr hoher Score

SKALA IN FUENF FARBSTUFEN:

  90-100 (Rot, sehr wichtig):    Aufgabe entfernt fast die gesamte
                                  aktuelle persoenliche Entropie auf
                                  einmal — das eine Ding, das gerade
                                  quer ueber alle Lebensbereiche
                                  blockiert. Selten. Nur vergeben, wenn
                                  die Erledigung wirklich vieles auf
                                  einmal aufloest.
                                  Beispiel: chronischer Zahnschmerz mit
                                  freiliegendem Nerv ziehen lassen —
                                  entfernt koerperlichen Schmerz, mentale
                                  Last, emotionale Sorge und praeventives
                                  Risiko gleichzeitig.

  80-89  (Rot, sehr wichtig):    Aufgabe entfernt sehr grosse Mengen
                                  Entropie in mehreren Lebensbereichen
                                  gleichzeitig.
                                  Beispiel: schmerzhaften Leberfleck am
                                  Ruecken haut-aerztlich abklaeren und
                                  entfernen — koerperliche Last weg,
                                  mentale Sorge weg, praeventive
                                  Sicherheit.

  60-79  (Orange):                Aufgabe entfernt grosse Entropie in
                                  einem klaren Lebensbereich, plus
                                  Streueffekte in Nachbarbereichen.
                                  Beispiel: Notfallset und Notstrom-
                                  System im Auto fertig packen — loest
                                  Unsicherheits-Last auf, ermoeglicht
                                  spontane Schweden-Fahrten, schafft
                                  mentale Ruhe.

  40-59  (Gelb):                  Aufgabe entfernt mittlere Entropie,
                                  fokussiert auf einen klaren Bereich.
                                  Beispiel: grosses Blutbild machen
                                  lassen — klaert mehrere offene
                                  Hypothesen auf einmal, primaer im
                                  Gesundheitsbereich.

  20-39  (Blau):                  Aufgabe entfernt nur kleine Entropie
                                  in einem klaren Bereich. Wichtig,
                                  aber nicht draengend.
                                  Beispiel: Mountainbike aus der
                                  Wohnung verkaufen — kleiner physischer
                                  Raumgewinn, klar abgegrenzt.

  0-19   (Gruen, geringste Prio): Aufgabe entfernt nur einen winzigen
                                  Teil Entropie in einem speziellen,
                                  abgegrenzten Bereich. Auch: Aufgaben,
                                  die reine Wunschzustaende hinzufuegen,
                                  ohne Bestehendes aufzuloesen, gehoeren
                                  hierher.
                                  Beispiel: neue Suno-Songkategorie
                                  ausprobieren — kreative Bereicherung,
                                  aber kein aktueller Entropie-Abbau.

WICHTIGE REGELN:

- Score basiert auf ENTFERNTER Entropie, NICHT auf Schweregrad allein.
  Eine hohe severity bedeutet nicht automatisch hohe priorityScore —
  wenn die Aufgabe nur einen kleinen Bereich beeinflusst, bleibt der
  Score niedrig trotz hoher severity.

- Wellbeing-First: mentale/psychische Entlastung gewichtet staerker
  als rein technische/aeussere Verbesserung. Eine Aufgabe, die innere
  Ruhe schafft, kann auch bei mittlerer severity hoch eingestuft
  werden.

- Aufwand-Nutzen-Hebel: bei sonst gleicher Entropie-Reduktion hebt
  niedriger Aufwand (estimatedDurationMinutes) den Score leicht an,
  weil schnelle Erledigung den Bewusstseinsraum sofort entlastet.
  Hoher Aufwand senkt den Score nicht, wenn die entfernte Entropie
  gross ist — aber "quick wins" mit grosser Wirkung sind besonders
  wertvoll.

- Wunsch-Hinzufuegung ist kein Entropie-Abbau: Aufgaben, die nur ein
  neues Ziel/Hobby/Erlebnis ergaenzen, ohne dass eine bestehende
  Belastung verschwindet, bleiben im Gruen/Blau-Bereich, auch wenn
  sie persoenlich attraktiv sind.

- Modulatoren: Nutzer-Prompts, Biomarker (z.B. niedrige Whoop-
  Recovery, niedrige HRV, schlechter Schlaf in den letzten Tagen) und
  Kalender-Verfuegbarkeit modulieren den Score. Wenn die Aufgabe
  direkt einen schwachen Biomarker adressiert, hebe den Score an.
  Wenn der Kalender heute keine Zeit erlaubt und die Aufgabe nicht
  zeitkritisch ist, senke leicht.

- Im Zweifel konservativer einstufen — Rot soll Frank den Atem
  stocken lassen ("oh, das ist wirklich wichtig"), nicht inflationaer
  sein. Realistisch sollten nur etwa 5-10 Prozent aller Eintraege im
  Rot-Bereich landen.

- BESTAETIGTE-METHODEN-HEBEL (Frank-Wunsch 2026-05-09): Wenn die neue
  Aufgabe durch eine bestaetigte Methode aus dem Insight-Board loesbar
  oder unterstuetzbar ist (Match auf Title, Beschreibung oder Tags),
  hebe priorityScore an:
    - +5 bis +10 wenn die Methode eine einzelne Kategorie adressiert
    - +10 bis +15 wenn die Methode 2 Kategorien adressiert (primary +
      additional)
    - +15 bis +20 wenn die Methode 3 oder mehr Kategorien gleichzeitig
      adressiert (Wellbeing-First-Hebel — eine Methode die mental,
      koerperlich, emotional und gesundheitlich gleichzeitig wirkt
      ist sehr wertvoll und macht Aufgaben die sie nutzen koennen
      hoch-prior).
  Beispiel: bestaetigte Methode "Lauftraining" wirkt MENTAL (BDNF,
  Klarheit) + KOERPERLICH (Ausdauer) + GESUNDHEITLICH (Symptom-
  reduktion) + EMOTIONAL (Stimmung) — eine neue Aufgabe wie "regel-
  maessig laufen gehen" oder "Lauftraining heute einbauen" bekommt
  durch diesen Hebel +15 bis +20 Punkte. Begruendung in
  priorityReason explizit erwaehnen ("durch bestaetigte Methode X
  loesbar, mehrfach-kategorial").

- PRIORITAETS-GEDAECHTNIS-ABGLEICH (Frank-Wunsch 2026-06-19): Oben kann ein
  Abschnitt "Prioritaets-Gedaechtnis" mit frueher manuell gesetzten Prioritaeten
  stehen. Wenn die neue Aufgabe inhaltlich QUASI DASSELBE Vorhaben beschreibt wie
  ein Eintrag dort (z.B. "laufen gehen" entspricht "Lauftraining im Wald"),
  uebernimm dessen Prioritaet als priorityScore und schreibe in priorityReason
  WORTWOERTLICH den Marker: aus frueherer aehnlicher Aufgabe uebernommen.
  Vergleiche hauptsaechlich die Beschreibung, auch den Titel. NUR thematisch
  verwandt reicht NICHT (Laufen ist NICHT Federball; "Sport" ist KEIN gemeinsamer
  Nenner; "Einkaufen" ist NICHT "Kochen"). Die Aehnlichkeit muss spezifisch und
  hoch sein. Im Zweifel: KEIN Treffer, normal bewerten.
"""

        private const val BASE_PROMPT =
            "Deine Aufgabe: Wandle die folgende gesprochene Notiz des Nutzers in einen strukturierten Entropie-Eintrag um."

        private val TAIL_INSTRUCTION = """
Antworte AUSSCHLIESSLICH in JSON, ohne Markdown-Codeblock, ohne Einleitung, ohne Schluss:

{
  "title": "Maximal 3 Woerter, die den Kern der Aufgabe eindeutig zusammenfassen (z.B. 'Steuererklaerung abgeben', 'Arzttermin vereinbaren'). Keine Fuellwoerter, kein Satz — nur die 3 praegnantesten Woerter.",
  "description": "Strukturierte Beschreibung in 1-3 Saetzen",
  "category": "KOERPERLICH | MENTAL | ZEITLICH | EMOTIONAL | GESUNDHEITLICH | UMGEBUNG | SONSTIGES",
  "severity": 1-10,
  "priorityScore": 0.0-100.0,
  "priorityReason": "Begruendung in 1 Satz — bezieht sich konkret auf die Entropie-Reduktion",
  "timeBucket": "HEUTE | MORGEN | FREIBLOCK | GERING | SPAETER",
  "estimatedDurationMinutes": null,
  "tags": ["tag1","tag2"],
  "aiNotes": null
}

severity ist die rohe Schwere des Problems (1-10).
timeBucket wird aus priorityScore abgeleitet: HEUTE=80-100, MORGEN=60-79, FREIBLOCK=40-59, GERING=20-39, SPAETER=0-19.
$PRIORITY_DOCTRINE
        """.trimIndent()

        private const val RESCORE_BASE_PROMPT =
            "Deine Aufgabe: Bewerte einen bestehenden Entropie-Eintrag NEU nach der aktualisierten priorityScore-Doktrin. Du aenderst KEINE inhaltlichen Felder (Titel/Beschreibung/Kategorie), aktualisiere priorityScore, priorityReason und (falls noch nicht manuell gesetzt) estimatedDurationMinutes. WICHTIG: wenn eine Frist (Deadline) gesetzt ist, MUSS die Prio entsprechend der Restzeit hoch sein — kurze Restzeit ist ein dominanter Prio-Treiber."

        private val RESCORE_TAIL_INSTRUCTION = """
Antworte AUSSCHLIESSLICH in JSON, ohne Markdown-Codeblock, ohne Einleitung, ohne Schluss:

{
  "priorityScore": 0.0-100.0,
  "priorityReason": "Begruendung in 1 Satz — bezieht sich konkret auf die Entropie-Reduktion",
  "estimatedDurationMinutes": null oder Ganzzahl (geschaetzte Dauer in Minuten)
}

Hinweis zu estimatedDurationMinutes:
- Schaetze die realistische Bearbeitungsdauer der Aufgabe in Minuten.
- Kleine Aufgaben (Anruf, kurze Nachricht): 5-15 min
- Mittlere Aufgaben (Termin, Dokument schreiben): 30-120 min
- Komplexe Projekte (Recherche, mehrtaegige Arbeit): in Stunden bis Tagen
  (1 Tag = 1440 Minuten, 1 Woche = 10080 Minuten)
- Bei Unklarheit lieber null — kein Raten.
- Falls Nachtraege Hinweise auf neue Komplexitaet/Dringlichkeit geben, passe die
  Schaetzung an.

$PRIORITY_DOCTRINE
        """.trimIndent()
    }
}

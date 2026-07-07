package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity

/**
 * Reine, testbare Logik fuer das Prioritaets-Gedaechtnis (Frank-Wunsch 2026-06-19).
 * Bewusst frei von Android-/Room-Abhaengigkeiten, damit sie als JVM-Unit-Test abgesichert werden kann.
 */

/** Formatiert das Prioritaets-Gedaechtnis als Kontextblock fuer den Gemini-Prompt. */
fun formatPriorityMemoriesForPrompt(memories: List<PriorityMemoryEntity>): String =
    memories.joinToString("\n") { m ->
        "- \"${m.title}\" | ${m.description} | Prioritaet ${m.priority.toInt()}"
    }

/**
 * Waehlt den zu aktualisierenden Gedaechtnis-Eintrag (Dedup beim Lernen):
 * zuerst exakt dieselbe Aufgabe (sourceEntryId), sonst gleicher Titel (getrimmt, case-insensitive),
 * sonst null (= neuer Eintrag wird angelegt). Die *semantische* Aehnlichkeit macht spaeter Gemini
 * beim Abgleich — diese lokale Dedup verhindert nur exakte Duplikate.
 */
fun selectMemoryToUpdate(
    existing: List<PriorityMemoryEntity>,
    sourceEntryId: String,
    title: String,
): PriorityMemoryEntity? =
    existing.firstOrNull { it.sourceEntryId == sourceEntryId }
        ?: existing.firstOrNull { it.title.trim().equals(title.trim(), ignoreCase = true) }

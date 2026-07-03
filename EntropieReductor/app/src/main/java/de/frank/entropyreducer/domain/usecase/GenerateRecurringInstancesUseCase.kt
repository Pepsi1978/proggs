package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.dao.EntropyEntryFollowupDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.RecurringTemplateRepository
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule

/**
 * Generiert beim App-Start (oder manuell) fuer alle aktiven RecurringTemplates
 * die seit dem letzten Lauf faelligen Eintraege. Idempotent — mehrfache Aufrufe
 * am gleichen Tag erzeugen keine Duplikate (deterministische ID-Bildung aus
 * templateId + occurrenceMs).
 *
 * Frank-Wunsch 2026-05-22: Wiederkehrende Aufgaben mit voller RFC-5545-Recurrence.
 * Library: org.dmfs:lib-recur.
 */
@Singleton
class GenerateRecurringInstancesUseCase @Inject constructor(
    private val templateRepo: RecurringTemplateRepository,
    private val entryRepo: EntryRepository,
    private val followupDao: EntropyEntryFollowupDao,
) {
    /**
     * Laeuft alle aktiven Vorlagen durch, erzeugt fuer jedes seit lastGeneratedAt
     * faellige Vorkommen eine EntropyEntryEntity, und aktualisiert die Vorlage.
     *
     * @return Anzahl tatsaechlich erzeugter neuer Eintraege.
     */
    suspend operator fun invoke(): Int {
        val now = System.currentTimeMillis()
        val templates = templateRepo.getActive()
        var totalCreated = 0

        for (template in templates) {
            try {
                val created = processSingleTemplate(template, now)
                totalCreated += created
            } catch (e: Exception) {
                // Eine fehlerhafte Vorlage darf nicht alle anderen blockieren.
                Diag.w(DiagnosticArea.TASKS, 
                    TAG,
                    "Vorlage '${template.title}' (${template.id}) uebersprungen: ${e.message}",
                )
            }
        }

        if (totalCreated > 0) {
            Diag.i(DiagnosticArea.TASKS, TAG, "Wiederkehrende Aufgaben: $totalCreated neue Eintraege erzeugt.")
        }
        return totalCreated
    }

    private suspend fun processSingleTemplate(
        template: RecurringTemplateEntity,
        now: Long,
    ): Int {
        val rule = RecurrenceRule(template.rrule)
        // Startpunkt: lastGeneratedAt falls schon mal gelaufen, sonst createdAt.
        val startMs = if (template.lastGeneratedAt > 0L) {
            template.lastGeneratedAt
        } else {
            template.createdAt
        }

        // Iterator ab Start. Falls Vorlage neu (lastGeneratedAt=0), erzeugen wir
        // BEIM ersten Lauf KEINE rueckwirkenden Eintraege — nur ab "ab jetzt".
        // Faengt der Iterator beim Start an, springen wir auf now() vor und
        // erzeugen ab dem naechsten echten Vorkommen.
        val iterStart = DateTime(startMs)
        val iter = rule.iterator(iterStart)

        // Wenn die Vorlage NEU ist (noch nie gelaufen): fastForward bis NOW
        // damit wir keine rueckwirkenden Eintraege erstellen.
        val rewindBoundary = if (template.lastGeneratedAt == 0L) now else startMs + 1
        if (rewindBoundary > startMs) {
            iter.fastForward(DateTime(rewindBoundary))
        }

        var created = 0
        var lastSeenMs = startMs

        // Maximal 50 Eintraege pro Vorlage pro Lauf — Schutz gegen lange Pausen
        // bei taeglichen Vorlagen (z.B. App nach 2 Monaten geoeffnet).
        val limit = 50
        while (iter.hasNext() && created < limit) {
            val occurrenceMs = iter.nextMillis()
            if (occurrenceMs > now) break
            if (template.untilEpochMs != null && occurrenceMs > template.untilEpochMs) break

            val entry = buildEntry(template, occurrenceMs)
            entryRepo.upsert(entry)
            created++
            lastSeenMs = occurrenceMs
        }

        // Naechstes Vorkommen nach now() berechnen (fuer Anzeige in der UI).
        val nextOccurrence: Long? = if (iter.hasNext()) {
            iter.nextMillis().takeIf {
                template.untilEpochMs == null || it <= template.untilEpochMs
            }
        } else {
            null
        }

        templateRepo.upsert(
            template.copy(
                lastGeneratedAt = now,
                nextOccurrenceAt = nextOccurrence,
                occurrenceCount = template.occurrenceCount + created,
                updatedAt = now,
            )
        )
        return created
    }

    private fun buildEntry(
        template: RecurringTemplateEntity,
        occurrenceMs: Long,
    ): EntropyEntryEntity {
        // Deterministische ID: gleicher Tag der gleichen Vorlage erzeugt gleiche ID
        // — verhindert Duplikate bei mehrfachem Aufruf am gleichen Tag.
        val deterministicId = "rec-${template.id}-$occurrenceMs"

        // Frank-Wunsch 2026-05-23: Loop-Aufgaben kommen OHNE Frist in die Liste.
        // Die Frist setzt Frank manuell im Detail wenn er sie braucht — sonst gar nicht.
        return EntropyEntryEntity(
            id = deterministicId,
            rawTranscript = "[Wiederkehrend] ${template.title}",
            title = template.title,
            description = template.description ?: "",
            category = template.category,
            severity = template.severity,
            priorityScore = template.priorityScore.toDouble(),
            // Frank-Wunsch 2026-06-01: Die im Loop gesetzte Prioritaet ist eine MANUELLE
            // Prioritaet — sie muss in der generierten Aufgabe gelten und darf NICHT von der
            // KI ueberschrieben werden. manualPriorityScore hat Vorrang vor priorityScore.
            manualPriorityScore = template.priorityScore.toDouble(),
            priorityReason = "Wiederkehrende Aufgabe aus Vorlage \"${template.title}\"",
            status = EntryStatus.OFFEN,
            // Frank-Wunsch 2026-05-31: Vorgegebener Ziel-Bucket der Vorlage; null = HEUTE.
            timeBucket = template.targetBucket ?: TimeBucket.HEUTE,
            manualBucket = template.targetBucket,
            manualBucketSetAt = if (template.targetBucket != null) occurrenceMs else null,
            estimatedDurationMinutes = template.estimatedDurationMinutes,
            createdAt = occurrenceMs,
            updatedAt = occurrenceMs,
            resolvedAt = null,
            tags = emptyList(),
            aiNotes = null,
            source = EntrySource.RECURRING_TEMPLATE,
            biomarkerSnapshotId = null,
            durationManuallySet = template.estimatedDurationMinutes != null,
            dueAtMs = null,
        )
    }

    /** Setzt die Uhrzeit (Stunden+Minuten) auf die lokale Mitternacht des Tages. */
    private fun computeDueTime(occurrenceMs: Long, timeOfDayMinutes: Int): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getDefault())
        cal.timeInMillis = occurrenceMs
        cal.set(java.util.Calendar.HOUR_OF_DAY, timeOfDayMinutes / 60)
        cal.set(java.util.Calendar.MINUTE, timeOfDayMinutes % 60)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Frank-Wunsch 2026-05-22 (Bugfix #949): Beim App-Start NICHT mehr blind
     * generieren. Stattdessen: fuer jede Vorlage sicherstellen dass es
     * GENAU EINE offene Instanz gibt (wenn aktiv) bzw. KEINE (wenn inaktiv).
     *
     * Frank-Wunsch 2026-05-25 (Bugfix Geist-Aufgaben): Innerhalb des LAUFENDEN
     * Tages wird HEUTE NIE wieder aufgefuellt. Wurde die heutige Instanz einer
     * Vorlage schon einmal erzeugt, darf sie NICHT erneut erscheinen — egal ob
     * Frank sie inzwischen erledigt (Status REDUZIERT) oder ganz geloescht hat.
     * Erst der naechste Tag bringt eine frische Instanz (neue Tages-ID). Damit
     * laeuft die HEUTE-Liste ueber den Tag nur noch nach UNTEN (5 -> 4 -> ...).
     *
     * Ablauf pro aktiver Vorlage:
     *  - Sammle alle OFFENEN RECURRING_TEMPLATE-Eintraege (ID-Praefix
     *    "rec-${templateId}-") und loesche aeltere offene Duplikate.
     *  - "Heute schon versorgt?" wird ueber ZWEI Marker erkannt:
     *      (a) die heutige Tages-ID existiert in IRGENDEINEM Status
     *          (offen/in Arbeit/erledigt/archiviert) — deckt "erledigt" ab,
     *      (b) template.lastGeneratedAt liegt am heutigen Tag — deckt
     *          "vom Nutzer geloescht" ab (Row weg, Marker bleibt).
     *  - Nur wenn (KEINE offene Instanz da) UND (heute noch NICHT versorgt):
     *    genau 1 neue Instanz fuer heute anlegen und lastGeneratedAt setzen.
     *  - Vorlage inaktiv → loesche ALLE offenen.
     *
     * Damit verschwinden die Duplikate die durch frueheres lastGeneratedAt=0
     * + RRULE-Iteration entstanden sind, UND eine heute erledigte/geloeschte
     * Loop-Aufgabe wird nicht mehr als "neue" Aufgabe wiederbelebt.
     */
    suspend fun cleanupAndEnsureSingle() {
        val now = System.currentTimeMillis()
        val todayMidnight = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val allTemplates = templateRepo.getAllForBackup()
        // Frank-Wunsch 2026-06-02 (Bugfix): VOR der Instanz-Verwaltung verwaiste Konvertierungs-
        // Duplikate heilen. Mit dem alten Konvertierungs-Code ("Aufgabe zu wiederkehrenden
        // hinzufuegen") blieb die Original-Aufgabe (eigene ID, z.B. NUTZER_MIC) als unverknuepftes
        // Duplikat neben der generierten Loop-Instanz liegen. Die Loop-Bearbeitung (setPriority etc.)
        // erreicht nur die rec-Instanz -> die Original zeigt weiter "KI". Diese Heilung entfernt das
        // Duplikat (bzw. wandelt die Original in die Loop-Instanz um) -> der Nutzer sieht nur noch
        // EINE Aufgabe mit der korrekten Loop-Prioritaet. Idempotent: ohne Duplikate ein No-Op.
        healOrphanedConversions(allTemplates.filter { it.isActive }, now)
        val allOpenEntries = entryRepo.getActive().first()

        // Frank-Wunsch 2026-06-01: fuer den Intervall-Cooldown brauchen wir den
        // Zeitstempel der LETZTEN Erledigung pro Vorlage. Jede erledigte Aufgabe traegt
        // dafuer resolvedAt (Tag + Uhrzeit). Wir lesen einmal alle Eintraege und bilden
        // pro Template-ID das Maximum der resolvedAt-Werte unter erledigten Instanzen.
        val allEntries = entryRepo.getAllForBackup()
        val lastCompletedByTemplate: Map<String, Long> =
            allEntries
                .asSequence()
                .filter {
                    it.source == EntrySource.RECURRING_TEMPLATE &&
                        (it.status == EntryStatus.REDUZIERT || it.status == EntryStatus.ARCHIVIERT) &&
                        it.resolvedAt != null
                }
                .mapNotNull { e ->
                    // ID-Form: "rec-{templateId}-{occurrenceMs}". Template-ID herausloesen.
                    val withoutPrefix = e.id.removePrefix("rec-")
                    val lastDash = withoutPrefix.lastIndexOf('-')
                    if (lastDash <= 0) null
                    else withoutPrefix.substring(0, lastDash) to (e.resolvedAt ?: 0L)
                }
                .groupingBy { it.first }
                .fold(0L) { acc, (_, ts) -> maxOf(acc, ts) }

        for (template in allTemplates) {
            try {
                val openForThis = allOpenEntries.filter {
                    it.source == EntrySource.RECURRING_TEMPLATE &&
                        it.id.startsWith("rec-${template.id}-") &&
                        it.status == EntryStatus.OFFEN
                }.sortedByDescending { it.createdAt }

                if (!template.isActive) {
                    // Inaktiv: alle offenen Instanzen weg.
                    for (e in openForThis) entryRepo.delete(e)
                    continue
                }

                // Aktiv: behalte juengste OFFENE, loesche aeltere offene Duplikate.
                if (openForThis.size > 1) {
                    for (e in openForThis.drop(1)) entryRepo.delete(e)
                }

                // Wurde die heutige Instanz schon einmal versorgt? getById liest
                // statusunabhaengig (auch REDUZIERT/ARCHIVIERT) → faengt den Fall
                // "heute erledigt" ab. lastGeneratedAt am heutigen Tag faengt den
                // Fall "heute erzeugt und dann geloescht" ab.
                val todayId = "rec-${template.id}-$todayMidnight"
                val handledToday =
                    entryRepo.get(todayId) != null || template.lastGeneratedAt >= todayMidnight

                // Frank-Wunsch 2026-06-01: Festes Intervall (alle N Tage). Statt die Aufgabe
                // im "Cooldown" zu verstecken, wird sie nach FAELLIGKEIT in den passenden Bucket
                // einsortiert: naechste Faelligkeit = letzte Erledigung + Intervall (Kalendertage).
                //  - heute oder ueberfaellig -> HEUTE
                //  - in 1 Tag                -> MORGEN
                //  - in 2..7 Tagen           -> FREIBLOCK
                //  - in >7 Tagen             -> SPAETER
                // So verschwindet z.B. Kraftsport (alle 5 Tage, gestern erledigt) nicht, sondern
                // wandert nach FREIBLOCK und rueckt Tag fuer Tag naeher Richtung HEUTE.
                // null = "KI entscheidet" -> kein Intervall-Bucket, bisherige taegliche Logik unten.
                val intervalDays = template.intervalDays
                if (intervalDays != null && intervalDays >= 1) {
                    val lastCompletedAt = lastCompletedByTemplate[template.id]
                    val nextDueMidnight =
                        if (lastCompletedAt != null && lastCompletedAt > 0L) {
                            midnightOf(lastCompletedAt) + intervalDays.toLong() * MS_PER_DAY
                        } else {
                            // Noch nie erledigt -> ab heute faellig.
                            todayMidnight
                        }
                    val daysUntilDue = ((nextDueMidnight - todayMidnight) / MS_PER_DAY).toInt()
                    val dueBucket =
                        when {
                            daysUntilDue <= 0 -> TimeBucket.HEUTE
                            daysUntilDue == 1 -> TimeBucket.MORGEN
                            daysUntilDue <= 7 -> TimeBucket.FREIBLOCK
                            else -> TimeBucket.SPAETER
                        }
                    if (openForThis.isNotEmpty()) {
                        // Vorhandene offene Instanz behalten und in den Faelligkeits-Bucket schieben.
                        // Frank-Wunsch 2026-06-01: dabei auch die manuelle Loop-Prio synchronisieren.
                        val keep = openForThis.first()
                        val prio = template.priorityScore.toDouble()
                        // Frank-Regel 2026-06-19: MANUELL hat IMMER Vorrang vor der Loop/KI-Logik.
                        // Hat der Nutzer die Instanz NACH der letzten Loop-Synchronisierung manuell in
                        // einen anderen Bucket geschoben (manualBucketSetAt neuer als lastGeneratedAt),
                        // wird die Faelligkeits-Zuordnung NICHT mehr aufgezwungen — sonst setzt der
                        // Loop-Cleanup bei jedem App-Start eine manuelle MORGEN-Verschiebung wieder auf
                        // den Faelligkeits-Bucket (HEUTE) zurueck (Bucket-Rollback-Bug 2026-06-19).
                        val userMovedManually =
                            keep.manualBucket != null &&
                                (keep.manualBucketSetAt ?: 0L) > template.lastGeneratedAt
                        // Frank-Regel 2026-06-19 (Loop-Prio-Analogon zum Bucket): Hat der Nutzer die
                        // Prio AN DIESER INSTANZ manuell gesetzt (manualPriorityScoreSetAt != null,
                        // gesetzt von TasksViewModel.setManualPriority), darf die Loop-Pflege sie NICHT
                        // mehr mit der Template-Prio ueberschreiben. Loop-/Template-erzeugte Instanzen
                        // haben den Marker null -> werden weiter synchronisiert.
                        val userSetPriorityManually = keep.manualPriorityScoreSetAt != null
                        if (userMovedManually) {
                            // Nur die Loop-Prioritaet weiter synchronisieren — der Bucket bleibt, wo
                            // der Nutzer ihn manuell hingelegt hat. Die Prio aber nur, wenn der Nutzer
                            // sie NICHT selbst manuell gesetzt hat.
                            Diag.i(
                                DiagnosticArea.TASKS, TAG,
                                "Loop-Sync '${keep.title.take(24)}': manuell auf ${keep.manualBucket} " +
                                    "(setAt=${keep.manualBucketSetAt} > lastGen=${template.lastGeneratedAt}) -> Bucket NICHT angetastet" +
                                    if (userSetPriorityManually) ", Prio manuell -> auch NICHT angetastet" else "",
                            )
                            if (!userSetPriorityManually && keep.manualPriorityScore != prio) {
                                entryRepo.upsert(
                                    keep.copy(priorityScore = prio, manualPriorityScore = prio, updatedAt = now),
                                )
                            }
                        } else if (keep.timeBucket != dueBucket ||
                            keep.manualBucket != dueBucket ||
                            (!userSetPriorityManually && keep.manualPriorityScore != prio)
                        ) {
                            entryRepo.upsert(
                                keep.copy(
                                    timeBucket = dueBucket,
                                    manualBucket = dueBucket,
                                    manualBucketSetAt = now,
                                    priorityScore = prio,
                                    // Manuell gesetzte Instanz-Prio bleibt erhalten; sonst Template-Prio.
                                    manualPriorityScore = if (userSetPriorityManually) keep.manualPriorityScore else prio,
                                    updatedAt = now,
                                ),
                            )
                            // lastGeneratedAt mitziehen, damit diese System-Synchronisierung beim
                            // naechsten Lauf nicht faelschlich als "manuell verschoben" gilt.
                            templateRepo.upsert(template.copy(lastGeneratedAt = now))
                        }
                    } else {
                        // Keine offene Instanz -> eine fuer die NAECHSTE Faelligkeit anlegen.
                        // Frank-Wunsch 2026-06-01: Eine Intervall-Loop-Aufgabe muss IMMER irgendwo
                        // sichtbar sein (im Faelligkeits-Bucket) — nicht "verschwinden". Der
                        // handledToday-Mechanismus gilt NICHT fuer Intervall-Vorlagen, weil die
                        // Instanz die naechste Faelligkeit repraesentiert, nicht "heute".
                        // Die ID basiert auf nextDueMidnight (statt heute), damit sie NIE mit der
                        // bereits erledigten Instanz kollidiert (kein Ueberschreiben per Upsert).
                        val dueId = "rec-${template.id}-$nextDueMidnight"
                        if (entryRepo.get(dueId) == null) {
                            entryRepo.upsert(
                                buildEntry(template, nextDueMidnight).copy(
                                    timeBucket = dueBucket,
                                    manualBucket = dueBucket,
                                    manualBucketSetAt = now,
                                    // Jetzt erstellt (nextDueMidnight kann in der Zukunft liegen).
                                    createdAt = now,
                                    updatedAt = now,
                                ),
                            )
                            templateRepo.upsert(
                                template.copy(
                                    lastGeneratedAt = now,
                                    occurrenceCount = template.occurrenceCount + 1,
                                    updatedAt = now,
                                ),
                            )
                        }
                    }
                    continue
                }

                // Frank-Wunsch 2026-06-01: bestehende offene Instanz (ohne festes Intervall)
                // auf die im Loop gesetzte Prioritaet synchronisieren — damit sie nicht als
                // "KI" angezeigt oder von der KI hochgesetzt wird. Greift auch fuer Instanzen
                // die vor dieser Aenderung ohne manualPriorityScore erzeugt wurden.
                openForThis.firstOrNull()?.let { keepNormal ->
                    val prio = template.priorityScore.toDouble()
                    // Frank-Regel 2026-06-19: manuell an der Instanz gesetzte Prio NICHT
                    // ueberschreiben (Marker manualPriorityScoreSetAt != null).
                    val userSetPriorityManually = keepNormal.manualPriorityScoreSetAt != null
                    if (!userSetPriorityManually && keepNormal.manualPriorityScore != prio) {
                        entryRepo.upsert(
                            keepNormal.copy(
                                priorityScore = prio,
                                manualPriorityScore = prio,
                                updatedAt = now,
                            ),
                        )
                    }
                }

                // Nur erzeugen wenn aktuell KEINE offene Instanz existiert UND heute
                // noch nichts versorgt wurde. Sonst bleibt die HEUTE-Liste so wie
                // Frank sie abgearbeitet hat (kein automatisches Nachfuellen).
                if (openForThis.isEmpty() && !handledToday) {
                    entryRepo.upsert(buildEntryForToday(template, now))
                    templateRepo.upsert(
                        template.copy(
                            lastGeneratedAt = now,
                            occurrenceCount = template.occurrenceCount + 1,
                            updatedAt = now,
                        ),
                    )
                }
            } catch (e: Exception) {
                Diag.w(DiagnosticArea.TASKS, TAG, "Cleanup fuer Vorlage '${template.title}' (${template.id}) fehlgeschlagen: ${e.message}")
            }
        }
    }

    /**
     * Erzeugt eine deterministische Aufgabe fuer "heute" (Mitternacht-basiert).
     * Mehrfache Aufrufe am selben Tag schreiben in dieselbe Row (Upsert idempotent).
     */
    private fun buildEntryForToday(template: RecurringTemplateEntity, nowMs: Long): EntropyEntryEntity {
        val midnight = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Frank-Wunsch 2026-05-23: Loop-Aufgaben kommen OHNE Frist in die Liste.
        // Frist setzt Frank manuell im Detail, falls noetig.
        return EntropyEntryEntity(
            id = "rec-${template.id}-$midnight",
            rawTranscript = "[Wiederkehrend] ${template.title}",
            title = template.title,
            description = template.description ?: "",
            category = template.category,
            severity = template.severity,
            priorityScore = template.priorityScore.toDouble(),
            // Frank-Wunsch 2026-06-01: Die im Loop gesetzte Prioritaet ist eine MANUELLE
            // Prioritaet — sie muss in der generierten Aufgabe gelten und darf NICHT von der
            // KI ueberschrieben werden. manualPriorityScore hat Vorrang vor priorityScore.
            manualPriorityScore = template.priorityScore.toDouble(),
            priorityReason = "Wiederkehrende Aufgabe aus Vorlage \"${template.title}\"",
            status = EntryStatus.OFFEN,
            // Frank-Wunsch 2026-05-31: Vorgegebener Ziel-Bucket der Vorlage; null = HEUTE.
            timeBucket = template.targetBucket ?: TimeBucket.HEUTE,
            manualBucket = template.targetBucket,
            manualBucketSetAt = if (template.targetBucket != null) nowMs else null,
            estimatedDurationMinutes = template.estimatedDurationMinutes,
            createdAt = nowMs,
            updatedAt = nowMs,
            resolvedAt = null,
            tags = emptyList(),
            aiNotes = null,
            source = EntrySource.RECURRING_TEMPLATE,
            biomarkerSnapshotId = null,
            durationManuallySet = template.estimatedDurationMinutes != null,
            dueAtMs = null,
        )
    }

    /**
     * Frank-Wunsch 2026-06-02 (Bugfix): Heilt verwaiste Konvertierungs-Duplikate.
     *
     * Der alte "Aufgabe zu wiederkehrenden hinzufuegen"-Pfad legte ein Template an, liess die
     * Original-Aufgabe (eigene ID, z.B. NUTZER_MIC) aber als unverknuepftes Duplikat liegen.
     * Zusaetzlich erzeugte cleanupAndEnsureSingle eine echte Loop-Instanz — Ergebnis: zwei
     * gleichnamige Aufgaben. setPriority/setTitle/setTargetBucket erreichen nur die rec-Instanz,
     * die Original zeigt weiter "Priorität KI" (manualPriorityScore=null). Fuer den Nutzer sieht
     * es aus, als wirke die Loop-Aenderung nicht.
     *
     * Heilung pro aktivem Template T: jede OFFENE Nicht-Loop-Aufgabe mit exakt T's Titel ist eine
     * verwaiste Konvertierung.
     *  - Existiert bereits eine offene rec-Instanz von T  -> die Original ist ein Duplikat:
     *    Nachtraege auf die rec-Instanz umhaengen, dann Original loeschen.
     *  - Existiert KEINE rec-Instanz                       -> die Original WIRD die heutige
     *    Loop-Instanz (rec-ID + source + Template-Prio), damit kuenftige Loop-Aenderungen sie
     *    erreichen (identisch zum Konvertierungs-Fix in EntryDetailViewModel).
     *
     * Exakter Titel-Match ist hier sicher genug: eine normale Aufgabe mit identischem Titel wie
     * ein aktives Loop-Template entsteht praktisch nur durch genau diese Konvertierung. Idempotent
     * — ohne verwaiste Duplikate passiert nichts. Reihenfolge respektiert die Followup-FK (CASCADE):
     * Ziel-Row existiert vor dem Umhaengen, Original wird zuletzt geloescht.
     */
    private suspend fun healOrphanedConversions(
        activeTemplates: List<RecurringTemplateEntity>,
        now: Long,
    ) {
        if (activeTemplates.isEmpty()) return
        val open = entryRepo.getActive().first().filter { it.status == EntryStatus.OFFEN }
        if (open.isEmpty()) return
        val templatesByTitle = activeTemplates.groupBy { it.title.trim() }

        for (orphan in open) {
            // Nur echte Original-Aufgaben (NICHT-Loop) sind Heilungs-Kandidaten.
            val isRec =
                orphan.source == EntrySource.RECURRING_TEMPLATE || orphan.id.startsWith("rec-")
            if (isRec) continue
            val template = templatesByTitle[orphan.title.trim()]?.firstOrNull() ?: continue
            try {
                val recInstance = open.firstOrNull {
                    it.source == EntrySource.RECURRING_TEMPLATE &&
                        it.id.startsWith("rec-${template.id}-") &&
                        it.status == EntryStatus.OFFEN
                }
                val prio = template.priorityScore.toDouble()
                if (recInstance != null) {
                    for (fu in followupDao.getByEntryId(orphan.id)) {
                        followupDao.upsert(
                            fu.copy(id = java.util.UUID.randomUUID().toString(), entryId = recInstance.id),
                        )
                    }
                    entryRepo.delete(orphan)
                    Diag.i(DiagnosticArea.TASKS, 
                        TAG,
                        "Verwaiste Konvertierung '${orphan.title}' entfernt (Duplikat zu ${recInstance.id}).",
                    )
                } else {
                    val recId = "rec-${template.id}-${midnightOf(now)}"
                    if (recId == orphan.id) continue
                    entryRepo.upsert(
                        orphan.copy(
                            id = recId,
                            source = EntrySource.RECURRING_TEMPLATE,
                            priorityScore = prio,
                            manualPriorityScore = prio,
                            // Wird zur Loop-Instanz -> Template-getriebene Prio, kein Instanz-Marker.
                            manualPriorityScoreSetAt = null,
                            updatedAt = now,
                        ),
                    )
                    for (fu in followupDao.getByEntryId(orphan.id)) {
                        followupDao.upsert(
                            fu.copy(id = java.util.UUID.randomUUID().toString(), entryId = recId),
                        )
                    }
                    entryRepo.delete(orphan)
                    templateRepo.upsert(
                        template.copy(
                            lastGeneratedAt = now,
                            occurrenceCount = maxOf(template.occurrenceCount, 1),
                            updatedAt = now,
                        ),
                    )
                    Diag.i(DiagnosticArea.TASKS, 
                        TAG,
                        "Verwaiste Konvertierung '${orphan.title}' in Loop-Instanz $recId umgewandelt.",
                    )
                }
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                Diag.w(DiagnosticArea.TASKS, 
                    TAG,
                    "Heilung fuer '${orphan.title}' (${orphan.id}) fehlgeschlagen: ${e.message}",
                )
            }
        }
    }

    /** Lokale Mitternacht (00:00) des Tages zu dem [ms] gehoert. */
    private fun midnightOf(ms: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    companion object {
        private const val TAG = "RecurringInstances"
        private const val MS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}

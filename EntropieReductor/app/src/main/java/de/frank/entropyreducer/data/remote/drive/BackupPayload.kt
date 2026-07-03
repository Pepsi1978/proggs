package de.frank.entropyreducer.data.remote.drive

import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraPersonalInfoEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.domain.model.ScientistRole
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.serialization.Serializable

/**
 * Backup-Format für Drive (appDataFolder).
 *
 * Frank-Wunsch 2026-05-09 (Abend): Vollstaendiges Backup ALLER Daten — nicht nur Aufgaben (v1),
 * sondern auch Insights, Memories, Hypothesen und Forscher-Sessions. Alles was in Frank's
 * persoenlicher Wissens-Domaene liegt soll bei Reinstall wiederherstellbar sein.
 *
 * version: erlaubt zukuenftige Schema-Evolution. 1 = nur entries (Pre-2026-05-09) 2 = + insights,
 * memories, hypotheses, scientistSessions, scientistMessages, hypothesisMessages (2026-05-09 Abend)
 * 3 = + biomarkerCardOrder — die Drag&Drop-Reihenfolge der Biomarker-Karten (Frank-Wunsch
 * 2026-05-10): die Position auf dem Biomarker-Screen wird jetzt mitgesichert, damit ein Wechsel auf
 * ein neues Handy die exakt gleiche Anordnung wiederherstellt. 5 = + amazfitWorkouts —
 * Sport-Sessions inkl. Detail-Daten (GPS-Track, Pulsverlauf, Tempoverlauf, Splits) als
 * Cross-Device-Sicherung. Frank-Wunsch 2026-05-11: Zepp loescht Detail-Daten serverseitig nach ~30
 * Tagen — durch das Backup bleiben sie fuer immer erhalten und koennen auf dem S23 Ultra ohne
 * API-Call wiederhergestellt werden (kein Re-Login, Zepp-Handy-App bleibt stabil eingeloggt).
 *
 * Beim Restore werden alle vorherigen Versionen akzeptiert — die nicht-vorhandenen Listen defaulten
 * auf emptyList(), die alten Aufgaben kommen zurueck, der Rest bleibt leer. Beim naechsten Sync
 * nach Restore wird automatisch ein v5-Backup geschrieben das den vollen Stand enthaelt.
 */
@Serializable
data class BackupPayload(
    // Direktive 3 Loop-3-Fix (war L3-4-Bug): Default war hartnaeckig bei 5
    // obwohl SyncCoordinator schon version=9 setzt. Falls jemals ein
    // BackupPayload-Snapshot ohne explizite version-Angabe erstellt wird (z.B.
    // in Tests), wuerde er fueschlich Version 5 melden. Default auf aktuelle
    // Schema-Version anheben damit alle Code-Pfade konsistent sind.
    val version: Int = 18,
    val exportedAt: Long,
    val entries: List<BackupEntry>,
    val insights: List<BackupInsight> = emptyList(),
    val memories: List<BackupMemory> = emptyList(),
    val hypotheses: List<BackupHypothesis> = emptyList(),
    val scientistSessions: List<BackupScientistSession> = emptyList(),
    val scientistMessages: List<BackupScientistMessage> = emptyList(),
    val hypothesisMessages: List<BackupHypothesisMessage> = emptyList(),
    /**
     * Drag&Drop-Reihenfolge der Biomarker-Karten als pipe-separierte Liste der Card-IDs (siehe
     * BiomarkerCardId). Leer = User hat noch nichts verschoben, Standard-Reihenfolge gilt. Beim
     * Restore wird die Liste in den lokalen BiomarkerCardOrderRepository.saveOrder() eingespielt —
     * ungueltige IDs (aus aelteren App-Versionen) werden dort automatisch herausgefiltert.
     */
    val biomarkerCardOrder: List<String> = emptyList(),
    /**
     * Schema v4 (Frank-Wunsch 2026-05-10 abend): Cross-Device-Cache aller Health-Connect-Werte
     * (Gewicht, Koerperfett, Magere Koerpermasse, Wasser, Knochenmasse, Hoehe, BMR). Wird auf dem
     * Sende-Geraet beim refreshWeight gefuellt und beim Restore auf dem Empfaenger-Geraet in die DB
     * geschrieben. Damit hat das neue Geraet sofort den vollen Verlauf, auch wenn Zepp dort nicht
     * rueckwirkend in HC pusht. Default = emptyList damit v3-Backups weiterhin lesbar bleiben.
     */
    val healthConnectValues: List<BackupHealthConnectValue> = emptyList(),
    /**
     * Schema v5 (Frank-Wunsch 2026-05-11): Cross-Device-Sicherung der gesamten Sport-Trainings
     * inklusive der teuren Detail-Streams (GPS-Track, Puls-, Tempo-, Pace-Verlauf, Splits).
     * Zepp-Cloud loescht diese Detail-Daten nach ~30 Tagen serverseitig — durch das Backup haben
     * wir sie fuer immer. Beim Restore auf einem zweiten Geraet entfaellt der API-Call zu Zepp
     * komplett: kein Re-Login, kein Token-Konflikt mit der Zepp-Handy-App. Default = emptyList
     * damit aeltere Backups (v1-v4) weiterhin lesbar bleiben.
     */
    val amazfitWorkouts: List<BackupAmazfitWorkout> = emptyList(),
    /**
     * Schema v6 (Frank-Wunsch 2026-05-20): Persoenliches Profil aus den Einstellungen. Wird in
     * jeden KI-Aufruf als Systemkontext mitgeschickt — bei Reinstall ist das ohne Backup verloren.
     */
    val profileText: String = "",
    /**
     * Schema v6 (Frank-Wunsch 2026-05-20): Eintraege aus dem Sub-Bereich "Entropie" (Tagebuch).
     * Inkl. KI-Zusammenfassung und allen Nachtraegen pro Eintrag.
     */
    val tagebuchEntries: List<BackupTagebuchEntry> = emptyList(),
    /**
     * Schema v6 (Frank-Wunsch 2026-05-20): Nachtraege zu Aufgaben-Eintraegen
     * (entropy_entry_followups). Werden ueber entryId mit dem Haupteintrag verknuepft.
     */
    val entropyEntryFollowups: List<BackupEntropyFollowup> = emptyList(),
    /**
     * Schema v7 (Frank-Wunsch 2026-05-20): Eintraege aus dem Sub-Bereich "Thesen" (Aufgaben-Tab
     * Index 2). Spiegelt das gleiche Format wie tagebuchEntries — jeder Eintrag hat Titel, Text,
     * optionale KI-Zusammenfassung und Nachtraege.
     */
    val thesenEntries: List<BackupThesenEntry> = emptyList(),
    /**
     * Schema v8 (Frank-Wunsch 2026-05-20): Eigene Prompts inkl. Kategorie. Pro Bereich der App
     * wirken nur Prompts der jeweiligen Kategorie. Ab v9 erweitert um model,
     * tokenLimitPerDay, trustModeDefault.
     */
    val savedPrompts: List<BackupSavedPrompt> = emptyList(),
    /**
     * Schema v9 (Frank-Wunsch 2026-05-21): Tool-Permissions pro Prompt — welche
     * Schreib-Tools freigeschaltet sind und ob Trust-Modus aktiv ist.
     */
    val promptToolPermissions: List<BackupPromptToolPermission> = emptyList(),
    /**
     * Schema v9: Auto-Trigger-Konfigurationen (CRON, CHAIN, EVENT).
     */
    val promptTriggers: List<BackupPromptTrigger> = emptyList(),
    /**
     * Schema v10 (Frank-Wunsch 2026-05-22, Sprint 2.8): Vorlagen wiederkehrender
     * Aufgaben (RFC-5545 RRULE). Ohne dieses Feld waeren alle Wiederholungs-Vorlagen
     * nach `adb uninstall` + Restore weg — gleicher Stolperstein wie
     * promptTriggers + supplements vorher. Default = emptyList damit aeltere
     * Backups (v1-v9) weiterhin sauber lesbar bleiben.
     */
    val recurringTemplates: List<BackupRecurringTemplate> = emptyList(),
    /**
     * Schema v11 (Frank-Bugfix 2026-05-22): Tagliche Amazfit-T-Rex-3-Werte
     * (PAI, BioCharge, Hauttemperatur, SpO2, Stress, Schritte, HRV, Ruhepuls,
     * Schlaf-Felder). Bisher GAR NICHT im Backup — bei Reinstall verloren.
     * Default = emptyList damit aeltere Backups (v1-v10) lesbar bleiben.
     */
    val amazfitDaily: List<BackupAmazfitDaily> = emptyList(),
    /**
     * Schema v12 (Frank-Wunsch 2026-06-09): Mentalboard-Eintraege (Aufgaben-Reiter "Mental").
     * Frei sortierte Liste kurzer Saetze — die Reihenfolge ergibt sich aus der Listen-Position.
     * Default emptyList damit aeltere Backups (v1-v11) weiterhin lesbar bleiben.
     */
    val mentals: List<BackupMental> = emptyList(),
    /**
     * Schema v13 (Frank-Wunsch 2026-06-10): Ideen-Eintraege (Aufgaben-Reiter "Ideen", 1:1-Klon
     * des Tagebuch/Entropie-Bereichs). Inkl. KI-Zusammenfassung und allen Nachtraegen pro Eintrag.
     * Default emptyList damit aeltere Backups (v1-v12) weiterhin lesbar bleiben.
     */
    val ideenEntries: List<BackupIdeenEntry> = emptyList(),
    /**
     * Schema v14 (Frank-Bugfix 2026-06-19): Gewohnheit-Eintraege (Aufgaben-Reiter "Gewohnheit").
     * Bisher GAR NICHT im Backup — der Reiter wurde hinzugefuegt, aber weder im Upload gesammelt
     * noch beim Restore eingespielt (obwohl restoreGewohnheiten() bereits existierte). Bei jedem
     * App-Update/Reinstall gingen die Gewohnheiten verloren. Gleiches Format wie Mentalboard
     * (nur id + Satz, Reihenfolge = Listen-Position). Default emptyList damit aeltere Backups
     * (v1-v13) weiterhin lesbar bleiben.
     */
    val gewohnheiten: List<BackupMental> = emptyList(),
    /**
     * Schema v15 (Frank-Wunsch 2026-06-19): Offene KI-Aufgabenvorschlaege (Aufgaben-Reiter,
     * ki_task_suggestions). Damit gehen erkannte, noch nicht angenommene Vorschlaege bei
     * Update/Reinstall/Geraetewechsel nicht verloren. Default emptyList (v1-v14 lesbar).
     */
    val taskSuggestions: List<BackupTaskSuggestion> = emptyList(),
    /**
     * Schema v15 (Frank-Wunsch 2026-06-19): Offene Gewohnheitsvorschlaege (gewohnheit_suggestions).
     * Gleiches id+text-Format wie Mental — BackupMental wiederverwendet.
     */
    val gewohnheitSuggestions: List<BackupMental> = emptyList(),
    /**
     * Schema v16 (Frank-Wunsch 2026-06-19, Sync-Etappe 1.2): Loesch-Protokoll (Tombstones).
     * Haelt fuer jede vom Nutzer geloeschte Entitaet (Typ + ID + Loeschzeitpunkt) einen Eintrag,
     * damit eine Loeschung auf einem Geraet beim Restore auch auf den anderen Geraeten ausgefuehrt
     * wird (1:1-Synchronitaet inkl. Loeschungen). Ohne dieses Feld ist der Restore rein additiv und
     * geloeschte Eintraege "auferstehen" auf dem Zweitgeraet. Konflikt-Regel: eine Loeschung greift
     * nur, wenn ihr Zeitstempel NEUER ist als die lokale Version der Entitaet (sonst gewinnt eine
     * spaetere Bearbeitung). Default emptyList damit aeltere Backups (v1-v15) lesbar bleiben.
     */
    val tombstones: List<BackupTombstone> = emptyList(),
    /**
     * Schema v17 (Frank-Wunsch 2026-06-19): Prioritaets-Gedaechtnis (gemerkte manuelle
     * Aufgaben-Prioritaeten). Default emptyList damit aeltere Backups (v1-v16) lesbar bleiben.
     */
    val priorityMemories: List<BackupPriorityMemory> = emptyList(),
)

/**
 * Schema v15: ein KI-Aufgabenvorschlag (id + Titel + Beschreibung). Spiegelt KiTaskSuggestion
 * aus dem Aufgaben-Reiter.
 *
 * Schema v18 (Frank-Wunsch 2026-06-20, ID-Architektur Backup-Lineage-Fix): originId/originType/
 * rootId mitsichern. Vorher ging die Herkunft beim Drive-Backup verloren -> auf einem 2. Geraet
 * hatte der Vorschlag originId=null -> der Ketten-Dedup (countByOriginId) war geraeteuebergreifend
 * BLIND -> Doppelvorschlag. Mit Herkunft im Backup bleibt der Dedup auch nach Sync/Restore robust.
 * null-Defaults: aeltere Backups (v15-v17) ohne diese Felder bleiben lesbar (Alt-Vorschlaege ohne
 * Herkunft, wie bisher).
 */
@Serializable
data class BackupTaskSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

/**
 * Schema v16: Ein Tombstone (Loesch-Markierung). [type] = Entitaetstyp (siehe TombstoneType im
 * data-Paket), [id] = die geloeschte Entitaets-ID, [deletedAt] = epoch ms der Loeschung. Beim
 * Restore wird eine lokale Entitaet nur geloescht, wenn deletedAt NEUER ist als ihr lokaler
 * updatedAt-Stand (delete-wins-only-if-newer).
 */
@Serializable
data class BackupTombstone(val type: String, val id: String, val deletedAt: Long)

/**
 * Schema v12 (Frank-Wunsch 2026-06-09): ein Mentalboard-Eintrag. Nur id + Satz; die
 * Reihenfolge ergibt sich aus der Listen-Position im Backup. Wird auch fuer Gewohnheiten und
 * Gewohnheits-Vorschlaege wiederverwendet (gleiches id+text-Format).
 *
 * Schema v18 (Frank-Wunsch 2026-06-20, ID-Architektur Backup-Lineage-Fix): originId/originType/
 * rootId mitsichern. Relevant v. a. fuer die GEWOHNHEITS-Vorschlaege (habit_suggestions) — sonst
 * ginge ihre Herkunft beim Drive-Backup verloren und der Ketten-Dedup (countByOriginId) waere auf
 * einem 2. Geraet blind (Doppelvorschlag). null-Defaults halten aeltere Backups (v12-v17) lesbar.
 */
@Serializable
data class BackupMental(
    val id: String,
    val text: String,
    val updatedAt: Long = 0L,
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

/**
 * Schema v17 (Frank-Wunsch 2026-06-19): ein Prioritaets-Gedaechtnis-Eintrag (gemerkte manuelle
 * Aufgaben-Prioritaet). updatedAt traegt delete-wins-only-if-newer beim Multi-Device-Restore.
 */
@Serializable
data class BackupPriorityMemory(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: Double = 50.0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val sourceEntryId: String? = null,
)

/**
 * Schema v10: 1:1-Spiegel der RecurringTemplateEntity (Sprint 2.8). nextOccurrenceAt
 * und lastGeneratedAt werden mitgesichert damit der Generator nach Restore nicht
 * sofort alle Faelligkeiten der Vergangenheit erneut feuert.
 */
@Serializable
data class BackupRecurringTemplate(
    val id: String,
    val title: String,
    val description: String? = null,
    /** EntropyCategory.name. Unbekannte Werte beim Restore -> SONSTIGES. */
    val category: String = "SONSTIGES",
    val priorityScore: Int = 50,
    val severity: Int = 5,
    val estimatedDurationMinutes: Int? = null,
    val rrule: String,
    val timeOfDayMinutes: Int = 480,
    val untilEpochMs: Long? = null,
    val nextOccurrenceAt: Long? = null,
    val lastGeneratedAt: Long = 0L,
    val occurrenceCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    /** TimeBucket.name oder null (= "KI"/automatisch HEUTE). Frank-Wunsch 2026-05-31. */
    val targetBucket: String? = null,
    /**
     * Schema v16 (Sync-Etappe 1.3): festes Wiederkehr-Intervall in Tagen (null = "KI"). Bisher NUR
     * ueber die rrule (FREQ=DAILY;INTERVAL=N) transportiert und beim Restore nie in die Entity
     * zurueckgeschrieben -> die "KI/Taeglich/Alle N Tage"-Anzeige sprang nach Restore auf "KI".
     * Default null damit aeltere Backups (v1-v15) lesbar bleiben.
     */
    val intervalDays: Int? = null,
)

/** Schema v8/v9: gespeicherter Prompt mit Kategorie + Agentic-AI-Felder. */
@Serializable
data class BackupSavedPrompt(
    val id: String,
    val name: String,
    val content: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    /** Eine von PromptCategory.name. Bei unbekanntem Wert beim Restore -> AUFGABEN. */
    val category: String = "AUFGABEN",
    /**
     * Schema v9 (Frank-Wunsch 2026-05-21): Vom Nutzer gewaehltes Gemini-Modell
     * fuer agentic-AI-Ausfuehrungen. Default "gemini-3.1-flash-lite".
     */
    val model: String = "gemini-3.1-flash-lite",
    /**
     * Schema v9: Optionales Tages-Token-Limit. null = kein Limit.
     */
    val tokenLimitPerDay: Int? = null,
    /**
     * Schema v9: Trust-Modus-Default fuer Write-Tools (ohne Confirm-Dialog).
     */
    val trustModeDefault: Boolean = false,
)

/**
 * Schema v9 (Frank-Wunsch 2026-05-21): Tool-Permission pro Prompt. Erlaubt
 * Cross-Device-Sicherung welche Write-Tools fuer welchen Prompt freigeschaltet
 * sind und ob sie ohne Confirm-Dialog ausgefuehrt werden duerfen.
 */
@Serializable
data class BackupPromptToolPermission(
    val id: String,
    val promptId: String,
    val toolName: String,
    val granted: Boolean = false,
    val trustMode: Boolean = false,
)

/**
 * Schema v9 (Frank-Wunsch 2026-05-21): Auto-Trigger-Konfiguration pro Prompt
 * (CRON/CHAIN/EVENT). nextScheduledAt wird beim Restore neu berechnet, daher
 * hier nicht persistiert.
 */
@Serializable
data class BackupPromptTrigger(
    val id: String,
    val promptId: String,
    /** TriggerType.name: MANUAL / CRON / EVENT / CHAIN. */
    val triggerType: String,
    val cronExpression: String? = null,
    val eventCondition: String? = null,
    val chainAfterPromptId: String? = null,
    val isActive: Boolean = true,
)

/** Schema v7: Thesen-Eintrag mit Nachtraegen und KI-Zusammenfassung. */
@Serializable
data class BackupThesenEntry(
    val id: String,
    val timestampMs: Long,
    // Edit-Sync (Phase B 2026-06-20): letzter Bearbeitungszeitpunkt fuer Last-Write-Wins. Default 0L
    // fuer abwaertskompatibles Lesen alter Backups (ignoreUnknownKeys); Baseline-Fallback im Restore.
    val updatedAt: Long = 0L,
    val title: String,
    val text: String,
    val summary: String? = null,
    val followups: List<BackupThesenFollowup> = emptyList(),
)

/** Schema v7: einzelner Nachtrag in den Thesen. */
@Serializable
data class BackupThesenFollowup(val id: String, val createdAtMs: Long, val text: String)

/** Schema v6: Tagebuch-Eintrag mit Nachtraegen und KI-Zusammenfassung. */
@Serializable
data class BackupTagebuchEntry(
    val id: String,
    val timestampMs: Long,
    // Edit-Sync (Phase B 2026-06-20): letzter Bearbeitungszeitpunkt fuer Last-Write-Wins. Default 0L
    // fuer abwaertskompatibles Lesen alter Backups (ignoreUnknownKeys); Baseline-Fallback im Restore.
    val updatedAt: Long = 0L,
    val title: String,
    val text: String,
    val summary: String? = null,
    val followups: List<BackupTagebuchFollowup> = emptyList(),
)

/** Schema v6: einzelner Nachtrag im Tagebuch. */
@Serializable
data class BackupTagebuchFollowup(val id: String, val createdAtMs: Long, val text: String)

/**
 * Schema v13 (Frank-Wunsch 2026-06-10): Ideen-Eintrag (Aufgaben-Reiter "Ideen", 1:1-Klon des
 * Tagebuch/Entropie-Bereichs). Gleiches Format wie BackupTagebuchEntry — Titel, Text, optionale
 * KI-Zusammenfassung und Nachtraege.
 */
@Serializable
data class BackupIdeenEntry(
    val id: String,
    val timestampMs: Long,
    // Edit-Sync (Phase B 2026-06-20): letzter Bearbeitungszeitpunkt fuer Last-Write-Wins. Nullable
    // wie die Room-Spalte ideas.updatedAt (Phase A); Baseline-Fallback timestampMs im Restore.
    val updatedAt: Long? = null,
    val title: String,
    val text: String,
    val summary: String? = null,
    val followups: List<BackupIdeenFollowup> = emptyList(),
)

/** Schema v13: einzelner Nachtrag in den Ideen. */
@Serializable
data class BackupIdeenFollowup(val id: String, val createdAtMs: Long, val text: String)

/** Schema v6: Nachtrag zu einer Aufgabe (entropy_entry_followups). */
@Serializable
data class BackupEntropyFollowup(
    val id: String,
    val entryId: String,
    val rawText: String,
    val improvedText: String? = null,
    val isImproved: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Frank-Wunsch 2026-05-19: Eigene Backup-Datei nur fuer Sport-Trainings. Liegt im
 * Drive-appDataFolder als `entropy_reducer_workouts_v1.json`. Wird beim SyncCoordinator- Upload
 * separat hochgeladen, nachdem das Haupt-Backup erfolgreich war. Beim Restore (z.B. nach `adb
 * uninstall`) wird die Datei automatisch heruntergeladen und in die lokale DB geschrieben — kein
 * Polar-ZIP-Re-Import noetig.
 *
 * Im Gegensatz zur slim-Projektion im Haupt-Backup enthaelt diese Liste ALLE Felder inkl. der
 * teuren Stream-Spalten (gpsTrackJson, heartRateSeriesJson, paceSeriesJson, paceStreamJson,
 * splitsJson). Nach unserer 2-Jahres-Retention ist das ein vertretbarer Datenumfang (~104
 * Trainings * ~50-100 KB = max 15 MB).
 *
 * version: erlaubt Schema-Evolution analog zu BackupPayload. 1 = initiale Version (2026-05-19)
 */
@Serializable
data class WorkoutsBackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val workouts: List<BackupAmazfitWorkout>,
)

@Serializable
data class BackupHealthConnectValue(val metric: String, val timestampMs: Long, val value: Double)

/**
 * Vollstaendige Snapshot-Repraesentation eines Amazfit/Zepp-Workouts inkl. aller Detail-Streams.
 * Felder 1:1 wie in AmazfitWorkoutEntity — bei Schema- Erweiterungen der Entity muessen die neuen
 * Felder hier ergaenzt werden.
 */
@Serializable
data class BackupAmazfitWorkout(
    val trackId: String,
    val dateKey: String,
    val startMs: Long,
    val endMs: Long,
    val durationSeconds: Long? = null,
    val sportType: Int? = null,
    val sportName: String? = null,
    val distanceMeters: Double? = null,
    val avgPaceSecPerKm: Double? = null,
    val maxPaceSecPerKm: Double? = null,
    val avgSpeedKmh: Double? = null,
    val maxSpeedKmh: Double? = null,
    val calories: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val gpsTrackJson: String? = null,
    val heartRateSeriesJson: String? = null,
    val paceSeriesJson: String? = null,
    val splitsJson: String? = null,
    val altitudeGainMeters: Double? = null,
    val altitudeLossMeters: Double? = null,
    val trainingEffectAerobic: Double? = null,
    val trainingEffectAnaerobic: Double? = null,
    val vo2Max: Double? = null,
    val cadence: Int? = null,
    val strideLengthCm: Int? = null,
    val recoveryTimeHours: Int? = null,
    val skinTempCelsius: Double? = null,
    val swolf: Int? = null,
    val poolLaps: Int? = null,
    val poolLengthMeters: Double? = null,
    val source: String? = null,
    val city: String? = null,
    val paceStreamJson: String? = null,
    /**
     * Schema v11 (Frank-Bugfix 2026-05-22): Schutz-Stempel fuer manuell editierte
     * Workouts. Bevor dieses Feld im Backup landete, gingen manuell gesetzte
     * Werte (avgHeartRate, maxHeartRate, Pace, Hoehe, Cadence, Stride, Kalorien)
     * nach Restore + Re-Sync verloren weil der Trainings-Sync den Schutz nur
     * bei manualOverridesMs != null aktiviert. Default null = kein Edit oder
     * Restore aus altem Backup-Format.
     */
    val manualOverridesMs: Long? = null,
    /**
     * Schema v11: Komma-separierte Labels der manuell editierten Felder
     * (z.B. "Ø Puls,Maximalpuls"). Wird im Detail-Screen fuer das
     * Schloss-Icon pro Stat-Karte gebraucht.
     */
    val manualOverrideFields: String? = null,
    val createdAt: Long,
)

/**
 * Schema v11 (Frank-Bugfix 2026-05-22): 1:1-Spiegel der AmazfitDailyEntity
 * (PAI, BioCharge, Hauttemperatur, SpO2, Stress, Schritte, Ruhepuls, HRV,
 * Schlaf-Felder). Vor v11 war diese Tabelle GAR NICHT im Backup — alle Werte
 * gingen nach Reinstall verloren. Zepp-Cloud-API wurde 2026-05-17 entfernt,
 * d.h. ohne dieses Backup-Feld waeren die historischen T-Rex-3-Daten dauerhaft
 * weg sobald die App neu installiert wird. Volumen klein (~ein Eintrag pro
 * Tag * 730 Tage Retention = ~365 KB JSON).
 */
@Serializable
data class BackupAmazfitDaily(
    val date: String,
    val capturedAt: Long,
    val paiScore: Int? = null,
    val bioChargeScore: Int? = null,
    val skinTempCelsius: Double? = null,
    val spo2Percent: Double? = null,
    val stressScore: Int? = null,
    val respiratoryRate: Double? = null,
    val steps: Int? = null,
    val distanceMeters: Double? = null,
    val activeCalories: Double? = null,
    val activeMinutes: Int? = null,
    val restingHeartRate: Int? = null,
    val averageHeartRate: Int? = null,
    val hrvMs: Double? = null,
    val sleepTotalMinutes: Int? = null,
    val sleepDeepMinutes: Int? = null,
    val sleepLightMinutes: Int? = null,
    val sleepWakeMinutes: Int? = null,
    val sleepRemMinutes: Int? = null,
    val sleepScore: Int? = null,
    val createdAt: Long,
)

@Serializable
data class BackupEntry(
    val id: String,
    val rawTranscript: String,
    val title: String,
    val description: String,
    val category: String,
    val severity: Int,
    val priorityScore: Double,
    val priorityReason: String,
    val status: String,
    val timeBucket: String,
    val estimatedDurationMinutes: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long? = null,
    val tags: List<String> = emptyList(),
    val aiNotes: String? = null,
    val source: String,
    val biomarkerSnapshotId: String? = null,
    /** Manueller Bucket-Override (Frank-Wunsch 2026-05-09). null = KI-Zuordnung. */
    val manualBucket: String? = null,
    val manualBucketSetAt: Long? = null,
    /**
     * Frank-Wunsch 2026-05-22 (zweite Iteration): Marker ob estimatedDurationMinutes
     * manuell vom Benutzer gesetzt wurde. Default false damit alte Backups als
     * KI-Schaetzung gelten (Rescore darf sie ueberschreiben).
     */
    val durationManuallySet: Boolean = false,
    /**
     * Frank-Wunsch 2026-05-22 (dritte Iteration): Frist als epoch ms. null = keine
     * Frist. Beeinflusst Prio-Berechnung — siehe ProcessEntryUseCase.
     */
    val dueAtMs: Long? = null,
    /**
     * Frank-Wunsch 2026-05-31: manuell gesetzte Prioritaet (0..100). null = KI.
     * Default null damit alte Backups weiter die KI-Prioritaet nutzen.
     */
    val manualPriorityScore: Double? = null,
    /**
     * Frank-Wunsch 2026-06-19: Marker ob die Prio an der Instanz manuell gesetzt wurde
     * (schlaegt die Loop-Pflege). Default null damit alte Backups als Template/KI gelten.
     */
    val manualPriorityScoreSetAt: Long? = null,
    /**
     * Schema v19 (Frank-Wunsch 2026-06-20, ID-Architektur robust): Herkunft der ANGENOMMENEN Aufgabe
     * mitsichern. Vorher ging sie beim Drive-Backup verloren -> auf einem 2. Geraet hatte die
     * angenommene Aufgabe rootId=null -> der Ketten-Dedup ueber die Endpunkte (countByRootId) war
     * geraeteuebergreifend wirkungslos und ein angenommener Vorschlag wurde erneut vorgeschlagen.
     * Default null = abwaertskompatibel (alte v18-Backups laden weiter, der Backfill verwurzelt sie).
     */
    val originId: String? = null,
    val originType: String? = null,
    val rootId: String? = null,
)

@Serializable
data class BackupInsight(
    val id: String,
    val title: String,
    val description: String,
    val targetCategory: String,
    val additionalCategories: List<String> = emptyList(),
    val confidence: Int,
    val successCount: Int,
    val attemptCount: Int,
    val avgBiomarkerImpact: String? = null,
    val avgFeltImpact: Double? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sourceHypothesisIds: List<String> = emptyList(),
    val manualSource: Boolean = false,
)

@Serializable
data class BackupMemory(
    val id: String,
    val content: String,
    val source: String,
    val isActive: Boolean,
    val confidence: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupHypothesis(
    val id: String,
    val title: String,
    val description: String,
    val rationale: String,
    val createdAt: Long,
    val plannedStartDate: Long,
    val plannedEndDate: Long,
    val actualStartDate: Long? = null,
    val actualEndDate: Long? = null,
    val status: String,
    val outcome: String? = null,
    val outcomeNotes: String? = null,
    val biomarkerBeforeId: String? = null,
    val biomarkerAfterId: String? = null,
    val felltEntropyChange: Int? = null,
    val relatedEntryIds: List<String> = emptyList(),
)

@Serializable
data class BackupScientistSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val isArchived: Boolean,
)

@Serializable
data class BackupScientistMessage(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
    val attachedHypothesisIds: List<String> = emptyList(),
)

@Serializable
data class BackupHypothesisMessage(
    val id: String,
    val hypothesisId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)

// ---------- Entity → Backup ----------

fun EntropyEntryEntity.toBackup(): BackupEntry =
    BackupEntry(
        id = id,
        rawTranscript = rawTranscript,
        title = title,
        description = description,
        category = category.name,
        severity = severity,
        priorityScore = priorityScore,
        priorityReason = priorityReason,
        status = status.name,
        timeBucket = timeBucket.name,
        estimatedDurationMinutes = estimatedDurationMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        resolvedAt = resolvedAt,
        tags = tags,
        aiNotes = aiNotes,
        source = source.name,
        biomarkerSnapshotId = biomarkerSnapshotId,
        manualBucket = manualBucket?.name,
        manualBucketSetAt = manualBucketSetAt,
        durationManuallySet = durationManuallySet,
        dueAtMs = dueAtMs,
        manualPriorityScore = manualPriorityScore,
        manualPriorityScoreSetAt = manualPriorityScoreSetAt,
        // Schema v19: Herkunft der angenommenen Aufgabe mitsichern (Ketten-Dedup cross-device).
        originId = originId,
        originType = originType,
        rootId = rootId,
    )

fun InsightEntity.toBackup(): BackupInsight =
    BackupInsight(
        id = id,
        title = title,
        description = description,
        targetCategory = targetCategory.name,
        additionalCategories = additionalCategories.map { it.name },
        confidence = confidence,
        successCount = successCount,
        attemptCount = attemptCount,
        avgBiomarkerImpact = avgBiomarkerImpact,
        avgFeltImpact = avgFeltImpact,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceHypothesisIds = sourceHypothesisIds,
        manualSource = manualSource,
    )

fun MemoryEntryEntity.toBackup(): BackupMemory =
    BackupMemory(
        id = id,
        content = content,
        source = source.name,
        isActive = isActive,
        confidence = confidence,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun HypothesisEntity.toBackup(): BackupHypothesis =
    BackupHypothesis(
        id = id,
        title = title,
        description = description,
        rationale = rationale,
        createdAt = createdAt,
        plannedStartDate = plannedStartDate,
        plannedEndDate = plannedEndDate,
        actualStartDate = actualStartDate,
        actualEndDate = actualEndDate,
        status = status.name,
        outcome = outcome?.name,
        outcomeNotes = outcomeNotes,
        biomarkerBeforeId = biomarkerBeforeId,
        biomarkerAfterId = biomarkerAfterId,
        felltEntropyChange = felltEntropyChange,
        relatedEntryIds = relatedEntryIds,
    )

fun ScientistSessionEntity.toBackup(): BackupScientistSession =
    BackupScientistSession(
        id = id,
        title = title,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
        isArchived = isArchived,
    )

fun ScientistMessageEntity.toBackup(): BackupScientistMessage =
    BackupScientistMessage(
        id = id,
        sessionId = sessionId,
        role = role.name,
        content = content,
        createdAt = createdAt,
        attachedHypothesisIds = attachedHypothesisIds,
    )

fun HypothesisMessageEntity.toBackup(): BackupHypothesisMessage =
    BackupHypothesisMessage(
        id = id,
        hypothesisId = hypothesisId,
        role = role.name,
        content = content,
        createdAt = createdAt,
    )

/**
 * Mapping aus der schlanken Backup-Projektion. Streams sind hier per SELECT bereits ausgeschlossen
 * — kein RAM-Druck.
 */
fun de.frank.entropyreducer.data.local.dao.AmazfitWorkoutBackupRow.toBackup():
    BackupAmazfitWorkout =
    BackupAmazfitWorkout(
        trackId = trackId,
        dateKey = dateKey,
        startMs = startMs,
        endMs = endMs,
        durationSeconds = durationSeconds,
        sportType = sportType,
        sportName = sportName,
        distanceMeters = distanceMeters,
        avgPaceSecPerKm = avgPaceSecPerKm,
        maxPaceSecPerKm = maxPaceSecPerKm,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        calories = calories,
        avgHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        gpsTrackJson = null,
        heartRateSeriesJson = null,
        paceSeriesJson = null,
        splitsJson = null,
        altitudeGainMeters = altitudeGainMeters,
        altitudeLossMeters = altitudeLossMeters,
        trainingEffectAerobic = trainingEffectAerobic,
        trainingEffectAnaerobic = trainingEffectAnaerobic,
        vo2Max = vo2Max,
        cadence = cadence,
        strideLengthCm = strideLengthCm,
        recoveryTimeHours = recoveryTimeHours,
        skinTempCelsius = skinTempCelsius,
        swolf = swolf,
        poolLaps = poolLaps,
        poolLengthMeters = poolLengthMeters,
        source = source,
        city = city,
        paceStreamJson = null,
        // Slim-Projection liest die Felder nicht aus der DB (SELECT-Spalten
        // sind explizit ohne manualOverridesMs/Fields). Default null reicht
        // — die volle Variante AmazfitWorkoutEntity.toBackup() liefert die
        // korrekten Werte fuer das separate Workouts-Backup. Hauptbackup
        // nutzt die Slim-Variante ohnehin nicht mehr (siehe SyncCoordinator).
        manualOverridesMs = null,
        manualOverrideFields = null,
        createdAt = createdAt,
    )

/**
 * Frank-Wunsch 2026-05-19: Vollstaendige Backup-Repraesentation INKLUSIVE aller Stream-Felder
 * (gpsTrackJson, heartRateSeriesJson, paceSeriesJson, paceStreamJson, splitsJson). Wird vom
 * separaten Workouts-Backup-File (`entropy_reducer_workouts_v1.json`) genutzt — das Haupt-Backup
 * bleibt schlank dank der 2-Jahres-Retention (~104 Trainings statt 956).
 *
 * Frueher (vor 2026-05-19) wurden die Streams pauschal auf null gesetzt um OOM bei 956+
 * Polar-Bulk-Trainings zu vermeiden. Mit der jetzigen Retention ist das Volumen unter ~15 MB
 * insgesamt — kein OOM-Risiko mehr.
 */
fun AmazfitWorkoutEntity.toBackup(): BackupAmazfitWorkout =
    BackupAmazfitWorkout(
        trackId = trackId,
        dateKey = dateKey,
        startMs = startMs,
        endMs = endMs,
        durationSeconds = durationSeconds,
        sportType = sportType,
        sportName = sportName,
        distanceMeters = distanceMeters,
        avgPaceSecPerKm = avgPaceSecPerKm,
        maxPaceSecPerKm = maxPaceSecPerKm,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        calories = calories,
        avgHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        gpsTrackJson = gpsTrackJson,
        heartRateSeriesJson = heartRateSeriesJson,
        paceSeriesJson = paceSeriesJson,
        splitsJson = splitsJson,
        altitudeGainMeters = altitudeGainMeters,
        altitudeLossMeters = altitudeLossMeters,
        trainingEffectAerobic = trainingEffectAerobic,
        trainingEffectAnaerobic = trainingEffectAnaerobic,
        vo2Max = vo2Max,
        cadence = cadence,
        strideLengthCm = strideLengthCm,
        recoveryTimeHours = recoveryTimeHours,
        skinTempCelsius = skinTempCelsius,
        swolf = swolf,
        poolLaps = poolLaps,
        poolLengthMeters = poolLengthMeters,
        source = source,
        city = city,
        paceStreamJson = paceStreamJson,
        // Schema v11: Schutz-Stempel und Feld-Tracking mitsichern damit
        // der Trainings-Sync nach Restore die manuellen Edits weiterhin schuetzt
        // (siehe AmazfitRepository: isManual-Check).
        manualOverridesMs = manualOverridesMs,
        manualOverrideFields = manualOverrideFields,
        createdAt = createdAt,
    )

// ---------- Backup → Entity ----------

/**
 * Mapping fuer Bucket-Strings aus aelteren Backup-Versionen (Frank-Wunsch 2026-05-09). DIESE_WOCHE
 * und DIESEN_MONAT existieren nicht mehr — sie werden auf FREIBLOCK gemappt damit alte
 * Drive-Backups nichts verlieren. Unbekannte Werte fallen auf SPAETER zurueck.
 */
private fun parseBucketCompat(name: String?): TimeBucket {
    if (name == null) return TimeBucket.HEUTE
    return runCatching { TimeBucket.valueOf(name) }
        .getOrElse {
            when (name) {
                "DIESE_WOCHE",
                "DIESEN_MONAT" -> TimeBucket.FREIBLOCK
                else -> TimeBucket.SPAETER
            }
        }
}

private fun parseCategoryCompat(name: String): EntropyCategory =
    runCatching { EntropyCategory.valueOf(name) }.getOrDefault(EntropyCategory.SONSTIGES)

fun BackupEntry.toEntity(): EntropyEntryEntity =
    EntropyEntryEntity(
        id = id,
        rawTranscript = rawTranscript,
        title = title,
        description = description,
        category = parseCategoryCompat(category),
        severity = severity.coerceIn(1, 10),
        priorityScore = priorityScore.coerceIn(0.0, 100.0),
        priorityReason = priorityReason,
        status = runCatching { EntryStatus.valueOf(status) }.getOrDefault(EntryStatus.OFFEN),
        timeBucket = parseBucketCompat(timeBucket),
        estimatedDurationMinutes = estimatedDurationMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        resolvedAt = resolvedAt,
        tags = tags,
        aiNotes = aiNotes,
        source = runCatching { EntrySource.valueOf(source) }.getOrDefault(EntrySource.NUTZER_TEXT),
        biomarkerSnapshotId = biomarkerSnapshotId,
        manualBucket = manualBucket?.let { parseBucketCompat(it) },
        manualBucketSetAt = manualBucketSetAt,
        durationManuallySet = durationManuallySet,
        dueAtMs = dueAtMs,
        manualPriorityScore = manualPriorityScore?.coerceIn(0.0, 100.0),
        manualPriorityScoreSetAt = manualPriorityScoreSetAt,
        // Schema v19: Herkunft der angenommenen Aufgabe aus dem Backup zurueckholen. Alte v18-Backups
        // liefern null -> der LineageBackfillMigrator verwurzelt sie beim naechsten Start selbst.
        originId = originId,
        originType = originType,
        rootId = rootId,
    )

fun BackupInsight.toEntity(): InsightEntity =
    InsightEntity(
        id = id,
        title = title,
        description = description,
        targetCategory = parseCategoryCompat(targetCategory),
        additionalCategories =
            additionalCategories
                .map { parseCategoryCompat(it) }
                .filter { it != parseCategoryCompat(targetCategory) }
                .distinct(),
        confidence = confidence.coerceIn(0, 100),
        successCount = successCount,
        attemptCount = attemptCount,
        avgBiomarkerImpact = avgBiomarkerImpact,
        avgFeltImpact = avgFeltImpact,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceHypothesisIds = sourceHypothesisIds,
        manualSource = manualSource,
    )

fun BackupMemory.toEntity(): MemoryEntryEntity =
    MemoryEntryEntity(
        id = id,
        content = content,
        source = runCatching { MemorySource.valueOf(source) }.getOrDefault(MemorySource.MANUELL),
        isActive = isActive,
        confidence = confidence.coerceIn(0, 100),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BackupHypothesis.toEntity(): HypothesisEntity =
    HypothesisEntity(
        id = id,
        title = title,
        description = description,
        rationale = rationale,
        createdAt = createdAt,
        plannedStartDate = plannedStartDate,
        plannedEndDate = plannedEndDate,
        actualStartDate = actualStartDate,
        actualEndDate = actualEndDate,
        status =
            runCatching { HypothesisStatus.valueOf(status) }
                .getOrDefault(HypothesisStatus.VORGESCHLAGEN),
        outcome = outcome?.let { runCatching { HypothesisOutcome.valueOf(it) }.getOrNull() },
        outcomeNotes = outcomeNotes,
        biomarkerBeforeId = biomarkerBeforeId,
        biomarkerAfterId = biomarkerAfterId,
        felltEntropyChange = felltEntropyChange,
        relatedEntryIds = relatedEntryIds,
    )

fun BackupScientistSession.toEntity(): ScientistSessionEntity =
    ScientistSessionEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        lastActiveAt = lastActiveAt,
        isArchived = isArchived,
    )

fun BackupScientistMessage.toEntity(): ScientistMessageEntity =
    ScientistMessageEntity(
        id = id,
        sessionId = sessionId,
        role = runCatching { ScientistRole.valueOf(role) }.getOrDefault(ScientistRole.NUTZER),
        content = content,
        createdAt = createdAt,
        attachedHypothesisIds = attachedHypothesisIds,
    )

fun BackupHypothesisMessage.toEntity(): HypothesisMessageEntity =
    HypothesisMessageEntity(
        id = id,
        hypothesisId = hypothesisId,
        role = runCatching { ScientistRole.valueOf(role) }.getOrDefault(ScientistRole.NUTZER),
        content = content,
        createdAt = createdAt,
    )

fun BackupAmazfitWorkout.toEntity(): AmazfitWorkoutEntity =
    AmazfitWorkoutEntity(
        trackId = trackId,
        dateKey = dateKey,
        startMs = startMs,
        endMs = endMs,
        durationSeconds = durationSeconds,
        sportType = sportType,
        sportName = sportName,
        distanceMeters = distanceMeters,
        avgPaceSecPerKm = avgPaceSecPerKm,
        maxPaceSecPerKm = maxPaceSecPerKm,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        calories = calories,
        avgHeartRate = avgHeartRate,
        maxHeartRate = maxHeartRate,
        gpsTrackJson = gpsTrackJson,
        heartRateSeriesJson = heartRateSeriesJson,
        paceSeriesJson = paceSeriesJson,
        splitsJson = splitsJson,
        altitudeGainMeters = altitudeGainMeters,
        altitudeLossMeters = altitudeLossMeters,
        trainingEffectAerobic = trainingEffectAerobic,
        trainingEffectAnaerobic = trainingEffectAnaerobic,
        vo2Max = vo2Max,
        cadence = cadence,
        strideLengthCm = strideLengthCm,
        recoveryTimeHours = recoveryTimeHours,
        skinTempCelsius = skinTempCelsius,
        swolf = swolf,
        poolLaps = poolLaps,
        poolLengthMeters = poolLengthMeters,
        source = source,
        city = city,
        paceStreamJson = paceStreamJson,
        // Schema v11: Schutz-Stempel + Feld-Labels uebernehmen damit
        // der Trainings-Sync nach dem Restore die manuellen Edits nicht
        // ueberschreibt. Alte Backups (v1-v10) liefern hier null —
        // der Schutz greift dann nicht und der naechste Trainings-Sync
        // gewinnt, was bei alten Backups akzeptabel ist (vorher gab es
        // diesen Schutz auf alten Geraeten ebenfalls nicht).
        manualOverridesMs = manualOverridesMs,
        manualOverrideFields = manualOverrideFields,
        createdAt = createdAt,
    )

// =========================================================================
// Health-Backup (Frank-Wunsch 2026-05-19): separate Drive-Datei
// `entropy_reducer_health_v1.json` fuer Whoop- und Oura-Daten. Verhindert
// Datenverlust nach Reinstall — Whoop liefert nur 90 Tage rueckwirkend,
// Oura nur 6 Monate. Volumen ca. 1-3 MB fuer 2 Jahre (keine GPS-Streams,
// nur Daily-Summary plus 5-Min-Schlafphasen-Strings).
// =========================================================================

/**
 * Komplettes Health-Backup mit Whoop (Daily Recovery + Workouts) und Oura (6 Tabellen). Wird als
 * eigene Drive-Datei `entropy_reducer_health_v1.json` im appDataFolder gespeichert, parallel zum
 * Haupt- und Workouts-Backup.
 *
 * version: 1 = initiale Version (Frank-Wunsch 2026-05-19).
 */
@Serializable
data class HealthBackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val whoopSnapshots: List<BackupBiomarkerSnapshot> = emptyList(),
    val whoopWorkouts: List<BackupWhoopWorkout> = emptyList(),
    val ouraReadiness: List<BackupOuraReadiness> = emptyList(),
    val ouraDailySleep: List<BackupOuraDailySleep> = emptyList(),
    val ouraActivity: List<BackupOuraActivity> = emptyList(),
    val ouraResilience: List<BackupOuraResilience> = emptyList(),
    val ouraSleepDetail: List<BackupOuraSleepDetail> = emptyList(),
    val ouraPersonalInfo: BackupOuraPersonalInfo? = null,
)

// ---------- Whoop Datenklassen ----------

/** 1:1 Spiegel der BiomarkerSnapshotEntity (Whoop Daily Recovery). */
@Serializable
data class BackupBiomarkerSnapshot(
    val id: String,
    val capturedAt: Long,
    val recoveryScore: Int? = null,
    val hrvMs: Double? = null,
    val restingHeartRate: Int? = null,
    val sleepPerformance: Int? = null,
    val sleepTotalMinutes: Int? = null,
    val sleepRemMinutes: Int? = null,
    val sleepDeepMinutes: Int? = null,
    val sleepLightMinutes: Int? = null,
    val sleepAwakeMinutes: Int? = null,
    val sleepDisturbances: Int? = null,
    val dayStrain: Double? = null,
    val dayKilojoules: Double? = null,
    val createdAt: Long,
    val respiratoryRate: Double? = null,
    val sleepConsistencyPercent: Int? = null,
    val sleepEfficiencyPercent: Int? = null,
    val sleepNeedMinutes: Int? = null,
    val sleepDebtMinutes: Int? = null,
    val spo2Percent: Double? = null,
    val skinTempCelsius: Double? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val sleepCycleCount: Int? = null,
)

/** 1:1 Spiegel der WhoopWorkoutEntity. */
@Serializable
data class BackupWhoopWorkout(
    val id: String,
    val dateKey: String,
    val startMs: Long,
    val endMs: Long,
    val sportId: Int? = null,
    val sportName: String? = null,
    val strain: Double? = null,
    val kilojoule: Double? = null,
    val averageHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val percentRecorded: Double? = null,
    val distanceMeter: Double? = null,
    val altitudeGainMeter: Double? = null,
    val zoneZeroMilli: Long? = null,
    val zoneOneMilli: Long? = null,
    val zoneTwoMilli: Long? = null,
    val zoneThreeMilli: Long? = null,
    val zoneFourMilli: Long? = null,
    val zoneFiveMilli: Long? = null,
    val createdAt: Long,
)

// ---------- Oura Datenklassen ----------

@Serializable
data class BackupOuraReadiness(
    val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val temperatureDeviation: Double? = null,
    val temperatureTrendDeviation: Double? = null,
    val activityBalance: Int? = null,
    val bodyTemperature: Int? = null,
    val hrvBalance: Int? = null,
    val previousDayActivity: Int? = null,
    val previousNight: Int? = null,
    val recoveryIndex: Int? = null,
    val restingHeartRate: Int? = null,
    val sleepBalance: Int? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraDailySleep(
    val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val deepSleepScore: Int? = null,
    val efficiencyScore: Int? = null,
    val latencyScore: Int? = null,
    val remSleepScore: Int? = null,
    val restfulnessScore: Int? = null,
    val timingScore: Int? = null,
    val totalSleepScore: Int? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraActivity(
    val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val activeCalories: Int? = null,
    val totalCalories: Int? = null,
    val targetCalories: Int? = null,
    val steps: Int? = null,
    val walkingDistanceMeters: Int? = null,
    val highActivitySeconds: Int? = null,
    val mediumActivitySeconds: Int? = null,
    val lowActivitySeconds: Int? = null,
    val nonWearSeconds: Int? = null,
    val restingSeconds: Int? = null,
    val sedentarySeconds: Int? = null,
    val inactivityAlerts: Int? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraResilience(
    val day: String,
    val capturedAt: Long,
    val level: String? = null,
    val sleepRecovery: Double? = null,
    val daytimeRecovery: Double? = null,
    val stress: Double? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraSleepDetail(
    val id: String,
    val day: String,
    val capturedAt: Long,
    val bedtimeStart: String? = null,
    val bedtimeEnd: String? = null,
    val type: String? = null,
    val totalSleepSeconds: Int? = null,
    val timeInBedSeconds: Int? = null,
    val awakeSeconds: Int? = null,
    val lightSeconds: Int? = null,
    val deepSeconds: Int? = null,
    val remSeconds: Int? = null,
    val efficiency: Int? = null,
    val latencySeconds: Int? = null,
    val restlessPeriods: Int? = null,
    val averageBreath: Double? = null,
    val averageHeartRate: Double? = null,
    val averageHrv: Int? = null,
    val lowestHeartRate: Int? = null,
    val sleepPhase5Min: String? = null,
    val createdAt: Long,
)

@Serializable
data class BackupOuraPersonalInfo(
    val id: Long = 1L,
    val ouraUserId: String? = null,
    val age: Int? = null,
    val weightKg: Double? = null,
    val heightMeters: Double? = null,
    val biologicalSex: String? = null,
    val email: String? = null,
    val capturedAt: Long,
)

// ---------- Entity → Backup Mappings ----------

fun BiomarkerSnapshotEntity.toBackup(): BackupBiomarkerSnapshot =
    BackupBiomarkerSnapshot(
        id = id,
        capturedAt = capturedAt,
        recoveryScore = recoveryScore,
        hrvMs = hrvMs,
        restingHeartRate = restingHeartRate,
        sleepPerformance = sleepPerformance,
        sleepTotalMinutes = sleepTotalMinutes,
        sleepRemMinutes = sleepRemMinutes,
        sleepDeepMinutes = sleepDeepMinutes,
        sleepLightMinutes = sleepLightMinutes,
        sleepAwakeMinutes = sleepAwakeMinutes,
        sleepDisturbances = sleepDisturbances,
        dayStrain = dayStrain,
        dayKilojoules = dayKilojoules,
        createdAt = createdAt,
        respiratoryRate = respiratoryRate,
        sleepConsistencyPercent = sleepConsistencyPercent,
        sleepEfficiencyPercent = sleepEfficiencyPercent,
        sleepNeedMinutes = sleepNeedMinutes,
        sleepDebtMinutes = sleepDebtMinutes,
        spo2Percent = spo2Percent,
        skinTempCelsius = skinTempCelsius,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        sleepCycleCount = sleepCycleCount,
    )

fun WhoopWorkoutEntity.toBackup(): BackupWhoopWorkout =
    BackupWhoopWorkout(
        id = id,
        dateKey = dateKey,
        startMs = startMs,
        endMs = endMs,
        sportId = sportId,
        sportName = sportName,
        strain = strain,
        kilojoule = kilojoule,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        percentRecorded = percentRecorded,
        distanceMeter = distanceMeter,
        altitudeGainMeter = altitudeGainMeter,
        zoneZeroMilli = zoneZeroMilli,
        zoneOneMilli = zoneOneMilli,
        zoneTwoMilli = zoneTwoMilli,
        zoneThreeMilli = zoneThreeMilli,
        zoneFourMilli = zoneFourMilli,
        zoneFiveMilli = zoneFiveMilli,
        createdAt = createdAt,
    )

fun OuraReadinessEntity.toBackup(): BackupOuraReadiness =
    BackupOuraReadiness(
        day = day,
        capturedAt = capturedAt,
        score = score,
        temperatureDeviation = temperatureDeviation,
        temperatureTrendDeviation = temperatureTrendDeviation,
        activityBalance = activityBalance,
        bodyTemperature = bodyTemperature,
        hrvBalance = hrvBalance,
        previousDayActivity = previousDayActivity,
        previousNight = previousNight,
        recoveryIndex = recoveryIndex,
        restingHeartRate = restingHeartRate,
        sleepBalance = sleepBalance,
        createdAt = createdAt,
    )

fun OuraDailySleepEntity.toBackup(): BackupOuraDailySleep =
    BackupOuraDailySleep(
        day = day,
        capturedAt = capturedAt,
        score = score,
        deepSleepScore = deepSleepScore,
        efficiencyScore = efficiencyScore,
        latencyScore = latencyScore,
        remSleepScore = remSleepScore,
        restfulnessScore = restfulnessScore,
        timingScore = timingScore,
        totalSleepScore = totalSleepScore,
        createdAt = createdAt,
    )

fun OuraActivityEntity.toBackup(): BackupOuraActivity =
    BackupOuraActivity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        activeCalories = activeCalories,
        totalCalories = totalCalories,
        targetCalories = targetCalories,
        steps = steps,
        walkingDistanceMeters = walkingDistanceMeters,
        highActivitySeconds = highActivitySeconds,
        mediumActivitySeconds = mediumActivitySeconds,
        lowActivitySeconds = lowActivitySeconds,
        nonWearSeconds = nonWearSeconds,
        restingSeconds = restingSeconds,
        sedentarySeconds = sedentarySeconds,
        inactivityAlerts = inactivityAlerts,
        createdAt = createdAt,
    )

fun OuraResilienceEntity.toBackup(): BackupOuraResilience =
    BackupOuraResilience(
        day = day,
        capturedAt = capturedAt,
        level = level,
        sleepRecovery = sleepRecovery,
        daytimeRecovery = daytimeRecovery,
        stress = stress,
        createdAt = createdAt,
    )

fun OuraSleepDetailEntity.toBackup(): BackupOuraSleepDetail =
    BackupOuraSleepDetail(
        id = id,
        day = day,
        capturedAt = capturedAt,
        bedtimeStart = bedtimeStart,
        bedtimeEnd = bedtimeEnd,
        type = type,
        totalSleepSeconds = totalSleepSeconds,
        timeInBedSeconds = timeInBedSeconds,
        awakeSeconds = awakeSeconds,
        lightSeconds = lightSeconds,
        deepSeconds = deepSeconds,
        remSeconds = remSeconds,
        efficiency = efficiency,
        latencySeconds = latencySeconds,
        restlessPeriods = restlessPeriods,
        averageBreath = averageBreath,
        averageHeartRate = averageHeartRate,
        averageHrv = averageHrv,
        lowestHeartRate = lowestHeartRate,
        sleepPhase5Min = sleepPhase5Min,
        createdAt = createdAt,
    )

fun OuraPersonalInfoEntity.toBackup(): BackupOuraPersonalInfo =
    BackupOuraPersonalInfo(
        id = id,
        ouraUserId = ouraUserId,
        age = age,
        weightKg = weightKg,
        heightMeters = heightMeters,
        biologicalSex = biologicalSex,
        email = email,
        capturedAt = capturedAt,
    )

// ---------- Backup → Entity Mappings ----------

fun BackupBiomarkerSnapshot.toEntity(): BiomarkerSnapshotEntity =
    BiomarkerSnapshotEntity(
        id = id,
        capturedAt = capturedAt,
        recoveryScore = recoveryScore,
        hrvMs = hrvMs,
        restingHeartRate = restingHeartRate,
        sleepPerformance = sleepPerformance,
        sleepTotalMinutes = sleepTotalMinutes,
        sleepRemMinutes = sleepRemMinutes,
        sleepDeepMinutes = sleepDeepMinutes,
        sleepLightMinutes = sleepLightMinutes,
        sleepAwakeMinutes = sleepAwakeMinutes,
        sleepDisturbances = sleepDisturbances,
        dayStrain = dayStrain,
        dayKilojoules = dayKilojoules,
        createdAt = createdAt,
        respiratoryRate = respiratoryRate,
        sleepConsistencyPercent = sleepConsistencyPercent,
        sleepEfficiencyPercent = sleepEfficiencyPercent,
        sleepNeedMinutes = sleepNeedMinutes,
        sleepDebtMinutes = sleepDebtMinutes,
        spo2Percent = spo2Percent,
        skinTempCelsius = skinTempCelsius,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        sleepCycleCount = sleepCycleCount,
    )

fun BackupWhoopWorkout.toEntity(): WhoopWorkoutEntity =
    WhoopWorkoutEntity(
        id = id,
        dateKey = dateKey,
        startMs = startMs,
        endMs = endMs,
        sportId = sportId,
        sportName = sportName,
        strain = strain,
        kilojoule = kilojoule,
        averageHeartRate = averageHeartRate,
        maxHeartRate = maxHeartRate,
        percentRecorded = percentRecorded,
        distanceMeter = distanceMeter,
        altitudeGainMeter = altitudeGainMeter,
        zoneZeroMilli = zoneZeroMilli,
        zoneOneMilli = zoneOneMilli,
        zoneTwoMilli = zoneTwoMilli,
        zoneThreeMilli = zoneThreeMilli,
        zoneFourMilli = zoneFourMilli,
        zoneFiveMilli = zoneFiveMilli,
        createdAt = createdAt,
    )

fun BackupOuraReadiness.toEntity(): OuraReadinessEntity =
    OuraReadinessEntity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        temperatureDeviation = temperatureDeviation,
        temperatureTrendDeviation = temperatureTrendDeviation,
        activityBalance = activityBalance,
        bodyTemperature = bodyTemperature,
        hrvBalance = hrvBalance,
        previousDayActivity = previousDayActivity,
        previousNight = previousNight,
        recoveryIndex = recoveryIndex,
        restingHeartRate = restingHeartRate,
        sleepBalance = sleepBalance,
        createdAt = createdAt,
    )

fun BackupOuraDailySleep.toEntity(): OuraDailySleepEntity =
    OuraDailySleepEntity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        deepSleepScore = deepSleepScore,
        efficiencyScore = efficiencyScore,
        latencyScore = latencyScore,
        remSleepScore = remSleepScore,
        restfulnessScore = restfulnessScore,
        timingScore = timingScore,
        totalSleepScore = totalSleepScore,
        createdAt = createdAt,
    )

fun BackupOuraActivity.toEntity(): OuraActivityEntity =
    OuraActivityEntity(
        day = day,
        capturedAt = capturedAt,
        score = score,
        activeCalories = activeCalories,
        totalCalories = totalCalories,
        targetCalories = targetCalories,
        steps = steps,
        walkingDistanceMeters = walkingDistanceMeters,
        highActivitySeconds = highActivitySeconds,
        mediumActivitySeconds = mediumActivitySeconds,
        lowActivitySeconds = lowActivitySeconds,
        nonWearSeconds = nonWearSeconds,
        restingSeconds = restingSeconds,
        sedentarySeconds = sedentarySeconds,
        inactivityAlerts = inactivityAlerts,
        createdAt = createdAt,
    )

fun BackupOuraResilience.toEntity(): OuraResilienceEntity =
    OuraResilienceEntity(
        day = day,
        capturedAt = capturedAt,
        level = level,
        sleepRecovery = sleepRecovery,
        daytimeRecovery = daytimeRecovery,
        stress = stress,
        createdAt = createdAt,
    )

fun BackupOuraSleepDetail.toEntity(): OuraSleepDetailEntity =
    OuraSleepDetailEntity(
        id = id,
        day = day,
        capturedAt = capturedAt,
        bedtimeStart = bedtimeStart,
        bedtimeEnd = bedtimeEnd,
        type = type,
        totalSleepSeconds = totalSleepSeconds,
        timeInBedSeconds = timeInBedSeconds,
        awakeSeconds = awakeSeconds,
        lightSeconds = lightSeconds,
        deepSeconds = deepSeconds,
        remSeconds = remSeconds,
        efficiency = efficiency,
        latencySeconds = latencySeconds,
        restlessPeriods = restlessPeriods,
        averageBreath = averageBreath,
        averageHeartRate = averageHeartRate,
        averageHrv = averageHrv,
        lowestHeartRate = lowestHeartRate,
        sleepPhase5Min = sleepPhase5Min,
        createdAt = createdAt,
    )

fun BackupOuraPersonalInfo.toEntity(): OuraPersonalInfoEntity =
    OuraPersonalInfoEntity(
        id = id,
        ouraUserId = ouraUserId,
        age = age,
        weightKg = weightKg,
        heightMeters = heightMeters,
        biologicalSex = biologicalSex,
        email = email,
        capturedAt = capturedAt,
    )

// =========================================================================
// Schema v6 (Frank-Wunsch 2026-05-20): Tagebuch + Profil + Entropie-Followups
// =========================================================================

fun de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity.toBackup():
    BackupEntropyFollowup =
    BackupEntropyFollowup(
        id = id,
        entryId = entryId,
        rawText = rawText,
        improvedText = improvedText,
        isImproved = isImproved,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun BackupEntropyFollowup.toEntity():
    de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity =
    de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity(
        id = id,
        entryId = entryId,
        rawText = rawText,
        improvedText = improvedText,
        isImproved = isImproved,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

// =========================================================================
// Schema v10 (Sprint 2.8, Frank-Wunsch 2026-05-22): RecurringTemplates
// =========================================================================

fun RecurringTemplateEntity.toBackup(): BackupRecurringTemplate =
    BackupRecurringTemplate(
        id = id,
        title = title,
        description = description,
        category = category.name,
        priorityScore = priorityScore,
        severity = severity,
        estimatedDurationMinutes = estimatedDurationMinutes,
        rrule = rrule,
        timeOfDayMinutes = timeOfDayMinutes,
        untilEpochMs = untilEpochMs,
        nextOccurrenceAt = nextOccurrenceAt,
        lastGeneratedAt = lastGeneratedAt,
        occurrenceCount = occurrenceCount,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        targetBucket = targetBucket?.name,
        intervalDays = intervalDays,
    )

fun BackupRecurringTemplate.toEntity(): RecurringTemplateEntity =
    RecurringTemplateEntity(
        id = id,
        title = title,
        description = description,
        category = parseCategoryCompat(category),
        priorityScore = priorityScore.coerceIn(0, 100),
        severity = severity.coerceIn(1, 10),
        estimatedDurationMinutes = estimatedDurationMinutes,
        rrule = rrule,
        timeOfDayMinutes = timeOfDayMinutes.coerceIn(0, 24 * 60 - 1),
        untilEpochMs = untilEpochMs,
        nextOccurrenceAt = nextOccurrenceAt,
        lastGeneratedAt = lastGeneratedAt,
        occurrenceCount = occurrenceCount.coerceAtLeast(0),
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        // Unbekannte/alte Werte -> null (= KI/automatisch HEUTE). Frank-Wunsch 2026-05-31.
        targetBucket = targetBucket?.let {
            runCatching { de.frank.entropyreducer.domain.model.TimeBucket.valueOf(it) }.getOrNull()
        },
        intervalDays = intervalDays,
    )

// =========================================================================
// Schema v17 (Frank-Wunsch 2026-06-19): Prioritaets-Gedaechtnis
// =========================================================================

fun de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity.toBackup():
    BackupPriorityMemory =
    BackupPriorityMemory(
        id = id,
        title = title,
        description = description,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceEntryId = sourceEntryId,
    )

fun BackupPriorityMemory.toEntity():
    de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity =
    de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity(
        id = id,
        title = title,
        description = description,
        priority = priority.coerceIn(0.0, 100.0),
        createdAt = createdAt,
        updatedAt = updatedAt,
        sourceEntryId = sourceEntryId,
    )

// =========================================================================
// Schema v11 (Frank-Bugfix 2026-05-22): Amazfit-Daily-Werte (T-Rex 3)
// =========================================================================

fun de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity.toBackup():
    BackupAmazfitDaily =
    BackupAmazfitDaily(
        date = date,
        capturedAt = capturedAt,
        paiScore = paiScore,
        bioChargeScore = bioChargeScore,
        skinTempCelsius = skinTempCelsius,
        spo2Percent = spo2Percent,
        stressScore = stressScore,
        respiratoryRate = respiratoryRate,
        steps = steps,
        distanceMeters = distanceMeters,
        activeCalories = activeCalories,
        activeMinutes = activeMinutes,
        restingHeartRate = restingHeartRate,
        averageHeartRate = averageHeartRate,
        hrvMs = hrvMs,
        sleepTotalMinutes = sleepTotalMinutes,
        sleepDeepMinutes = sleepDeepMinutes,
        sleepLightMinutes = sleepLightMinutes,
        sleepWakeMinutes = sleepWakeMinutes,
        sleepRemMinutes = sleepRemMinutes,
        sleepScore = sleepScore,
        createdAt = createdAt,
    )

fun BackupAmazfitDaily.toEntity():
    de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity =
    de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity(
        date = date,
        capturedAt = capturedAt,
        paiScore = paiScore,
        bioChargeScore = bioChargeScore,
        skinTempCelsius = skinTempCelsius,
        spo2Percent = spo2Percent,
        stressScore = stressScore,
        respiratoryRate = respiratoryRate,
        steps = steps,
        distanceMeters = distanceMeters,
        activeCalories = activeCalories,
        activeMinutes = activeMinutes,
        restingHeartRate = restingHeartRate,
        averageHeartRate = averageHeartRate,
        hrvMs = hrvMs,
        sleepTotalMinutes = sleepTotalMinutes,
        sleepDeepMinutes = sleepDeepMinutes,
        sleepLightMinutes = sleepLightMinutes,
        sleepWakeMinutes = sleepWakeMinutes,
        sleepRemMinutes = sleepRemMinutes,
        sleepScore = sleepScore,
        createdAt = createdAt,
    )

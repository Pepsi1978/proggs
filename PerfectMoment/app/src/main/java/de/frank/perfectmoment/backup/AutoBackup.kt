package de.frank.perfectmoment.backup

import de.frank.perfectmoment.data.local.PerfectMomentDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Hält die Sicherung in Google Drive von selbst aktuell.
 *
 * Sobald ein Aufhänger, ein Skill oder eine Sitzung dazukommt, sich ändert oder verschwindet, wird
 * die Sicherung neu geschrieben — es liegt immer nur der aktuelle Stand dort, nichts sammelt sich an.
 *
 * Zwei Bremsen verhindern unnötige Übertragungen: Nach einer Änderung wird kurz gewartet, damit aus
 * mehreren schnellen Bearbeitungen eine Sicherung wird. Und es wird nur hochgeladen, wenn sich der
 * Inhalt gegenüber der letzten Sicherung wirklich unterscheidet — eine laufende Sitzung meldet ihren
 * Fortschritt oft, ohne dass sich am gesicherten Bestand etwas ändert.
 *
 * Ist Google Drive nicht verbunden, passiert still nichts: Ein Anmeldedialog darf nie unaufgefordert
 * im Hintergrund auftauchen.
 */
class AutoBackup(
    private val scope: CoroutineScope,
    private val database: PerfectMomentDatabase,
    private val repository: BackupRepository,
) {

    private var lastSavedContent: Int? = null

    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            combine(
                database.hookDao().observeHooks(),
                database.skillDao().observeSkills(),
                database.sessionDao().observeSessions(),
            ) { hooks, skills, sessions ->
                // Fingerabdruck des sicherungsrelevanten Bestands.
                listOf(
                    hooks.map { "${it.id}:${it.emoji}:${it.text}:${it.sortIndex}" },
                    skills.map { "${it.id}:${it.name}:${it.text}" },
                    sessions.map { "${it.id}:${it.topic}:${it.playCount}" },
                ).hashCode()
            }
                .drop(1) // Der erste Wert ist nur der Ist-Zustand beim Start, keine Änderung.
                .debounce(BACKUP_DELAY_MS)
                .collect { fingerprint ->
                    if (fingerprint == lastSavedContent) return@collect
                    if (!repository.isConnected()) return@collect
                    runCatching { repository.backupNow() }
                        .onSuccess { lastSavedContent = fingerprint }
                }
        }
    }

    private companion object {
        /** Ruhezeit nach der letzten Änderung, bevor gesichert wird. */
        const val BACKUP_DELAY_MS = 20_000L
    }
}

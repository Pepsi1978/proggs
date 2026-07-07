package de.frank.entropyreducer.domain.status

import com.google.common.truth.Truth.assertThat
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.domain.model.TimeBucket
import org.junit.Test

class CalculateStatusUseCaseTest {

    private val useCase = CalculateStatusUseCase()

    @Test fun `Leere Liste ohne Whoop ergibt Maximalstatus`() {
        val result = useCase.calculate(emptyList(), null, emptyList(), null)
        // Aufgaben 100 + Kontext 50 (UNBEKANNT, baseline) → re-skaliert (60% + 40%)
        assertThat(result.total).isAtLeast(75)
        assertThat(result.openEntryCount).isEqualTo(0)
        assertThat(result.biomarkerScore).isNull()
    }

    @Test fun `Ohne Whoop wird Aufgaben+Kontext re-skaliert`() {
        val result = useCase.calculate(emptyList(), null, emptyList(), null)
        // Whoop nicht verbunden — die Komponenten Aufgaben+Kontext werden auf 100 % skaliert.
        assertThat(result.biomarkerScore).isNull()
        assertThat(result.tasksScore).isEqualTo(100)
    }

    @Test fun `Volle Severity reduziert tasksScore auf null`() {
        // Skala 2026-05-09: 100 Punkte Severity = 0%. Also 10 Eintraege x severity 10.
        val entries = (1..10).map { entry(severity = 10, status = EntryStatus.OFFEN) }
        val result = useCase.calculate(entries, null, emptyList(), null)
        assertThat(result.tasksScore).isEqualTo(0)
    }

    @Test fun `Heute reduzierter Eintrag erhoeht tasksScore direkt`() {
        // Mit neuer Skala (Frank-Reklamation 2026-05-09): jeder heute reduzierte
        // Eintrag bringt +5 pro Severity-Punkt direkt ins tasksScore. Bei 5 OFFEN
        // x severity 10 = Last 50 -> baseScore 50. Plus 1 reduzierter mit
        // severity 10 = Bonus 50. Total 100 (geclippt).
        val open = (1..5).map { entry(severity = 10, status = EntryStatus.OFFEN) }
        val resolved = entry(severity = 10, status = EntryStatus.REDUZIERT, resolvedAt = System.currentTimeMillis())
        val result = useCase.calculate(open + resolved, null, emptyList(), null)
        assertThat(result.tasksScore).isAtLeast(80)
    }

    @Test fun `Frei-Tag mit erledigten Aufgaben gibt Bonus`() {
        val today = day(ShiftCode.FREI)
        val resolved = entry(severity = 5, status = EntryStatus.REDUZIERT, resolvedAt = System.currentTimeMillis())
        val result = useCase.calculate(listOf(resolved), null, emptyList(), today)
        assertThat(result.contextScore).isAtLeast(60)
    }

    @Test fun `Tagdienst hat niedrigeren Kontext-Baseline als Frei`() {
        val tagdienst = useCase.calculate(emptyList(), null, emptyList(), day(ShiftCode.TAGDIENST))
        val frei = useCase.calculate(emptyList(), null, emptyList(), day(ShiftCode.FREI))
        assertThat(tagdienst.contextScore).isLessThan(frei.contextScore)
    }

    @Test fun `Mit Whoop wird Biomarker-Komponente eingezogen`() {
        val snapshot = BiomarkerSnapshotEntity(
            id = "x", capturedAt = System.currentTimeMillis(),
            recoveryScore = 70, hrvMs = 45.0, restingHeartRate = 50,
            sleepPerformance = 80, sleepTotalMinutes = 420,
            sleepRemMinutes = 80, sleepDeepMinutes = 60, sleepLightMinutes = 240,
            sleepAwakeMinutes = 40, sleepDisturbances = 5,
            dayStrain = 12.0, dayKilojoules = 8000.0, createdAt = 0L,
        )
        val result = useCase.calculate(emptyList(), snapshot, listOf(snapshot), null)
        assertThat(result.biomarkerScore).isNotNull()
        assertThat(result.biomarkerScore).isAtLeast(50)
    }

    @Test fun `HRV ueber Median erhoeht Biomarker-Score`() {
        val median = BiomarkerSnapshotEntity(
            id = "m", capturedAt = 0L, recoveryScore = 70, hrvMs = 40.0, restingHeartRate = 50,
            sleepPerformance = 80, sleepTotalMinutes = 420, sleepRemMinutes = 80,
            sleepDeepMinutes = 60, sleepLightMinutes = 240, sleepAwakeMinutes = 40,
            sleepDisturbances = 5, dayStrain = 12.0, dayKilojoules = 8000.0, createdAt = 0L,
        )
        val high = median.copy(id = "h", hrvMs = 60.0)
        val low = median.copy(id = "l", hrvMs = 20.0)
        val resultHigh = useCase.calculate(emptyList(), high, listOf(median, high), null)
        val resultLow = useCase.calculate(emptyList(), low, listOf(median, low), null)
        assertThat(resultHigh.total).isGreaterThan(resultLow.total)
    }

    private fun entry(
        id: String = "id-${System.nanoTime()}",
        severity: Int,
        status: EntryStatus,
        resolvedAt: Long? = null,
        category: EntropyCategory = EntropyCategory.MENTAL,
    ) = EntropyEntryEntity(
        id = id, rawTranscript = "", title = "T", description = "D",
        category = category, severity = severity, priorityScore = 50.0,
        priorityReason = "", status = status, timeBucket = TimeBucket.HEUTE,
        estimatedDurationMinutes = 30, createdAt = 0L, updatedAt = 0L,
        resolvedAt = resolvedAt, tags = emptyList(), aiNotes = null,
        source = EntrySource.NUTZER_MIC, biomarkerSnapshotId = null,
    )

    private fun day(shift: ShiftCode) = CalendarDayEntity(
        date = "2026-05-07", shiftCode = shift, rawCalendarText = "",
        workWindowStart = null, workWindowEnd = null,
        sleepWindowStart = null, sleepWindowEnd = null,
        availableMinutesEstimate = 240, syncedAt = 0L,
    )
}

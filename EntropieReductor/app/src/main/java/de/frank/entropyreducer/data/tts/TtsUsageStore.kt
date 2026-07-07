package de.frank.entropyreducer.data.tts

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Zaehlt die an Google Cloud Text-to-Speech (Chirp 3 HD) gesendeten Zeichen des laufenden
 * Kalendermonats mit — Franks Selbst-Beobachtung des Gratis-Kontingents (1 Mio Zeichen/Monat),
 * weil Google in der Cloud Console KEINEN brauchbaren Zeichen-Zaehler anbietet (Vorfall 2026-07-03).
 *
 * Root-Cause-Ansatz (Observability-Direktive): Die App weiss bei jedem Vorlese-Aufruf exakt, wie
 * viele Zeichen sie schickt (`text.length` des synthetisierten Textes). Google rechnet TTS pro
 * Eingabe-Zeichen ab (inkl. Leerzeichen), also ist die String-Laenge die korrekte Zaehlgroesse.
 *
 * Verhalten:
 *  - Gezaehlt wird NUR bei einem echten, erfolgreichen Google-Call (in [TtsPlayer.synthesize]);
 *    Cache-Treffer gehen nicht an Google und werden bewusst nicht mitgezaehlt.
 *  - Rollover: Wechselt der Kalendermonat, wird der Zaehler beim naechsten Zugriff auf 0 gesetzt
 *    (analog zum monatlichen Reset des Google-Free-Tiers). Lokaler Monat — am Monatsuebergang
 *    minimal ungenau gegenueber Googles Pacific-Time-Grenze, fuer den Ueberblick unerheblich.
 *  - Reaktiv: [usage] ist ein Hot-StateFlow, den die Mental-UI live mitliest (hochzaehlen beim
 *    Vorlesen).
 *  - Drive-Sync (Frank-Wunsch 2026-07-03): [add] meldet per Rueckgabe, wenn eine neue
 *    [BACKUP_STEP]-Schwelle (50.000 Zeichen) ueberschritten wurde — dann sichert [TtsUsageBackup]
 *    den Stand ins Drive (nicht bei JEDEM Zeichen, sonst liefe das Backup beim Vorlesen dauernd).
 *    [applyRestored] zieht beim App-Start den lokalen Stand auf den (groesseren) Drive-Wert hoch,
 *    damit der Zaehler eine Neuinstallation ueberlebt (Genauigkeit +-50.000, bewusst akzeptiert).
 *
 * Nicht sensibel -> normale SharedPreferences (kein EncryptedSharedPreferences noetig).
 */
@Singleton
class TtsUsageStore
@Inject
constructor(
    @ApplicationContext context: Context,
) {

    private companion object {
        const val TAG = "TtsUsageStore"
        const val PREFS = "tts_usage"
        const val KEY_MONTH = "month" // Format "2026-07"
        const val KEY_CHARS = "chars_this_month"

        /** Hoechstes bereits ins Drive gesicherte 50k-Vielfache (`chars / BACKUP_STEP`). */
        const val KEY_BACKUP_THRESHOLD = "backup_threshold"

        /** Google-Gratis-Kontingent fuer Chirp 3 HD: 1 Mio Zeichen pro Monat. */
        const val MONTHLY_FREE_LIMIT = 1_000_000L

        /** Alle 50.000 Zeichen wird der Stand ins Drive gesichert (Frank-Wunsch 2026-07-03). */
        const val BACKUP_STEP = 50_000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _usage = MutableStateFlow(loadCurrent())

    /** Live-Stand des laufenden Monats fuer die UI. */
    val usage: StateFlow<TtsUsage> = _usage.asStateFlow()

    /**
     * Verbucht [chars] gesendete Zeichen auf den laufenden Monat. Thread-sicher. Bei Monatswechsel
     * wird vorher auf 0 zurueckgesetzt.
     *
     * @return `true`, wenn durch diese Buchung eine neue [BACKUP_STEP]-Schwelle (50k) erreicht wurde
     *         und der Stand deshalb ins Drive gesichert werden soll; sonst `false`.
     */
    @Synchronized
    fun add(chars: Int): Boolean {
        if (chars <= 0) return false
        val nowMonth = currentMonthKey()
        val monthChanged = prefs.getString(KEY_MONTH, null) != nowMonth
        val base = if (monthChanged) 0L else prefs.getLong(KEY_CHARS, 0L)
        val prevThreshold = if (monthChanged) 0L else prefs.getLong(KEY_BACKUP_THRESHOLD, 0L)
        val total = base + chars
        val newThreshold = total / BACKUP_STEP
        val backupDue = newThreshold > prevThreshold
        prefs.edit()
            .putString(KEY_MONTH, nowMonth)
            .putLong(KEY_CHARS, total)
            .putLong(KEY_BACKUP_THRESHOLD, newThreshold)
            .apply()
        _usage.value = TtsUsage(charsThisMonth = total)
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "TTS-Verbrauch +$chars -> $total/$MONTHLY_FREE_LIMIT (Monat $nowMonth, " +
                "Schwelle $prevThreshold->$newThreshold, backupDue=$backupDue)",
        )
        return backupDue
    }

    /**
     * App-Start-Restore (Frank-Wunsch 2026-07-03): Hebt den lokalen Stand auf den Drive-Wert an —
     * ABER nur, wenn der Backup-Monat der aktuelle ist UND der Drive-Wert groesser ist. So ueberlebt
     * der Zaehler eine Neuinstallation, ohne einen alten (hoeheren) Vormonatswert faelschlich
     * einzuspielen.
     */
    @Synchronized
    fun applyRestored(month: String, chars: Long) {
        val nowMonth = currentMonthKey()
        if (month != nowMonth) {
            Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Restore ignoriert: Backup-Monat $month != aktuell $nowMonth")
            return
        }
        val local = if (prefs.getString(KEY_MONTH, null) == nowMonth) prefs.getLong(KEY_CHARS, 0L) else 0L
        if (chars <= local) {
            Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Restore ignoriert: Drive $chars <= lokal $local")
            return
        }
        prefs.edit()
            .putString(KEY_MONTH, nowMonth)
            .putLong(KEY_CHARS, chars)
            .putLong(KEY_BACKUP_THRESHOLD, chars / BACKUP_STEP)
            .apply()
        _usage.value = TtsUsage(charsThisMonth = chars)
        Diag.d(DiagnosticArea.GOOGLE_TTS, TAG, "Restore angewendet: lokal $local -> $chars (Monat $nowMonth)")
    }

    /** Aktueller Stand fuer das Drive-Backup (Monat + Zeichen). */
    @Synchronized
    fun snapshot(): TtsUsageSnapshot {
        val nowMonth = currentMonthKey()
        val chars = if (prefs.getString(KEY_MONTH, null) == nowMonth) prefs.getLong(KEY_CHARS, 0L) else 0L
        return TtsUsageSnapshot(month = nowMonth, chars = chars)
    }

    /** Liest den aktuellen Stand — beruecksichtigt einen zwischenzeitlichen Monatswechsel. */
    private fun loadCurrent(): TtsUsage {
        val nowMonth = currentMonthKey()
        val chars = if (prefs.getString(KEY_MONTH, null) == nowMonth) prefs.getLong(KEY_CHARS, 0L) else 0L
        return TtsUsage(charsThisMonth = chars)
    }

    private fun currentMonthKey(): String {
        val ym = YearMonth.now()
        return "%04d-%02d".format(ym.year, ym.monthValue)
    }
}

/**
 * Momentaufnahme des Monats-Verbrauchs. [percentFree] ist der noch freie Anteil in Prozent
 * (0..100), auf ganze Prozent gerundet und bei Ueberschreitung auf 0 begrenzt.
 */
data class TtsUsage(
    val charsThisMonth: Long,
    val limit: Long = 1_000_000L,
) {
    val percentFree: Int
        get() {
            if (limit <= 0L) return 0
            val free = (limit - charsThisMonth).coerceIn(0L, limit)
            return ((free * 100L) / limit).toInt()
        }
}

/** Backup-Stand fuer den Drive-Sync (Monat + kumulierte Zeichen). */
data class TtsUsageSnapshot(
    val month: String,
    val chars: Long,
)

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
 *  - Rollover: Wechselt der Kalendermonat, wird der Zaehler beim naechsten [add] auf 0 gesetzt
 *    (analog zum monatlichen Reset des Google-Free-Tiers). Lokaler Monat — am Monatsuebergang
 *    minimal ungenau gegenueber Googles Pacific-Time-Grenze, fuer den Ueberblick unerheblich.
 *  - Reaktiv: [usage] ist ein Hot-StateFlow, den die Mental-UI live mitliest (hochzaehlen beim
 *    Vorlesen).
 *
 * Nicht sensibel -> normale SharedPreferences (kein EncryptedSharedPreferences noetig). Der Zaehler
 * ist geraetelokal: nach Neuinstallation startet er wieder bei 0 (dokumentierte Einschraenkung).
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

        /** Google-Gratis-Kontingent fuer Chirp 3 HD: 1 Mio Zeichen pro Monat. */
        const val MONTHLY_FREE_LIMIT = 1_000_000L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _usage = MutableStateFlow(loadCurrent())

    /** Live-Stand des laufenden Monats fuer die UI. */
    val usage: StateFlow<TtsUsage> = _usage.asStateFlow()

    /**
     * Verbucht [chars] gesendete Zeichen auf den laufenden Monat. Thread-sicher (kann aus mehreren
     * IO-Coroutinen kommen). Bei Monatswechsel wird vorher auf 0 zurueckgesetzt.
     */
    @Synchronized
    fun add(chars: Int) {
        if (chars <= 0) return
        val nowMonth = currentMonthKey()
        val storedMonth = prefs.getString(KEY_MONTH, null)
        val base = if (storedMonth == nowMonth) prefs.getLong(KEY_CHARS, 0L) else 0L
        val total = base + chars
        prefs.edit()
            .putString(KEY_MONTH, nowMonth)
            .putLong(KEY_CHARS, total)
            .apply()
        _usage.value = TtsUsage(charsThisMonth = total)
        Diag.d(
            DiagnosticArea.GOOGLE_TTS,
            TAG,
            "TTS-Verbrauch +$chars -> $total/$MONTHLY_FREE_LIMIT Zeichen (Monat $nowMonth)",
        )
    }

    /** Liest den aktuellen Stand — beruecksichtigt einen zwischenzeitlichen Monatswechsel. */
    private fun loadCurrent(): TtsUsage {
        val nowMonth = currentMonthKey()
        val storedMonth = prefs.getString(KEY_MONTH, null)
        val chars = if (storedMonth == nowMonth) prefs.getLong(KEY_CHARS, 0L) else 0L
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

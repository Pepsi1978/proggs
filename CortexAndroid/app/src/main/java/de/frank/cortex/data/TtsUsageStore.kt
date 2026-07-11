package de.frank.cortex.data

import android.content.Context
import android.content.SharedPreferences
import de.frank.cortex.observability.CortexLog
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TtsUsageStore {
    private const val PREFS = "tts_usage"
    private const val KEY_MONTH = "month"
    private const val KEY_CHARS = "chars_this_month"

    private lateinit var prefs: SharedPreferences
    private val _usage = MutableStateFlow(TtsUsage())
    val usage: StateFlow<TtsUsage> = _usage.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        refreshMonth()
    }

    @Synchronized
    fun refreshMonth() {
        val month = currentMonthKey()
        val storedMonth = prefs.getString(KEY_MONTH, null)
        val chars = if (storedMonth == month) prefs.getLong(KEY_CHARS, 0L) else 0L
        if (storedMonth != month) {
            prefs.edit().putString(KEY_MONTH, month).putLong(KEY_CHARS, 0L).apply()
        }
        _usage.value = TtsUsage(charsThisMonth = chars)
    }

    @Synchronized
    fun addSuccessfulSynthesis(chars: Int) {
        if (chars <= 0) return
        val month = currentMonthKey()
        val base = if (prefs.getString(KEY_MONTH, null) == month) {
            prefs.getLong(KEY_CHARS, 0L)
        } else {
            0L
        }
        val total = base + chars
        prefs.edit().putString(KEY_MONTH, month).putLong(KEY_CHARS, total).apply()
        _usage.value = TtsUsage(charsThisMonth = total)
        CortexLog.info(
            "TtsUsageStore",
            "add",
            "Chirp-TTS-Verbrauch aktualisiert",
            mapOf("chars" to chars, "total" to total, "month" to month)
        )
    }

    private fun currentMonthKey(): String = YearMonth.now().toString()
}

data class TtsUsage(
    val charsThisMonth: Long = 0L,
    val limit: Long = 1_000_000L
) {
    val percentFree: Int
        get() {
            if (limit <= 0L) return 0
            val free = (limit - charsThisMonth).coerceIn(0L, limit)
            return ((free * 100L) / limit).toInt()
        }
}

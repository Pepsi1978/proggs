package de.frank.stacklabor.werftstudio.service.tts

import android.content.Context
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TtsUsage(
    val month: String,
    val charactersByProvider: Map<TtsProviderId, Long>,
) {
    val totalCharacters: Long get() = charactersByProvider.values.sum()
    val googleCharacters: Long get() = charactersByProvider[TtsProviderId.GOOGLE_CLOUD] ?: 0L
    val googleFreeCharactersRemaining: Long get() = (1_000_000L - googleCharacters).coerceAtLeast(0L)
}

class TtsUsageStore(context: Context, private val monthProvider: () -> YearMonth = YearMonth::now) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val _usage = MutableStateFlow(load())
    val usage: StateFlow<TtsUsage> = _usage.asStateFlow()

    @Synchronized
    fun add(provider: TtsProviderId, characters: Int) {
        if (characters <= 0) return
        val month = currentMonth()
        val current = if (preferences.getString(KEY_MONTH, null) == month) {
            preferences.getLong(provider.key(), 0L)
        } else {
            0L
        }
        val editor = preferences.edit()
        if (preferences.getString(KEY_MONTH, null) != month) {
            TtsProviderId.entries.forEach { editor.remove(it.key()) }
        }
        editor.putString(KEY_MONTH, month).putLong(provider.key(), current + characters).apply()
        _usage.value = load()
    }

    @Synchronized
    fun snapshot(): TtsUsage = load().also { _usage.value = it }

    private fun load(): TtsUsage {
        val month = currentMonth()
        val matchingMonth = preferences.getString(KEY_MONTH, null) == month
        return TtsUsage(
            month,
            TtsProviderId.entries.associateWith { if (matchingMonth) preferences.getLong(it.key(), 0L) else 0L },
        )
    }

    private fun currentMonth(): String = monthProvider().toString()
    private fun TtsProviderId.key(): String = "characters_${name.lowercase()}"

    private companion object {
        const val PREFERENCES = "tts_usage"
        const val KEY_MONTH = "month"
    }
}

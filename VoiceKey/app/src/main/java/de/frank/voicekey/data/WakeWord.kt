package de.frank.voicekey.data

import java.util.UUID

enum class WakeLang(val label: String) {
    DE("Deutsche Wake-Wörter"),
    EN("Englische Wake-Wörter"),
}

/**
 * Ein Wake-Wort. Favoriten sind die AKTIVEN Wörter: nur sie landen in der
 * Erkennungs-Grammatik des Vosk-Recognizers (praezisere Erkennung, weniger Fehlausloeser).
 */
data class WakeWord(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val lang: WakeLang,
    val favorit: Boolean = false,
)

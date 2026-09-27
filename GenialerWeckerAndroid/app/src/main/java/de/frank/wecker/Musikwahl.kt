package de.frank.wecker

import java.io.File

/**
 * Was beim Schritt „MP3 / Weckton“ wirklich erklingt – dieselbe Regel wie [AlarmPlaylist]:
 * ohne eigene Datei der eingebaute Ton, bei fehlender Datei der klassische Ersatzweckton.
 */
object MusikAnzeige {
    data class Anzeige(val titel: String, val quelle: String, val ersatz: Boolean)

    fun von(alarm: Alarm, dateiVorhanden: Boolean = File(alarm.music).length() > 44): Anzeige = when {
        alarm.music.isBlank() -> Anzeige(Tones.names[alarm.tone] ?: Tones.names.getValue("classic"), "Eingebauter Weckton", false)
        !dateiVorhanden -> Anzeige(Tones.names.getValue("classic"),
            "Ersatz, weil „${alarm.musicName.ifBlank { "die gewählte Datei" }}“ nicht mehr auf dem Gerät ist", true)
        else -> Anzeige(alarm.musicName.ifBlank { "Eigene Audio-Datei" },
            if (alarm.musicQuelle == "geraet") "Geräte-Weckton" else "Audio-Datei vom Gerät", false)
    }
}

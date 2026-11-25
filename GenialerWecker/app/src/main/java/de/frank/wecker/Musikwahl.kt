package de.frank.wecker

import java.io.File

/** Herkunft des Klangs im Schritt „MP3 / Weckton“. */
enum class MusikArt { EINGEBAUT, MP3, GERAET }

/**
 * Löst für UI-Anzeigen exakt dieselbe Regel auf wie [AlarmPlaylist]:
 * ohne eigene Datei erklingt der eingebaute Weckton, bei fehlender oder beschädigter Datei
 * der klassische Ersatzweckton, sonst die gespeicherte MP3-/Audiodatei bzw. der Geräte-Weckton.
 */
object MusikAnzeige {
    data class Anzeige(
        val titel: String,
        val quelle: String,
        val art: MusikArt,
        val ersatz: Boolean,
        val fehlenderName: String = "",
    )

    fun dateiVerfuegbar(pfad: String): Boolean = pfad.isNotBlank() && File(pfad).length() > 44

    fun von(alarm: Alarm, dateiVorhanden: Boolean = dateiVerfuegbar(alarm.music)): Anzeige = when {
        alarm.music.isBlank() -> Anzeige(
            titel = Tones.names[alarm.tone] ?: Tones.names.getValue("classic"),
            quelle = "Eingebauter Weckton",
            art = MusikArt.EINGEBAUT,
            ersatz = false,
        )
        !dateiVorhanden -> {
            val vermisst = alarm.musicName.ifBlank { "die gewählte Datei" }
            Anzeige(
                titel = Tones.names.getValue("classic"),
                quelle = "Ersatz – „$vermisst“ ist nicht mehr auf dem Gerät verfügbar",
                art = if (alarm.musicQuelle == "geraet") MusikArt.GERAET else MusikArt.MP3,
                ersatz = true,
                fehlenderName = vermisst,
            )
        }
        alarm.musicQuelle == "geraet" -> Anzeige(
            titel = alarm.musicName.ifBlank { "Geräte-Weckton" },
            quelle = "Geräte-Weckton",
            art = MusikArt.GERAET,
            ersatz = false,
        )
        else -> Anzeige(
            titel = alarm.musicName.ifBlank { "Eigene Audio-Datei" },
            quelle = "MP3 / Audio-Datei",
            art = MusikArt.MP3,
            ersatz = false,
        )
    }

    /** Kurze einzeilige Klangangabe für Weckerkarten und Abschnittsköpfe. */
    fun kurz(alarm: Alarm, dateiVorhanden: Boolean = dateiVerfuegbar(alarm.music)): String {
        val info = von(alarm, dateiVorhanden)
        return when {
            info.ersatz -> "Datei fehlt (Ersatzton)"
            info.art == MusikArt.MP3 -> "MP3: ${info.titel}"
            else -> info.titel
        }
    }
}

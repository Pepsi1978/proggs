package de.frank.stacklabor.daten.entitaeten

import androidx.room.TypeConverter

/**
 * Wandelt die Aufzaehlungen in Text und zurueck.
 *
 * Gespeichert wird bewusst der Name (z. B. "WASSER") und nicht die Ordnungszahl: So bleibt ein
 * bestehender Datenbestand lesbar, wenn spaeter ein Wert dazukommt oder sich die Reihenfolge
 * aendert. Unbekannter Text ergibt den fachlichen Vorgabewert statt eines Absturzes.
 */
class Wandler {

    @TypeConverter
    fun loeslichkeitNachText(wert: Loeslichkeit?): String? = wert?.name

    @TypeConverter
    fun textNachLoeslichkeit(wert: String?): Loeslichkeit? =
        wert?.let { text ->
            Loeslichkeit.entries.firstOrNull { it.name == text } ?: Loeslichkeit.WASSER
        }

    @TypeConverter
    fun darreichungsformNachText(wert: Darreichungsform?): String? = wert?.name

    @TypeConverter
    fun textNachDarreichungsform(wert: String?): Darreichungsform? =
        wert?.let { text ->
            Darreichungsform.entries.firstOrNull { it.name == text } ?: Darreichungsform.SONSTIGE
        }

    @TypeConverter
    fun einheitNachText(wert: Einheit?): String? = wert?.name

    @TypeConverter
    fun textNachEinheit(wert: String?): Einheit? =
        wert?.let { text -> Einheit.entries.firstOrNull { it.name == text } ?: Einheit.MG }

    @TypeConverter
    fun bereichNachText(wert: Bereich?): String? = wert?.name

    @TypeConverter
    fun textNachBereich(wert: String?): Bereich? =
        wert?.let { text -> Bereich.entries.firstOrNull { it.name == text } ?: Bereich.STACK }

    @TypeConverter
    fun wirkungNachText(wert: Wirkung?): String? = wert?.name

    @TypeConverter
    fun textNachWirkung(wert: String?): Wirkung? =
        wert?.let { text -> Wirkung.entries.firstOrNull { it.name == text } ?: Wirkung.STOERT }

    @TypeConverter
    fun konkurrenzArtNachText(wert: KonkurrenzArt?): String? = wert?.name

    @TypeConverter
    fun textNachKonkurrenzArt(wert: String?): KonkurrenzArt? =
        wert?.let { text ->
            KonkurrenzArt.entries.firstOrNull { it.name == text } ?: KonkurrenzArt.WIRKUNG
        }
}

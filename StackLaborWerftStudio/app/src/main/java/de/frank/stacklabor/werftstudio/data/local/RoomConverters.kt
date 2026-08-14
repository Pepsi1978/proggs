package de.frank.stacklabor.werftstudio.data.local

import androidx.room.TypeConverter
import de.frank.stacklabor.werftstudio.domain.model.BewertungsBereich
import de.frank.stacklabor.werftstudio.domain.model.Darreichungsform
import de.frank.stacklabor.werftstudio.domain.model.DosisVariante
import de.frank.stacklabor.werftstudio.domain.model.Einheit
import de.frank.stacklabor.werftstudio.domain.model.FrequenzTyp
import de.frank.stacklabor.werftstudio.domain.model.KonkurrenzArt
import de.frank.stacklabor.werftstudio.domain.model.Loeslichkeit
import de.frank.stacklabor.werftstudio.domain.model.Sortieransicht
import de.frank.stacklabor.werftstudio.domain.model.Wirkung
import java.math.BigDecimal
import java.util.Base64

class RoomConverters {
    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun stringListToString(values: List<String>?): String? = values?.joinToString(",") {
        Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8))
    }

    @TypeConverter
    fun stringToStringList(value: String?): List<String>? = value?.takeIf(String::isNotEmpty)?.split(',')
        ?.map { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }
        ?: value?.let { emptyList() }

    @TypeConverter fun loeslichkeitToCode(value: Loeslichkeit): Int = value.code
    @TypeConverter fun codeToLoeslichkeit(code: Int): Loeslichkeit = enumFromCode(Loeslichkeit.entries, code) { it.code }
    @TypeConverter fun darreichungsformToCode(value: Darreichungsform): Int = value.code
    @TypeConverter fun codeToDarreichungsform(code: Int): Darreichungsform = enumFromCode(Darreichungsform.entries, code) { it.code }
    @TypeConverter fun einheitToCode(value: Einheit?): Int? = value?.code
    @TypeConverter fun codeToEinheit(code: Int?): Einheit? = code?.let { enumFromCode(Einheit.entries, it) { value -> value.code } }
    @TypeConverter fun dosisVarianteToCode(value: DosisVariante): Int = value.code
    @TypeConverter fun codeToDosisVariante(code: Int): DosisVariante = enumFromCode(DosisVariante.entries, code) { it.code }
    @TypeConverter fun frequenzTypToCode(value: FrequenzTyp): Int = value.code
    @TypeConverter fun codeToFrequenzTyp(code: Int): FrequenzTyp = enumFromCode(FrequenzTyp.entries, code) { it.code }
    @TypeConverter fun bereichToCode(value: BewertungsBereich): Int = value.code
    @TypeConverter fun codeToBereich(code: Int): BewertungsBereich = enumFromCode(BewertungsBereich.entries, code) { it.code }
    @TypeConverter fun wirkungToCode(value: Wirkung): Int = value.code
    @TypeConverter fun codeToWirkung(code: Int): Wirkung = enumFromCode(Wirkung.entries, code) { it.code }
    @TypeConverter fun konkurrenzArtToCode(value: KonkurrenzArt): Int = value.code
    @TypeConverter fun codeToKonkurrenzArt(code: Int): KonkurrenzArt = enumFromCode(KonkurrenzArt.entries, code) { it.code }
    @TypeConverter fun sortieransichtToCode(value: Sortieransicht): Int = value.code
    @TypeConverter fun codeToSortieransicht(code: Int): Sortieransicht = enumFromCode(Sortieransicht.entries, code) { it.code }

    private inline fun <reified T> enumFromCode(
        values: Iterable<T>,
        code: Int,
        selector: (T) -> Int,
    ): T = values.firstOrNull { selector(it) == code }
        ?: throw IllegalArgumentException("Unbekannter ${T::class.simpleName}-Code: $code")
}

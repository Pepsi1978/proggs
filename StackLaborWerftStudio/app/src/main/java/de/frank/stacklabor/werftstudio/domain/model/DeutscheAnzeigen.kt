package de.frank.stacklabor.werftstudio.domain.model

import java.math.BigDecimal
import java.util.Locale

fun Darreichungsform.anzeige(): String = when (this) {
    Darreichungsform.KAPSEL -> "Kapsel"
    Darreichungsform.TABLETTE -> "Tablette"
    Darreichungsform.LOEFFEL -> "Löffel"
    Darreichungsform.TASSE -> "Tasse"
    Darreichungsform.PULVER -> "Pulver"
    Darreichungsform.TROPFEN -> "Tropfen"
    Darreichungsform.SONSTIGE -> "Sonstige"
}

fun Mittel.darreichungsformAnzeige(): String {
    val eigenerText = darreichungsformText?.trim().orEmpty()
    return eigenerText.toDarreichungsformOrNull()?.anzeige()
        ?: eigenerText.mitDeutschenUmlauten().ifBlank { darreichungsform.anzeige() }
}

fun Mittel.darreichungsformAnzeige(stueckzahl: BigDecimal): String {
    val form = darreichungsformAnzeige()
    if (stueckzahl.compareTo(BigDecimal.ONE) == 0) return form
    return when (form.lowercase(Locale.GERMANY)) {
        "kapsel" -> "Kapseln"
        "tablette" -> "Tabletten"
        else -> form
    }
}

fun Einheit.anzeige(): String = when (this) {
    Einheit.UG -> "µg"
    Einheit.STUECK -> "Stück"
    else -> name.lowercase()
}

fun StackEintragDosis.einheitAnzeige(): String =
    einheitText?.trim().orEmpty().mitDeutschenUmlauten().ifBlank { einheit?.anzeige().orEmpty() }

fun StackEintrag.frequenzAnzeige(): String =
    frequenzText?.trim().orEmpty().mitDeutschenUmlauten().ifBlank {
        if (frequenzTyp == FrequenzTyp.TAEGLICH) "Täglich" else "Alle ${alleNTage ?: 2} Tage"
    }

fun Loeslichkeit.anzeige(): String = when (this) {
    Loeslichkeit.WASSER -> "wasserlöslich"
    Loeslichkeit.FETT -> "fettlöslich"
    Loeslichkeit.BEIDES -> "wasser- und fettlöslich"
}

fun String.toDarreichungsformOrNull(): Darreichungsform? {
    val wert = trim().replace("ö", "oe", ignoreCase = true).uppercase(Locale.GERMANY)
    return Darreichungsform.entries.firstOrNull { it.name == wert || it.anzeige().equals(trim(), true) }
}

fun String.toEinheitOrNull(): Einheit? {
    val wert = trim().replace("µ", "U").replace("ü", "ue", ignoreCase = true)
    return Einheit.entries.firstOrNull { it.name.equals(wert, true) || it.anzeige().equals(trim(), true) }
}

fun String.mitDeutschenUmlauten(): String = TECHNISCHE_SCHREIBWEISEN.entries.fold(this) { text, (alt, neu) ->
    text.replace(Regex("\\b${Regex.escape(alt)}\\b"), neu)
}

private val TECHNISCHE_SCHREIBWEISEN: Map<String, String> = linkedMapOf(
    "LOEFFEL" to "Löffel",
    "Loeffel" to "Löffel",
    "loeffel" to "löffel",
    "STUECK" to "Stück",
    "Stueck" to "Stück",
    "stueck" to "stück",
    "TAEGLICH" to "Täglich",
    "Taeglich" to "Täglich",
    "taeglich" to "täglich",
    "STUETZT" to "stützt",
    "Stuetzt" to "Stützt",
    "stuetzt" to "stützt",
    "STOERT" to "stört",
    "Stoert" to "Stört",
    "stoert" to "stört",
    "LOESLICHKEIT" to "Löslichkeit",
    "Loeslichkeit" to "Löslichkeit",
    "loeslichkeit" to "löslichkeit",
)

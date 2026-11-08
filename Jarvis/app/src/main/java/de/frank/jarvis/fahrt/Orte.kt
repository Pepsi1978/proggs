package de.frank.jarvis.fahrt

import android.content.Context
import android.content.Intent
import android.net.Uri
import de.frank.jarvis.data.Einstellungen
import java.util.Locale

/** Franks benannte Orte: Zuhause, Arbeit und Papa aus den Einstellungen; alles andere ist eine Adresse oder ein Ort. */
object Orte {
    /** Personen und Orte ohne gespeicherte Adresse: Maps würde hier raten, also nachfragen. */
    private val UNKLAR = setOf(
        "mama", "mami", "mutter", "mutti", "oma", "opa", "omi", "bruder", "schwester", "tante", "onkel", "sohn", "tochter", "cousin", "cousine",
        "freund", "freundin", "kumpel", "chef", "chefin", "kollege", "kollegin", "schatz", "frau", "mann", "arzt", "ärztin", "zahnarzt", "zahnärztin", "hausarzt", "doktor",
    )

    private fun rein(text: String): String = text.trim().trim('.', '!', '?', ',', ';', ':', '„', '“', '"').trim().lowercase(Locale.GERMAN)
        .replace(Regex("^(zum|zur|zu|nach|ins|in die|in den)\\s+"), "").trim()

    fun benannt(context: Context, text: String): String? {
        val e = Einstellungen.get(context)
        return when (rein(text)) {
            "arbeit", "auf arbeit", "dienst", "museumsinsel" -> e.adresseArbeit
            "zuhause", "hause", "zu hause", "heim", "wohnung", "daheim" -> e.adresseZuhause
            "papa", "papas", "papi", "vater", "meinem papa", "meinem vater", "papa nach hause" -> e.adressePapa
            else -> null
        }
    }

    /** Eine Person oder ein Ort, für den keine Adresse gespeichert ist („Mama“, „meiner Schwester“, „zum Arzt“)? */
    fun unklar(text: String): Boolean = rein(text).replace(Regex("^(meine[mnrs]?|meiner|unsere[mnrs]?|dem|der)\\s+"), "").let { it in UNKLAR }
}

/** Die Autonavigation von Google Maps: startet sofort vom aktuellen Standort, ohne Routenvorschau. */
object Navigation {
    const val MAPS = "com.google.android.apps.maps"

    fun intent(ziel: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(ziel)}&mode=d")).setPackage(MAPS)
}

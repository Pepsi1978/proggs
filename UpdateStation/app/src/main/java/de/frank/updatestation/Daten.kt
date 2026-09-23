package de.frank.updatestation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/** Ein Eintrag aus app/src/main/assets/versionslog.json bzw. aus update.json. */
data class VersionsEintrag(val versionCode: Long, val versionName: String, val stand: String, val notiz: String) {
    fun alsJson(): JSONObject = JSONObject()
        .put("versionCode", versionCode).put("versionName", versionName).put("stand", stand).put("notiz", notiz)

    companion object {
        fun liste(arr: JSONArray?): List<VersionsEintrag> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                VersionsEintrag(o.optLong("versionCode"), o.optString("versionName"), o.optString("stand"), o.optString("notiz"))
            }
        }
    }
}

/** Liest den Versionslog, den jede App als Asset mitbringt, direkt aus der installierten App. */
object Versionslog {
    fun installiert(context: Context, paket: String): List<VersionsEintrag> = runCatching {
        context.createPackageContext(paket, 0).assets.open("versionslog.json").use {
            VersionsEintrag.liste(JSONObject(it.bufferedReader().readText()).optJSONArray("eintraege"))
        }
    }.getOrDefault(emptyList())
}

/** Inhalt einer update.json, geschrieben vom Skill "apk-update" (Format 1). */
data class UpdateManifest(
    val projekt: String,
    val paket: String,
    val versionCode: Long,
    val versionName: String,
    val versionStand: String?,
    val apk: String,
    val groesse: Long,
    val sha256: String,
    val signaturSha256: String,
    val erstelltAm: String,
    val versionslog: List<VersionsEintrag> = emptyList(),
) {
    companion object {
        fun ausJson(text: String): UpdateManifest {
            val o = JSONObject(text)
            val paket = o.getString("paket")
            // Der Paketname wird Teil von Dateinamen und Schlüsseln: nur gültige Android-Paketnamen.
            require(PAKET_MUSTER.matches(paket)) { "Ungültiger Paketname in update.json" }
            return UpdateManifest(
                projekt = o.getString("projekt"),
                paket = paket,
                versionCode = o.getLong("versionCode"),
                versionName = o.optString("versionName"),
                versionStand = o.optString("versionStand").takeIf { it.isNotBlank() && it != "null" },
                apk = o.getString("apk"),
                groesse = o.optLong("groesse"),
                sha256 = o.getString("sha256").lowercase(),
                signaturSha256 = o.optString("signaturSha256").lowercase(),
                erstelltAm = o.optString("erstelltAm"),
                versionslog = VersionsEintrag.liste(o.optJSONArray("versionslog")),
            )
        }
    }

    fun alsJson(): JSONObject = JSONObject()
        .put("projekt", projekt).put("paket", paket).put("versionCode", versionCode)
        .put("versionName", versionName).put("versionStand", versionStand ?: "")
        .put("apk", apk).put("groesse", groesse).put("sha256", sha256)
        .put("signaturSha256", signaturSha256).put("erstelltAm", erstelltAm)
        .put("versionslog", JSONArray().apply { versionslog.forEach { put(it.alsJson()) } })
}

/** Gültiger Android-Paketname: Segmente aus Buchstaben, Ziffern und _, mindestens zwei, durch Punkte getrennt. */
val PAKET_MUSTER = Regex("""^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$""")

/** SHA-256-Fingerabdruck in Kleinbuchstaben ohne Trennzeichen. */
val SHA256_MUSTER = Regex("""^[0-9a-f]{64}$""")

/** Ein gefundenes Update in der Quelle. [apkRef] ist Drive-Datei-ID oder Dokument-URI, null = APK (noch) nicht da. */
data class Fund(val manifest: UpdateManifest, val apkRef: String?)

/**
 * Ergebnis einer Suche. [zwischenstaende] = Projektordner → Kennung der Lage (Grund und Nummern),
 * deren Inhalt gerade nicht stimmig ist (neuere APK als update.json, update.json unlesbar, APK
 * ohne update.json, Lesefehler) – meist läuft dort noch die Drive-Synchronisierung.
 */
data class Suchergebnis(val funde: List<Fund>, val zwischenstaende: Map<String, String>)

/** Höchste Build-Nummer aus Dateinamen der Form "<Projekt>-<Version>-vc<Nummer>.apk". */
fun hoechsteApkNummer(namen: List<String>): Long =
    namen.mapNotNull { Regex("""-vc(\d+)\.apk$""").find(it)?.groupValues?.get(1)?.toLongOrNull() }.maxOrNull() ?: 0

enum class Status { UPDATE, AKTUELL, INSTALLIERT_NEUER, NICHT_INSTALLIERT, SIGNATUR_ANDERS, APK_FEHLT }

data class AppEintrag(
    val fund: Fund,
    val label: String,
    val installiertCode: Long?,
    val installiertName: String?,
    val status: Status,
) {
    val paket get() = fund.manifest.paket
}

sealed interface InstallStatus {
    data class Laedt(val prozent: Int) : InstallStatus
    data object Prueft : InstallStatus
    data object WartetAufBestaetigung : InstallStatus
    data object Fertig : InstallStatus
    data class Fehler(val text: String) : InstallStatus
}

data class Zustand(
    val eintraege: List<AppEintrag> = emptyList(),
    val prueftGerade: Boolean = false,
    val letztePruefung: Long = 0,
    val fehler: String? = null,
    val anmeldungNoetig: Boolean = false,
    val installationen: Map<String, InstallStatus> = emptyMap(),
)

/** Gemeinsamer Zustand für Oberfläche, Worker und Install-Empfänger (gleicher Prozess). */
object ZustandsSpeicher {
    val zustand = MutableStateFlow(Zustand())

    fun setzeInstallation(paket: String, status: InstallStatus?) = zustand.update {
        it.copy(installationen = if (status == null) it.installationen - paket else it.installationen + (paket to status))
    }
}

class Einstellungen(context: Context) {
    private val prefs = context.getSharedPreferences("einstellungen", Context.MODE_PRIVATE)

    var quelle: String?
        get() = prefs.getString("quelle", null)
        set(v) = prefs.edit().putString("quelle", v).apply()

    var ordnerUri: String?
        get() = prefs.getString("ordnerUri", null)
        set(v) = prefs.edit().putString("ordnerUri", v).apply()

    var drivePfad: String
        get() = prefs.getString("drivePfad", null) ?: STANDARD_PFAD
        set(v) = prefs.edit().putString("drivePfad", v).putString("driveOrdnerId", null).apply()

    var driveOrdnerId: String?
        get() = prefs.getString("driveOrdnerId", null)
        set(v) = prefs.edit().putString("driveOrdnerId", v).apply()

    var letztePruefung: Long
        get() = prefs.getLong("letztePruefung", 0)
        set(v) = prefs.edit().putLong("letztePruefung", v).apply()

    /** Takt der automatischen Prüfung in Minuten, immer eine der [INTERVALL_STUFEN]. */
    var intervallMinuten: Int
        get() = normalisiereIntervall(prefs.getInt("intervallMinuten", STANDARD_INTERVALL))
        set(v) = prefs.edit().putInt("intervallMinuten", normalisiereIntervall(v)).apply()

    /**
     * Ergebnis der Zählung: [offen] = Fenster von max Versuchen noch nicht ausgeschöpft,
     * [erschoepft] = ausgeschöpft und noch nicht gewarnt, [neu] = Episode beginnt gerade.
     */
    data class Nachpruefung(val offen: Set<String>, val erschoepft: Set<String>, val neu: Set<String>)

    /**
     * Zählt gezielte Nachprüfungen pro Projekt im Sync-Zwischenstand. Ändert sich die Lage eines
     * Projekts (andere Kennung), beginnt seine Episode neu; stimmige Projekte fallen heraus.
     * Als hängend gilt eine Episode erst, wenn sowohl [max] Versuche als auch [HAENGT_NACH_MS]
     * echte Zeit vergangen sind – schnelle manuelle Prüfungen allein reichen nie. Bis dahin bleibt
     * das Projekt in [Nachpruefung.offen] und wird gezielt weiter nachgeprüft.
     */
    fun zaehleNachpruefungen(zwischenstaende: Map<String, String>, max: Int): Nachpruefung {
        val alt = nachpruefungsStand()
        val neu = JSONObject()
        val offen = mutableSetOf<String>()
        val erschoepft = mutableSetOf<String>()
        val beginnt = mutableSetOf<String>()
        val jetzt = System.currentTimeMillis()
        zwischenstaende.forEach { (projekt, kennung) ->
            val vorher = alt.optJSONObject(projekt)?.takeIf { it.optString("kennung") == kennung }
            val n = (vorher?.optInt("n") ?: 0) + 1
            val seit = vorher?.optLong("seit")?.takeIf { it > 0 } ?: jetzt
            val gewarnt = vorher?.optBoolean("gewarnt") ?: false
            val eintrag = JSONObject().put("kennung", kennung).put("n", n).put("seit", seit).put("gewarnt", gewarnt)
            neu.put(projekt, eintrag)
            if (n == 1) beginnt += projekt
            if (!haengt(eintrag, max, jetzt)) offen += projekt else if (!gewarnt) erschoepft += projekt
        }
        prefs.edit().putString("nachpruefungen", neu.toString()).apply()
        return Nachpruefung(offen, erschoepft, beginnt)
    }

    private fun haengt(eintrag: JSONObject, max: Int, jetzt: Long): Boolean =
        eintrag.optInt("n") > max && jetzt - eintrag.optLong("seit", jetzt) >= HAENGT_NACH_MS

    /** Nur setzen, wenn die Warnung tatsächlich als Benachrichtigung gezeigt wurde. */
    fun markiereGewarnt(projekt: String) {
        val stand = nachpruefungsStand()
        stand.optJSONObject(projekt)?.put("gewarnt", true) ?: return
        prefs.edit().putString("nachpruefungen", stand.toString()).apply()
    }

    /** Hängende Projekte (für den Hinweis in der App) – dieselbe Bedingung wie in [zaehleNachpruefungen]. */
    fun haengendeProjekte(max: Int): List<String> {
        val stand = nachpruefungsStand()
        val jetzt = System.currentTimeMillis()
        return stand.keys().asSequence().filter { k -> stand.optJSONObject(k)?.let { haengt(it, max, jetzt) } == true }.sorted().toList()
    }

    private fun nachpruefungsStand(): JSONObject =
        runCatching { JSONObject(prefs.getString("nachpruefungen", "{}") ?: "{}") }.getOrDefault(JSONObject())

    /** Intervall, mit dem die periodische Prüfung zuletzt geplant wurde (0 = noch nie). */
    var geplantesIntervall: Int
        get() = prefs.getInt("geplantesIntervall", 0)
        set(v) = prefs.edit().putInt("geplantesIntervall", v).apply()

    /** Hintergrundprüfung ist wiederholt gescheitert (Hinweis in der App) bzw. wurde schon gemeldet. */
    var pruefFehler: Boolean
        get() = prefs.getBoolean("pruefFehler", false)
        set(v) = prefs.edit().putBoolean("pruefFehler", v).apply()
    var pruefFehlerGemeldet: Boolean
        get() = prefs.getBoolean("pruefFehlerGemeldet", false)
        set(v) = prefs.edit().putBoolean("pruefFehlerGemeldet", v).apply()

    /** Quelle liefert dauerhaft keine Projekte mehr (Hinweis in der App) bzw. wurde schon gemeldet. */
    var quelleLeer: Boolean
        get() = prefs.getBoolean("quelleLeer", false)
        set(v) = prefs.edit().putBoolean("quelleLeer", v).apply()
    var quelleLeerGemeldet: Boolean
        get() = prefs.getBoolean("quelleLeerGemeldet", false)
        set(v) = prefs.edit().putBoolean("quelleLeerGemeldet", v).apply()

    /**
     * Die an Android übergebene, noch nicht abgeschlossene Installations-Session je Paket (ID und
     * Zeitpunkt). Überlebt einen Prozessneustart, damit kein zweiter Bestätigungsdialog entsteht.
     */
    fun offeneSession(paket: String): Pair<Int, Long>? {
        val teile = prefs.getString("session_$paket", null)?.split(':') ?: return null
        val id = teile.getOrNull(0)?.toIntOrNull() ?: return null
        return id to (teile.getOrNull(1)?.toLongOrNull() ?: 0L)
    }
    /** Synchron gespeichert (commit), weil der Receiver Ergebnisse nur für diese ID annimmt; false = nicht gespeichert. */
    fun setzeOffeneSession(paket: String, id: Int): Boolean =
        prefs.edit().putString("session_$paket", "$id:${System.currentTimeMillis()}").commit()
    fun loescheOffeneSession(paket: String) = prefs.edit().remove("session_$paket").apply()

    fun gemeldet(paket: String): Long = prefs.getLong("gemeldet_$paket", 0)
    fun setzeGemeldet(paket: String, code: Long) = prefs.edit().putLong("gemeldet_$paket", code).apply()

    var funde: List<Fund>
        get() = runCatching {
            val arr = JSONArray(prefs.getString("funde", "[]"))
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Fund(UpdateManifest.ausJson(o.getJSONObject("m").toString()), o.optString("ref").takeIf { r -> r.isNotBlank() })
            }
        }.getOrDefault(emptyList())
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(JSONObject().put("m", it.manifest.alsJson()).put("ref", it.apkRef ?: "")) }
            prefs.edit().putString("funde", arr.toString()).apply()
        }

    companion object {
        const val STANDARD_PFAD = "Dokumente/Updates"
        const val STANDARD_INTERVALL = 30

        /** Schlüssel für "Quelle liefert gerade gar nichts" im Nachprüfungszähler. */
        const val LEER = "*"

        /** Frühestens nach dieser echten Zeit gilt ein Zwischenstand als hängend. */
        const val HAENGT_NACH_MS = 30 * 60_000L

        /** WorkManager erlaubt periodische Arbeit frühestens alle 15 Minuten. */
        val INTERVALL_STUFEN = listOf(15, 20, 30, 45, 60, 90, 120)

        fun normalisiereIntervall(minuten: Int): Int =
            INTERVALL_STUFEN.minByOrNull { kotlin.math.abs(it - minuten) } ?: STANDARD_INTERVALL

        fun intervallText(minuten: Int): String = when {
            minuten == 60 -> "jede Stunde"
            minuten == 120 -> "alle 2 Stunden"
            minuten > 60 && minuten % 60 != 0 -> "alle ${minuten / 60} h ${minuten % 60} Min."
            else -> "alle $minuten Min."
        }
    }
}

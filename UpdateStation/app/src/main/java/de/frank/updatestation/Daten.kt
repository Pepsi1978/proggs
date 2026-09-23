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
            return UpdateManifest(
                projekt = o.getString("projekt"),
                paket = o.getString("paket"),
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

/** Ein gefundenes Update in der Quelle. [apkRef] ist Drive-Datei-ID oder Dokument-URI, null = APK (noch) nicht da. */
data class Fund(val manifest: UpdateManifest, val apkRef: String?)

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

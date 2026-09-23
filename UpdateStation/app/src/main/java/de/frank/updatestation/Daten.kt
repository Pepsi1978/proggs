package de.frank.updatestation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

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
            )
        }
    }

    fun alsJson(): JSONObject = JSONObject()
        .put("projekt", projekt).put("paket", paket).put("versionCode", versionCode)
        .put("versionName", versionName).put("versionStand", versionStand ?: "")
        .put("apk", apk).put("groesse", groesse).put("sha256", sha256)
        .put("signaturSha256", signaturSha256).put("erstelltAm", erstelltAm)
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
    }
}

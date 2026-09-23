package de.frank.updatestation

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import java.security.MessageDigest

object Pruefer {

    /** Holt alle update.json aus der Quelle und bewertet sie gegen die installierten Apps. */
    suspend fun pruefe(context: Context): List<AppEintrag> {
        val quelle = Quellen.aktuelle(context) ?: return emptyList()
        val einst = Einstellungen(context)
        ZustandsSpeicher.zustand.update { it.copy(prueftGerade = true, fehler = null) }
        try {
            val ergebnis = quelle.suche()
            // Ordner im Sync-Zwischenstand ohne lesbares Manifest: gespeicherten Fund behalten statt ihn still zu verlieren.
            val gelesen = ergebnis.funde.map { it.manifest.projekt }.toSet()
            val behalten = einst.funde.filter { it.manifest.projekt in ergebnis.zwischenstaende && it.manifest.projekt !in gelesen }
            val funde = ergebnis.funde + behalten
            einst.funde = funde
            einst.letztePruefung = System.currentTimeMillis()
            val liste = bewerte(context, funde)
            Log.i(TAG, "Prüfung: ${funde.size} Updates gelesen, " + liste.groupingBy { it.status }.eachCount())
            // Pro Projekt begrenzt: ein dauerhaft hängender Ordner blockiert keine anderen.
            val offen = einst.zaehleNachpruefungen(ergebnis.zwischenstaende, PruefWorker.MAX_NACHPRUEFUNGEN)
            if (ergebnis.zwischenstaende.isNotEmpty()) {
                Log.i(TAG, "Zwischenstand in ${ergebnis.zwischenstaende.keys.sorted()}, ${behalten.size} gespeicherte Funde behalten, Nachprüfung für ${offen.sorted()}")
            }
            if (offen.isNotEmpty()) PruefWorker.planeNachpruefung(context, offen)
            ZustandsSpeicher.zustand.update {
                it.copy(eintraege = liste, letztePruefung = einst.letztePruefung, anmeldungNoetig = false)
            }
            return liste
        } catch (e: AnmeldungNoetig) {
            ZustandsSpeicher.zustand.update { it.copy(anmeldungNoetig = true) }
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Prüfung fehlgeschlagen", e)
            ZustandsSpeicher.zustand.update { it.copy(fehler = e.message ?: e.javaClass.simpleName) }
            throw e
        } finally {
            ZustandsSpeicher.zustand.update { it.copy(prueftGerade = false) }
        }
    }

    /** Ohne Netz: letzte Funde gegen den aktuellen Installationsstand neu bewerten. */
    fun bewerteGespeichert(context: Context) {
        val einst = Einstellungen(context)
        val liste = bewerte(context, einst.funde)
        ZustandsSpeicher.zustand.update { it.copy(eintraege = liste, letztePruefung = einst.letztePruefung) }
    }

    fun bewerte(context: Context, funde: List<Fund>): List<AppEintrag> {
        val pm = context.packageManager
        return funde.map { fund ->
            val m = fund.manifest
            val info = installiert(pm, m.paket)
            val label = info?.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: m.projekt
            val code = info?.longVersionCode
            val status = when {
                info == null -> Status.NICHT_INSTALLIERT
                m.versionCode < code!! -> Status.INSTALLIERT_NEUER
                m.versionCode == code -> Status.AKTUELL
                m.signaturSha256.isNotBlank() && m.signaturSha256 !in signaturen(info) -> Status.SIGNATUR_ANDERS
                fund.apkRef == null -> Status.APK_FEHLT
                else -> Status.UPDATE
            }
            AppEintrag(fund, label, code, info?.versionName, status)
        }.sortedWith(compareBy({ it.status.ordinal }, { it.label.lowercase() }))
    }

    fun installiert(pm: PackageManager, paket: String): PackageInfo? = try {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(paket, PackageManager.GET_SIGNING_CERTIFICATES)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    /** SHA-256 aller Signatur-Zertifikate (inkl. Rotations-Historie), klein geschrieben, ohne Doppelpunkte. */
    fun signaturen(info: PackageInfo): Set<String> {
        val s = info.signingInfo ?: return emptySet()
        val zert = if (s.hasMultipleSigners()) s.apkContentsSigners else s.signingCertificateHistory
        return zert.orEmpty().map { sha256(it.toByteArray()) }.toSet()
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

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
    /** [ausNachpruefung]: Aufruf aus der gezielten Nachprüfung (steuert, wie die nächste eingeplant wird). */
    suspend fun pruefe(context: Context, ausNachpruefung: Boolean = false): List<AppEintrag> {
        val quelle = Quellen.aktuelle(context) ?: return emptyList()
        val einst = Einstellungen(context)
        ZustandsSpeicher.zustand.update { it.copy(prueftGerade = true, fehler = null) }
        try {
            val ergebnis = quelle.suche()
            val gespeichert = einst.funde
            // Handy-seitiger Beleg, wann ein neuer Stand angekommen ist: nur bei echter Versionsänderung je Projekt.
            val alteVersion = gespeichert.associate { it.manifest.projekt to it.manifest.versionCode }
            ergebnis.funde.filter { alteVersion[it.manifest.projekt] != it.manifest.versionCode }.forEach { f ->
                Diagnose.ereignis(context, Phase.SYNC, "STAND_NEU", "projekt" to f.manifest.projekt,
                    "vc" to f.manifest.versionCode, "apk" to if (f.apkRef != null) "ja" else "nein")
            }
            // Quelle liefert gar nichts, obwohl Funde bekannt sind (z. B. Drive-Anbieter kurz leer):
            // wie einen Zwischenstand behandeln, nicht sofort alles verwerfen.
            val leer = ergebnis.funde.isEmpty() && ergebnis.zwischenstaende.isEmpty() && gespeichert.isNotEmpty()
            val zwischen = if (leer) mapOf(Einstellungen.LEER to "LEER") else ergebnis.zwischenstaende
            // Ordner im Sync-Zwischenstand ohne lesbares Manifest: gespeicherten Fund behalten statt ihn still zu verlieren.
            val gelesen = ergebnis.funde.map { it.manifest.projekt }.toSet()
            val behalten = if (leer) gespeichert
                else gespeichert.filter { it.manifest.projekt in zwischen && it.manifest.projekt !in gelesen }
            // Pro Projekt begrenzt: ein dauerhaft hängender Ordner blockiert keine anderen.
            val np = einst.zaehleNachpruefungen(zwischen, PruefWorker.MAX_NACHPRUEFUNGEN)
            var funde = ergebnis.funde + behalten
            if (leer && Einstellungen.LEER !in np.offen) {
                // Nach dem Nachprüfungsfenster alte Funde nicht länger als installierbar anbieten.
                funde = emptyList()
                einst.quelleLeer = true
                Diagnose.ereignis(context, Phase.SYNC, "QUELLE_LEER", "verworfen" to gespeichert.size)
            }
            if (ergebnis.funde.isNotEmpty()) {
                einst.quelleLeer = false
                einst.quelleLeerGemeldet = false
            } else if (einst.quelleLeer && !einst.quelleLeerGemeldet &&
                Benachrichtigungen.warnung(context, 43, "Keine Update-Projekte mehr gefunden",
                    "Der Update-Ordner ist seit längerer Zeit leer. UpdateStation sucht bei jeder Prüfung weiter.")
            ) {
                einst.quelleLeerGemeldet = true
            }
            einst.funde = funde
            einst.letztePruefung = System.currentTimeMillis()
            einst.pruefFehler = false
            einst.pruefFehlerGemeldet = false
            val liste = bewerte(context, funde)
            Log.i(TAG, "Prüfung: ${funde.size} Updates gelesen, " + liste.groupingBy { it.status }.eachCount())
            Diagnose.ereignis(context, Phase.SCAN, "ERGEBNIS", "projekte" to funde.size,
                "updates" to liste.count { it.status == Status.UPDATE }, "neu" to liste.count { it.status == Status.NICHT_INSTALLIERT },
                "zwischenstand" to zwischen.size, "behalten" to behalten.size)
            np.neu.forEach { p ->
                Diagnose.ereignis(context, Phase.SYNC, "ZWISCHENSTAND", "projekt" to p, "grund" to zwischen[p]?.substringBefore('|'))
            }
            np.erschoepft.filter { it != Einstellungen.LEER }.forEach { p ->
                Diagnose.ereignis(context, Phase.SYNC, "HAENGT", "projekt" to p, "grund" to zwischen[p]?.substringBefore('|'))
                val gezeigt = Benachrichtigungen.warnung(context, p.hashCode() + 11, "Update-Ordner „$p“ nicht stimmig",
                    "Seit etwa 30 Minuten passen APK und update.json nicht zusammen. Veröffentlichung am PC prüfen.")
                if (gezeigt) einst.markiereGewarnt(p)
            }
            if (zwischen.isNotEmpty()) {
                Log.i(TAG, "Zwischenstand in ${zwischen.keys.sorted()}, ${behalten.size} gespeicherte Funde behalten, Nachprüfung für ${np.offen.sorted()}")
            }
            if (np.offen.isNotEmpty()) PruefWorker.planeNachpruefung(context, np.offen, ausNachpruefung)
            ZustandsSpeicher.zustand.update {
                it.copy(eintraege = liste, letztePruefung = einst.letztePruefung, anmeldungNoetig = false)
            }
            return liste
        } catch (e: AnmeldungNoetig) {
            Diagnose.ereignis(context, Phase.SCAN, "ANMELDUNG_NOETIG")
            ZustandsSpeicher.zustand.update { it.copy(anmeldungNoetig = true) }
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Prüfung fehlgeschlagen", e)
            Diagnose.ereignis(context, Phase.SCAN, "FEHLER", "klasse" to Diagnose.klasse(e))
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

package de.frank.updatestation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object Installierer {

    /**
     * Lädt die APK, prüft sie gegen update.json und die installierte App und übergibt sie erst dann
     * an den Paket-Installer. Installiert wird nur, wenn der versionCode wirklich höher ist.
     */
    suspend fun installiere(context: Context, eintrag: AppEintrag) {
        val m = eintrag.fund.manifest
        val paket = m.paket
        try {
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Laedt(0))
            val datei = lade(context, eintrag)

            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Prueft)
            pruefe(context, datei, m)

            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufBestaetigung)
            uebergebe(context, datei, m, eintrag.label)
        } catch (e: Exception) {
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(e.message ?: "Unbekannter Fehler"))
        }
    }

    private suspend fun lade(context: Context, eintrag: AppEintrag): File = withContext(Dispatchers.IO) {
        val m = eintrag.fund.manifest
        val quelle = Quellen.aktuelle(context) ?: error("Keine Update-Quelle eingerichtet.")
        val ordner = File(context.cacheDir, "updates").apply { mkdirs() }
        ordner.listFiles()?.forEach { it.delete() }
        val datei = File(ordner, "${m.paket}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        var gelesen = 0L
        var letzteProzent = -1
        quelle.oeffne(eintrag.fund).use { ein ->
            datei.outputStream().use { aus ->
                val puffer = ByteArray(64 * 1024)
                while (true) {
                    val n = ein.read(puffer)
                    if (n < 0) break
                    aus.write(puffer, 0, n)
                    digest.update(puffer, 0, n)
                    gelesen += n
                    if (m.groesse > 0) {
                        val p = (gelesen * 100 / m.groesse).toInt().coerceIn(0, 100)
                        if (p != letzteProzent) {
                            letzteProzent = p
                            ZustandsSpeicher.setzeInstallation(m.paket, InstallStatus.Laedt(p))
                        }
                    }
                }
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (hash != m.sha256) {
            datei.delete()
            error("Prüfsumme passt nicht – die Datei ist wohl noch nicht fertig synchronisiert. Später erneut versuchen.")
        }
        datei
    }

    private fun pruefe(context: Context, datei: File, m: UpdateManifest) {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val archiv = pm.getPackageArchiveInfo(datei.path, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("APK ist beschädigt und lässt sich nicht lesen.")
        if (archiv.packageName != m.paket) error("APK gehört zu ${archiv.packageName}, nicht zu ${m.paket}.")
        if (archiv.longVersionCode != m.versionCode) error("Version in der APK (${archiv.longVersionCode}) passt nicht zu update.json (${m.versionCode}).")

        val installiert = Pruefer.installiert(pm, m.paket) ?: return
        if (archiv.longVersionCode <= installiert.longVersionCode) {
            error("Installiert ist bereits Version ${installiert.versionName} (${installiert.longVersionCode}) – kein Update nötig.")
        }
        val neu = Pruefer.signaturen(archiv)
        if (neu.isEmpty()) error("Signatur der APK ist nicht lesbar.")
        if (neu.intersect(Pruefer.signaturen(installiert)).isEmpty()) {
            error("Signatur passt nicht zur installierten App. Aus Sicherheitsgründen wird nicht installiert.")
        }
    }

    private fun uebergebe(context: Context, datei: File, m: UpdateManifest, label: String) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(m.paket)
            setSize(datei.length())
            // Nie still installieren: Android soll jede Installation ausdrücklich bestätigen lassen.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("update.apk", 0, datei.length()).use { aus ->
                datei.inputStream().use { it.copyTo(aus) }
                session.fsync(aus)
            }
            val intent = Intent(context, InstallErgebnisReceiver::class.java)
                .putExtra(InstallErgebnisReceiver.EXTRA_PAKET, m.paket)
                .putExtra(InstallErgebnisReceiver.EXTRA_LABEL, label)
                .putExtra(InstallErgebnisReceiver.EXTRA_VERSION, m.versionName)
            val pi = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pi.intentSender)
        }
    }
}

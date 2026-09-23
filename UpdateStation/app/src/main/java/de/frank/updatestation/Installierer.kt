package de.frank.updatestation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** Was ein Installationswunsch ausgelöst hat – für die Rückmeldung an den Nutzer. */
enum class Start { GESTARTET, ERSETZT, LAEUFT_BEREITS, WARTET_AUF_BESTAETIGUNG }

object Installierer {

    /**
     * Pakete, deren Installation läuft – vom Start des Downloads bis zum endgültigen Ergebnis im
     * [InstallErgebnisReceiver] (auch während Android auf die Bestätigung wartet). Verhindert eine
     * zweite Session und einen zweiten Bestätigungsdialog für dasselbe Paket. Nur im Speicher:
     * ein Prozessneustart hebt die Sperre auf, [SPERRE_MAX_MS] begrenzt eine liegengebliebene.
     */
    private val laufend = mutableMapOf<String, Long>()
    private const val SPERRE_MAX_MS = 15 * 60_000L

    fun laeuft(paket: String): Boolean = synchronized(laufend) {
        val seit = laufend[paket] ?: return false
        if (System.currentTimeMillis() - seit < SPERRE_MAX_MS) true else { laufend -= paket; false }
    }

    private fun beginne(paket: String): Boolean = synchronized(laufend) {
        if (laeuft(paket)) false else { laufend[paket] = System.currentTimeMillis(); true }
    }

    /** Bei eigenen Fehlern vor der Übergabe an Android. */
    fun beende(paket: String) = synchronized(laufend) { laufend -= paket }

    /** Vom Receiver bei endgültigem Ergebnis (Erfolg, Fehler, Abbruch): Sperre und gespeicherte Session lösen. */
    fun abschliessen(context: Context, paket: String) {
        beende(paket)
        Einstellungen(context).loescheOffeneSession(paket)
    }

    /**
     * Gehört ein Receiver-Ergebnis zur aktuellen Session des Pakets? Nur wenn die eigene, im
     * PendingIntent mitgegebene Session-ID gleich der gespeicherten offenen Session ist. Ergebnisse
     * ersetzter oder älterer Sessions (auch nach Prozessneustart) ändern so nie Sperre oder Status.
     */
    fun gehoertZurAktuellen(context: Context, paket: String, sitzung: Int): Boolean {
        if (sitzung < 0) return false
        val offen = Einstellungen(context).offeneSession(paket) ?: return false
        return offen.first == sitzung
    }

    /**
     * Lädt die APK, prüft sie gegen update.json und die installierte App und übergibt sie erst dann
     * an den Paket-Installer. Installiert wird nur, wenn der versionCode wirklich höher ist, und
     * immer erst nach Bestätigung im Android-Dialog.
     */
    suspend fun installiere(context: Context, eintrag: AppEintrag): Start {
        val m = eintrag.fund.manifest
        val paket = m.paket
        if (!beginne(paket)) {
            Log.i(TAG, "$paket: Installation läuft bereits, kein zweiter Start")
            return Start.LAEUFT_BEREITS
        }
        // Nach Prozessneustart: wartet eine frühere Session noch auf Bestätigung, keinen zweiten Dialog öffnen.
        Einstellungen(context).offeneSession(paket)?.let { (id, seit) ->
            val lebt = runCatching { context.packageManager.packageInstaller.getSessionInfo(id) }.getOrNull() != null
            if (lebt && System.currentTimeMillis() - seit < SPERRE_MAX_MS) {
                beende(paket)
                ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufBestaetigung)
                Log.i(TAG, "$paket: frühere Session $id wartet noch auf Bestätigung")
                return Start.WARTET_AUF_BESTAETIGUNG
            }
        }
        var datei: File? = null
        var ersetzt = false
        try {
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Laedt(0))
            datei = lade(context, eintrag)

            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Prueft)
            pruefe(context, datei, m)

            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufBestaetigung)
            ersetzt = uebergebe(context, datei, m, eintrag.label)
            // Sperre bleibt bis zum Ergebnis im InstallErgebnisReceiver.
        } catch (e: CancellationException) {
            beende(paket)
            ZustandsSpeicher.setzeInstallation(paket, null)
            throw e
        } catch (e: Exception) {
            beende(paket)
            Log.w(TAG, "$paket: Vorbereitung der Installation fehlgeschlagen", e)
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(e.message ?: "Unbekannter Fehler"))
        } finally {
            // Die Session hat die Daten kopiert (oder es gab einen Fehler): nur die eigene Datei löschen.
            datei?.delete()
        }
        return if (ersetzt) Start.ERSETZT else Start.GESTARTET
    }

    private suspend fun lade(context: Context, eintrag: AppEintrag): File = withContext(Dispatchers.IO) {
        val m = eintrag.fund.manifest
        // Der Paketname kommt aus update.json: vor jeder Pfadbildung streng prüfen.
        if (!PAKET_MUSTER.matches(m.paket)) error("Ungültiger Paketname in update.json.")
        val quelle = Quellen.aktuelle(context) ?: error("Keine Update-Quelle eingerichtet.")
        val ordner = File(context.cacheDir, "updates").apply { mkdirs() }
        // Nur liegengebliebene Reste aufräumen – laufende Downloads anderer Pakete bleiben unberührt.
        val grenze = System.currentTimeMillis() - 24 * 60 * 60_000L
        ordner.listFiles()?.filter { it.lastModified() < grenze }?.forEach { it.delete() }
        val teil = File(ordner, "${m.paket}-vc${m.versionCode}.apk.part")
        val datei = File(ordner, "${m.paket}-vc${m.versionCode}.apk")
        if (teil.parentFile != ordner || datei.parentFile != ordner) error("Ungültiger Dateiname für den Download.")
        val digest = MessageDigest.getInstance("SHA-256")
        var gelesen = 0L
        var letzteProzent = -1
        try {
            quelle.oeffne(eintrag.fund).use { ein ->
                teil.outputStream().use { aus ->
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
                error("Prüfsumme passt nicht – die Datei ist wohl noch nicht fertig synchronisiert. Später erneut versuchen.")
            }
            datei.delete()
            if (!teil.renameTo(datei)) error("Heruntergeladene APK ließ sich nicht ablegen.")
        } finally {
            teil.delete()
        }
        datei
    }

    /** Paket, Version, Hash (in [lade]), Signatur laut update.json und Signatur der installierten App müssen zusammenpassen. */
    private fun pruefe(context: Context, datei: File, m: UpdateManifest) {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val archiv = pm.getPackageArchiveInfo(datei.path, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("APK ist beschädigt und lässt sich nicht lesen.")
        if (archiv.packageName != m.paket) error("APK gehört zu ${archiv.packageName}, nicht zu ${m.paket}.")
        if (archiv.longVersionCode != m.versionCode) error("Version in der APK (${archiv.longVersionCode}) passt nicht zu update.json (${m.versionCode}).")

        val neu = Pruefer.signaturen(archiv)
        if (neu.isEmpty()) error("Signatur der APK ist nicht lesbar.")
        // Vertrag: jede update.json nennt den SHA-256 des Signaturzertifikats, und die APK trägt genau diese Signatur.
        if (!SHA256_MUSTER.matches(m.signaturSha256)) error("update.json nennt keine gültige Signatur – aus Sicherheitsgründen wird nicht installiert.")
        if (m.signaturSha256 !in neu) error("Signatur der APK passt nicht zu update.json. Aus Sicherheitsgründen wird nicht installiert.")
        val installiert = Pruefer.installiert(pm, m.paket)
        if (installiert != null) {
            if (archiv.longVersionCode <= installiert.longVersionCode) {
                error("Installiert ist bereits Version ${installiert.versionName} (${installiert.longVersionCode}) – kein Update nötig.")
            }
            if (neu.intersect(Pruefer.signaturen(installiert)).isEmpty()) {
                error("Signatur passt nicht zur installierten App. Aus Sicherheitsgründen wird nicht installiert.")
            }
        }
        Log.i(TAG, "${m.paket}: APK geprüft (vc ${archiv.longVersionCode}, Signatur passt, ${if (installiert == null) "neue App" else "Update"})")
    }

    /** Übergibt die geprüfte APK an Android. Liefert true, wenn dabei ein vorheriger Versuch ersetzt wurde. */
    private fun uebergebe(context: Context, datei: File, m: UpdateManifest, label: String): Boolean {
        val installer = context.packageManager.packageInstaller
        val einst = Einstellungen(context)
        // Nur hier landen alte Sessions, die abgelaufen (älter als SPERRE_MAX_MS) oder unbekannt sind:
        // verwerfen, ihre Bestätigungs-Benachrichtigung entfernen und den Ersatz klar melden –
        // so sind nie zwei Bestätigungsdialoge für dieselbe App offen.
        val alte = installer.mySessions.filter { it.appPackageName == m.paket }
        alte.forEach { alt ->
            runCatching { installer.abandonSession(alt.sessionId) }
            Log.w(TAG, "${m.paket}: vorheriger Installationsversuch (Session ${alt.sessionId}) ersetzt")
        }
        if (alte.isNotEmpty()) Benachrichtigungen.entferneBestaetigung(context, m.paket)
        einst.loescheOffeneSession(m.paket)
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(m.paket)
            setSize(datei.length())
            // Nie still installieren: Android soll jede Installation ausdrücklich bestätigen lassen.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }
        val id = installer.createSession(params)
        try {
            // Vor dem Commit dauerhaft speichern: der Receiver nimmt nur Ergebnisse dieser ID an.
            if (!einst.setzeOffeneSession(m.paket, id)) error("Installationsvorgang ließ sich nicht speichern.")
            installer.openSession(id).use { session ->
                session.openWrite("update.apk", 0, datei.length()).use { aus ->
                    datei.inputStream().use { it.copyTo(aus) }
                    session.fsync(aus)
                }
                val intent = Intent(context, InstallErgebnisReceiver::class.java)
                    .putExtra(InstallErgebnisReceiver.EXTRA_PAKET, m.paket)
                    .putExtra(InstallErgebnisReceiver.EXTRA_LABEL, label)
                    .putExtra(InstallErgebnisReceiver.EXTRA_VERSION, m.versionName)
                    .putExtra(InstallErgebnisReceiver.EXTRA_SITZUNG, id)
                val pi = PendingIntent.getBroadcast(
                    context, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pi.intentSender)
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(id) }
            einst.loescheOffeneSession(m.paket)
            throw e
        }
        return alte.isNotEmpty()
    }
}

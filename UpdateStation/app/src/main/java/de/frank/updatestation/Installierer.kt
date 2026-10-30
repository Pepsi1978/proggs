package de.frank.updatestation

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/** Eigener, erwarteter Fehler mit festem [code] fürs Diagnoseprotokoll; die Meldung ist für die Oberfläche. */
class InstallFehler(val code: String, text: String) : IllegalStateException(text)

private fun fehler(code: String, text: String): Nothing = throw InstallFehler(code, text)

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

    /**
     * Mehrere Updates laden und prüfen gleichzeitig, höchstens [GLEICHZEITIG] auf einmal – der
     * Drive-Anbieter bricht bei zu vielen großen Downloads nebeneinander eher ab.
     */
    private const val GLEICHZEITIG = 3
    private val ladePlaetze = Semaphore(GLEICHZEITIG)

    /**
     * Installationen laufen in einem eigenen Bereich, unabhängig von der Activity: Drehen oder ein
     * Designwechsel bricht keinen Download und keine wartende Übergabe ab.
     */
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Startet die Installation im eigenen Bereich; das Ergebnis darf die Activity abwarten, ohne sie festzuhalten. */
    fun starte(context: Context, eintrag: AppEintrag, freigegeben: Boolean = false): Deferred<Start> {
        val app = context.applicationContext
        return bereich.async {
            // Jarvis nie ohne die Frage nach dem Zeitpunkt; danach kommt der Start aus Aufschub mit freigegeben.
            val ohneFrage = freigegeben || eintrag.paket != Aufschub.PAKET_JARVIS || Aufschub.frage(app, eintrag.paket) == Aufschub.Frage.OHNE_OVERLAY
            if (ohneFrage) installiere(app, eintrag) else Start.GESTARTET
        }
    }

    /**
     * Alle Updates werden sofort an Android übergeben, nur die Bestätigungsdialoge öffnen einzeln:
     * Solange einer offen ist, verwirft Android den nächsten oft still. Die übrigen warten hier, bis
     * das Ergebnis des offenen kommt; nach [DIALOG_WARTEN_MS] geht es trotzdem weiter, damit ein
     * liegengelassener Dialog nicht alles aufhält. Die Benachrichtigungen zum Bestätigen kommen
     * davon unabhängig sofort. Nur im Speicher: nach einem Prozessneustart bleiben die Benachrichtigungen.
     */
    private class Dialog(val paket: String, val intent: Intent)
    private val wartendeDialoge = ArrayDeque<Dialog>()
    private var offenerDialog: String? = null
    private val zeitgeber = Handler(Looper.getMainLooper())
    private const val DIALOG_WARTEN_MS = 2 * 60_000L

    /** Vom Receiver bei STATUS_PENDING_USER_ACTION. Liefert false, wenn sich der Dialog nicht öffnen ließ. */
    fun zeigeBestaetigung(context: Context, paket: String, intent: Intent): Boolean {
        val jetzt = synchronized(wartendeDialoge) {
            wartendeDialoge.removeAll { it.paket == paket }
            if (offenerDialog == null || offenerDialog == paket) { offenerDialog = paket; true }
            else { wartendeDialoge += Dialog(paket, intent); false }
        }
        if (!jetzt) {
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufDialog)
            return true
        }
        return oeffne(context.applicationContext, paket, intent)
    }

    private fun oeffne(context: Context, paket: String, intent: Intent): Boolean {
        ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufBestaetigung)
        zeitgeber.removeCallbacksAndMessages(null)
        zeitgeber.postDelayed({ gibDialogFrei(context, paket) }, DIALOG_WARTEN_MS)
        val geoeffnet = runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "$paket: Bestätigungsdialog ließ sich nicht öffnen", it) }
            .isSuccess
        if (!geoeffnet) gibDialogFrei(context, paket)
        return geoeffnet
    }

    /** Gibt den Dialogplatz von [paket] frei und öffnet den nächsten wartenden Dialog. */
    private fun gibDialogFrei(context: Context, paket: String) {
        val naechster = synchronized(wartendeDialoge) {
            wartendeDialoge.removeAll { it.paket == paket }
            if (offenerDialog != paket) return
            offenerDialog = null
            wartendeDialoge.removeFirstOrNull()?.also { offenerDialog = it.paket }
        } ?: return
        oeffne(context, naechster.paket, naechster.intent)
    }

    /** Bei eigenen Fehlern vor der Übergabe an Android. */
    fun beende(paket: String) = synchronized(laufend) { laufend -= paket }

    /** Vom Receiver bei endgültigem Ergebnis (Erfolg, Fehler, Abbruch): Sperre, Session und Dialogplatz lösen. */
    fun abschliessen(context: Context, paket: String) {
        beende(paket)
        Einstellungen(context).loescheOffeneSession(paket)
        gibDialogFrei(context.applicationContext, paket)
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
    private suspend fun installiere(context: Context, eintrag: AppEintrag): Start {
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
                Diagnose.ereignis(context, Phase.INSTALLATION, "WARTET_AUF_BESTAETIGUNG", "paket" to paket)
                return Start.WARTET_AUF_BESTAETIGUNG
            }
        }
        var datei: File? = null
        var ersetzt = false
        Diagnose.ereignis(context, Phase.INSTALLATION, "START", "paket" to paket, "vc" to m.versionCode)
        try {
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufLadeplatz)
            val geladen = ladePlaetze.withPermit { lade(context, eintrag) }.also { datei = it }
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Prueft)
            pruefe(context, geladen, m)

            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufBestaetigung)
            ersetzt = uebergebe(context, geladen, m, eintrag.label)
            Diagnose.ereignis(context, Phase.INSTALLATION, if (ersetzt) "UEBERGEBEN_ERSETZT" else "UEBERGEBEN", "paket" to paket, "vc" to m.versionCode)
            // Sperre bleibt bis zum Ergebnis im InstallErgebnisReceiver.
        } catch (e: CancellationException) {
            beende(paket)
            ZustandsSpeicher.setzeInstallation(paket, null)
            Diagnose.ereignis(context, Phase.INSTALLATION, "ABGEBROCHEN", "paket" to paket)
            throw e
        } catch (e: Exception) {
            beende(paket)
            if (e is InstallFehler) {
                val phase = if (e.code == "HASH" || e.code == "ABLAGE") Phase.DOWNLOAD else Phase.PRUEFUNG
                Diagnose.ereignis(context, phase, e.code, "paket" to paket, "vc" to m.versionCode)
            } else {
                Diagnose.ereignis(context, Phase.DOWNLOAD, "FEHLER", "paket" to paket, "klasse" to Diagnose.klasse(e))
            }
            Log.w(TAG, "$paket: Vorbereitung der Installation fehlgeschlagen", e)
            ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(e.message ?: "Unbekannter Fehler", m.versionCode))
        } finally {
            // Die Session hat die Daten kopiert (oder es gab einen Fehler): nur die eigene Datei löschen.
            // Eine vorgeladene bleibt liegen, bis die Installation gelungen ist (Dialog abgebrochen = nächstes Mal sofort).
            datei?.takeIf { !Vorladen.istVorgeladen(context, it) }?.delete()
        }
        return if (ersetzt) Start.ERSETZT else Start.GESTARTET
    }

    private suspend fun lade(context: Context, eintrag: AppEintrag): File = withContext(Dispatchers.IO) {
        val m = eintrag.fund.manifest
        // Der Paketname kommt aus update.json: vor jeder Pfadbildung streng prüfen.
        if (!PAKET_MUSTER.matches(m.paket)) fehler("PAKETNAME", "Ungültiger Paketname in update.json.")
        // Schon im Hintergrund geladen: Prüfsumme gegen update.json, dann ohne Download weiter.
        Vorladen.vorhanden(context, m)?.let { fertig ->
            val hash = MessageDigest.getInstance("SHA-256").let { d ->
                fertig.inputStream().use { ein -> val p = ByteArray(64 * 1024); while (true) { val n = ein.read(p); if (n < 0) break; d.update(p, 0, n) } }
                d.digest().joinToString("") { "%02x".format(it) }
            }
            if (hash == m.sha256) {
                Diagnose.ereignis(context, Phase.DOWNLOAD, "AUS_VORLADEN", "paket" to m.paket, "vc" to m.versionCode)
                return@withContext fertig
            }
            fertig.delete()
        }
        val quelle = Quellen.aktuelle(context) ?: fehler("KEINE_QUELLE", "Keine Update-Quelle eingerichtet.")
        val ordner = File(context.cacheDir, "updates").apply { mkdirs() }
        // Nur liegengebliebene Reste aufräumen – laufende Downloads anderer Pakete bleiben unberührt.
        val grenze = System.currentTimeMillis() - 24 * 60 * 60_000L
        ordner.listFiles()?.filter { it.lastModified() < grenze }?.forEach { it.delete() }
        val teil = File(ordner, "${m.paket}-vc${m.versionCode}.apk.part")
        val datei = File(ordner, "${m.paket}-vc${m.versionCode}.apk")
        if (teil.parentFile != ordner || datei.parentFile != ordner) fehler("DATEINAME", "Ungültiger Dateiname für den Download.")
        val digest = MessageDigest.getInstance("SHA-256")
        var gelesen = 0L
        var letzteProzent = -1
        ZustandsSpeicher.setzeInstallation(m.paket, InstallStatus.WirdBereitgestellt(m.groesse / 1_000_000))
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
                fehler("HASH", "Prüfsumme passt nicht – die Datei ist wohl noch nicht fertig synchronisiert. Später erneut versuchen.")
            }
            datei.delete()
            if (!teil.renameTo(datei)) fehler("ABLAGE", "Heruntergeladene APK ließ sich nicht ablegen.")
        } finally {
            teil.delete()
        }
        datei
    }

    /** Paket, Version, Hash (in [lade]), Signatur laut update.json und Signatur der installierten App müssen zusammenpassen. */
    private suspend fun pruefe(context: Context, datei: File, m: UpdateManifest) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        val archiv = pm.getPackageArchiveInfo(datei.path, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: fehler("APK_UNLESBAR", "APK ist beschädigt und lässt sich nicht lesen.")
        if (archiv.packageName != m.paket) fehler("PAKET", "APK gehört zu ${archiv.packageName}, nicht zu ${m.paket}.")
        if (archiv.longVersionCode != m.versionCode) fehler("VERSION", "Version in der APK (${archiv.longVersionCode}) passt nicht zu update.json (${m.versionCode}).")

        val neu = Pruefer.signaturen(archiv)
        if (neu.isEmpty()) fehler("SIGNATUR_UNLESBAR", "Signatur der APK ist nicht lesbar.")
        // Vertrag: jede update.json nennt den SHA-256 des Signaturzertifikats, und die APK trägt genau diese Signatur.
        if (!SHA256_MUSTER.matches(m.signaturSha256)) fehler("SIGNATUR_MANIFEST_FEHLT", "update.json nennt keine gültige Signatur – aus Sicherheitsgründen wird nicht installiert.")
        if (m.signaturSha256 !in neu) fehler("SIGNATUR_MANIFEST", "Signatur der APK passt nicht zu update.json. Aus Sicherheitsgründen wird nicht installiert.")
        val installiert = Pruefer.installiert(pm, m.paket)
        if (installiert != null) {
            if (archiv.longVersionCode <= installiert.longVersionCode) {
                fehler("SCHON_AKTUELL", "Installiert ist bereits Version ${installiert.versionName} (${installiert.longVersionCode}) – kein Update nötig.")
            }
            if (neu.intersect(Pruefer.signaturen(installiert)).isEmpty()) {
                fehler("SIGNATUR_INSTALLIERT", "Signatur passt nicht zur installierten App. Aus Sicherheitsgründen wird nicht installiert.")
            }
        }
        Log.i(TAG, "${m.paket}: APK geprüft (vc ${archiv.longVersionCode}, Signatur passt, ${if (installiert == null) "neue App" else "Update"})")
        Diagnose.ereignis(context, Phase.PRUEFUNG, "OK", "paket" to m.paket, "vc" to archiv.longVersionCode,
            "art" to if (installiert == null) "NEUE_APP" else "UPDATE")
    }

    /** Übergibt die geprüfte APK an Android. Liefert true, wenn dabei ein vorheriger Versuch ersetzt wurde. */
    private suspend fun uebergebe(context: Context, datei: File, m: UpdateManifest, label: String): Boolean = withContext(Dispatchers.IO) {
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
        val pflichtTeil = pflichtTeil(context, datei, m)
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(m.paket)
            setSize(datei.length() + (pflichtTeil?.length() ?: 0))
            // Updates tippt Frank in UpdateStation selbst an, Paket, Version und Signatur sind oben geprüft: dann ohne
            // zweiten Android-Dialog (klappt, wenn UpdateStation die App zuletzt installiert hat; sonst fragt Android
            // trotzdem nach, das fängt STATUS_PENDING_USER_ACTION ab). Neue Apps bestätigt er immer ausdrücklich.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val update = Pruefer.installiert(context.packageManager, m.paket) != null
                setRequireUserAction(
                    if (update) PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
                    else PackageInstaller.SessionParams.USER_ACTION_REQUIRED,
                )
            }
        }
        val id = installer.createSession(params)
        try {
            // Vor dem Commit dauerhaft speichern: der Receiver nimmt nur Ergebnisse dieser ID an.
            if (!einst.setzeOffeneSession(m.paket, id)) fehler("SPEICHERN", "Installationsvorgang ließ sich nicht speichern.")
            installer.openSession(id).use { session ->
                session.openWrite("update.apk", 0, datei.length()).use { aus ->
                    datei.inputStream().use { it.copyTo(aus) }
                    session.fsync(aus)
                }
                pflichtTeil?.let { teil ->
                    session.openWrite("freigabe.apk", 0, teil.length()).use { aus ->
                        teil.inputStream().use { it.copyTo(aus) }
                        session.fsync(aus)
                    }
                }
                val intent = Intent(context, InstallErgebnisReceiver::class.java)
                    .putExtra(InstallErgebnisReceiver.EXTRA_PAKET, m.paket)
                    .putExtra(InstallErgebnisReceiver.EXTRA_LABEL, label)
                    .putExtra(InstallErgebnisReceiver.EXTRA_VERSION, m.versionName)
                    .putExtra(InstallErgebnisReceiver.EXTRA_VERSION_CODE, m.versionCode)
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
        } finally {
            pflichtTeil?.delete()
        }
        alte.isNotEmpty()
    }

    /** Jarvis trägt seinen Pflicht-Teil als Asset in der APK: Android installiert die App nur zusammen mit ihm. */
    private fun pflichtTeil(context: Context, datei: File, m: UpdateManifest): File? = ZipFile(datei).use { zip ->
        val eintrag = zip.getEntry("assets/freigabe-teil.apk") ?: return@use null
        val ziel = File(File(context.cacheDir, "updates").apply { mkdirs() }, "${m.paket}-vc${m.versionCode}-freigabe.tmp")
        zip.getInputStream(eintrag).use { ein -> ziel.outputStream().use { ein.copyTo(it) } }
        ziel
    }
}

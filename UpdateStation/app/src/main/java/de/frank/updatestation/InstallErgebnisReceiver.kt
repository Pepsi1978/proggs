package de.frank.updatestation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log

class InstallErgebnisReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val paket = intent.getStringExtra(EXTRA_PAKET) ?: return
        val label = intent.getStringExtra(EXTRA_LABEL) ?: paket
        val version = intent.getStringExtra(EXTRA_VERSION).orEmpty()
        val sitzung = intent.getIntExtra(EXTRA_SITZUNG, -1)
        if (!Installierer.gehoertZurAktuellen(context, paket, sitzung)) {
            // Ergebnis einer ersetzten oder älteren Session (auch aus Versionen vor dieser): darf Sperre
            // und Status des aktuellen Versuchs nicht ändern. Android installiert ggf. trotzdem weiter;
            // den installierten Stand bewertet UpdateStation beim nächsten Öffnen bzw. Scan neu.
            Log.i(TAG, "$paket: Ergebnis der Session $sitzung gehört nicht zur aktuellen – ignoriert")
            Diagnose.ereignis(context, Phase.INSTALLATION, "ERGEBNIS_IGNORIERT", "paket" to paket, "sitzung" to sitzung)
            return
        }
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val bestaetigen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (bestaetigen == null) {
                    // Ohne Bestätigungs-Intent kann niemand die Installation freigeben – sichtbar melden statt hängen.
                    val text = "Android hat keinen Bestätigungsdialog geliefert. Bitte erneut installieren."
                    Log.w(TAG, "$paket: STATUS_PENDING_USER_ACTION ohne EXTRA_INTENT")
                    Diagnose.ereignis(context, Phase.INSTALLATION, "KEIN_BESTAETIGUNGSDIALOG", "paket" to paket)
                    Installierer.abschliessen(context, paket)
                    ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(text))
                    Benachrichtigungen.fehler(context, paket, label, text)
                    return
                }
                bestaetigen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ZustandsSpeicher.setzeInstallation(paket, InstallStatus.WartetAufBestaetigung)
                // Immer zusätzlich eine Benachrichtigung: Aus dem Hintergrund verwirft Android den
                // startActivity-Aufruf oft still, dann bliebe die Installation ohne Hinweis liegen.
                val gemeldet = Benachrichtigungen.bestaetigen(context, paket, label, bestaetigen)
                val geoeffnet = runCatching { context.startActivity(bestaetigen) }
                    .onFailure { Log.w(TAG, "$paket: Bestätigungsdialog ließ sich nicht öffnen", it) }
                    .isSuccess
                Log.i(TAG, "$paket: Bestätigung nötig (Dialog geöffnet=$geoeffnet, Benachrichtigung=$gemeldet)")
                Diagnose.ereignis(context, Phase.INSTALLATION, "BESTAETIGUNG_NOETIG", "paket" to paket,
                    "dialog" to geoeffnet, "benachrichtigung" to gemeldet)
                if (!geoeffnet && !gemeldet) {
                    // Niemand kann bestätigen: Sperre lösen, der nächste Versuch verwirft die alte Session.
                    Installierer.abschliessen(context, paket)
                    ZustandsSpeicher.setzeInstallation(
                        paket,
                        InstallStatus.Fehler("Bestätigung nicht anzeigbar: Benachrichtigungen erlauben und erneut installieren."),
                    )
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Installierer.abschliessen(context, paket)
                Diagnose.ereignis(context, Phase.INSTALLATION, "ERFOLG", "paket" to paket)
                ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fertig)
                Benachrichtigungen.entferneUpdate(context, paket)
                Benachrichtigungen.entferneBestaetigung(context, paket)
                Benachrichtigungen.installiert(context, paket, label, version)
                Pruefer.bewerteGespeichert(context)
            }
            else -> {
                val text = when (status) {
                    PackageInstaller.STATUS_FAILURE_ABORTED -> "Installation abgebrochen."
                    PackageInstaller.STATUS_FAILURE_CONFLICT -> "Konflikt mit der installierten App (Signatur oder Paket)."
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "APK passt nicht zu diesem Gerät."
                    PackageInstaller.STATUS_FAILURE_STORAGE -> "Zu wenig Speicherplatz."
                    else -> intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Installation fehlgeschlagen."
                }
                Log.w(TAG, "$paket: Installation fehlgeschlagen (Status $status): $text")
                Diagnose.ereignis(context, Phase.INSTALLATION, "ENDSTATUS", "paket" to paket, "status" to status)
                Installierer.abschliessen(context, paket)
                ZustandsSpeicher.setzeInstallation(paket, InstallStatus.Fehler(text))
                Benachrichtigungen.entferneBestaetigung(context, paket)
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED) Benachrichtigungen.fehler(context, paket, label, text)
            }
        }
    }

    companion object {
        const val EXTRA_PAKET = "paket"
        const val EXTRA_LABEL = "label"
        const val EXTRA_VERSION = "version"
        const val EXTRA_SITZUNG = "sitzung"
    }
}

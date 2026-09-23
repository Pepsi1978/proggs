package de.frank.updatestation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import de.frank.updatestation.ui.Aktionen
import de.frank.updatestation.ui.HauptScreen
import de.frank.updatestation.ui.UpdateStationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var einst: Einstellungen
    private val snackbar = SnackbarHostState()
    private var quelle by mutableStateOf<String?>(null)
    private var drivePfad by mutableStateOf(Einstellungen.STANDARD_PFAD)
    private var intervall by mutableStateOf(Einstellungen.STANDARD_INTERVALL)
    private var darfInstallieren by mutableStateOf(true)
    private var darfBenachrichtigen by mutableStateOf(true)

    private val benachrichtigungsRecht = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        aktualisiereRechte()
    }

    private val googleFreigabe = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { ergebnis ->
        if (ergebnis.resultCode == Activity.RESULT_OK) {
            runCatching { Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(ergebnis.data) }
                .onSuccess { driveVerbunden() }
                .onFailure { melde("Google-Freigabe fehlgeschlagen: ${it.message}") }
        } else {
            melde("Google-Freigabe abgebrochen.")
        }
    }

    private val ordnerWahl = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        einst.ordnerUri = uri.toString()
        einst.quelle = "ordner"
        quelle = "ordner"
        pruefe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        einst = Einstellungen(this)
        quelle = einst.quelle
        drivePfad = einst.drivePfad
        intervall = einst.intervallMinuten

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hatBenachrichtigungsRecht()) {
            benachrichtigungsRecht.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            UpdateStationTheme {
                val zustand by ZustandsSpeicher.zustand.collectAsState()
                HauptScreen(
                    zustand = zustand,
                    quelle = quelle,
                    drivePfad = drivePfad,
                    intervall = intervall,
                    darfInstallieren = darfInstallieren,
                    darfBenachrichtigen = darfBenachrichtigen,
                    snackbar = remember { snackbar },
                    aktionen = Aktionen(
                        pruefen = ::pruefe,
                        installiere = ::installiere,
                        installiereAlle = {
                            lifecycleScope.launch {
                                ZustandsSpeicher.zustand.value.eintraege
                                    .filter { it.status == Status.UPDATE && !Installierer.laeuft(it.paket) }
                                    .forEach { Installierer.installiere(this@MainActivity, it) }
                            }
                        },
                        verbindeDrive = ::verbindeDrive,
                        waehleOrdner = { ordnerWahl.launch(null) },
                        erlaubeInstallation = {
                            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                        },
                        erlaubeBenachrichtigungen = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) && !hatBenachrichtigungsRecht()) {
                                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                benachrichtigungsRecht.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        speicherePfad = { neu ->
                            einst.drivePfad = neu.ifBlank { Einstellungen.STANDARD_PFAD }
                            drivePfad = einst.drivePfad
                            pruefe()
                        },
                        speichereIntervall = { minuten ->
                            einst.intervallMinuten = minuten
                            intervall = einst.intervallMinuten
                            PruefWorker.plane(this@MainActivity)
                            melde("Automatische Prüfung ${Einstellungen.intervallText(intervall)} (ungefährer Takt)")
                        },
                    ),
                )
            }
        }
        verarbeite(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        verarbeite(intent)
    }

    override fun onResume() {
        super.onResume()
        aktualisiereRechte()
        Pruefer.bewerteGespeichert(this)
        // Beim Öffnen frisch prüfen, wenn die letzte Prüfung älter als 5 Minuten ist.
        if (quelle != null && System.currentTimeMillis() - einst.letztePruefung > 5 * 60_000) pruefe()
    }

    /** "Jetzt installieren" aus der Benachrichtigung. */
    private fun verarbeite(intent: Intent?) {
        val paket = intent?.getStringExtra(Benachrichtigungen.EXTRA_INSTALLIERE) ?: return
        intent.removeExtra(Benachrichtigungen.EXTRA_INSTALLIERE)
        Pruefer.bewerteGespeichert(this)
        val eintrag = ZustandsSpeicher.zustand.value.eintraege.firstOrNull { it.paket == paket && it.status == Status.UPDATE }
        if (eintrag == null) melde("Dieses Update ist nicht mehr nötig – die App ist bereits aktuell.") else installiere(eintrag)
    }

    private fun installiere(eintrag: AppEintrag) {
        if (!packageManager.canRequestPackageInstalls()) {
            melde("Bitte zuerst „Unbekannte Apps installieren“ erlauben.")
            return
        }
        if (Installierer.laeuft(eintrag.paket)) {
            melde("${eintrag.label}: Installation läuft bereits – bitte den Android-Dialog bestätigen oder abbrechen.")
            return
        }
        lifecycleScope.launch {
            when (Installierer.installiere(this@MainActivity, eintrag)) {
                Start.LAEUFT_BEREITS -> melde("${eintrag.label}: Installation läuft bereits.")
                Start.WARTET_AUF_BESTAETIGUNG ->
                    melde("${eintrag.label}: Ein früherer Versuch wartet noch auf deine Bestätigung – Benachrichtigung antippen oder abbrechen.")
                Start.ERSETZT -> melde("${eintrag.label}: Ein vorheriger, liegengebliebener Installationsversuch wurde ersetzt.")
                Start.GESTARTET -> Unit
            }
        }
    }

    private fun pruefe() {
        if (ZustandsSpeicher.zustand.value.prueftGerade) return
        lifecycleScope.launch {
            runCatching { Pruefer.pruefe(this@MainActivity) }
        }
    }

    private fun verbindeDrive() {
        lifecycleScope.launch {
            try {
                val r = DriveAuth.autorisiere(this@MainActivity)
                val pi = r.pendingIntent
                if (r.hasResolution() && pi != null) {
                    googleFreigabe.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                } else {
                    driveVerbunden()
                }
            } catch (e: ApiException) {
                melde(
                    if (e.statusCode == 10) "Google kennt diese App noch nicht: In der Cloud Console einen Android-OAuth-Client für de.frank.updatestation mit dem SHA-1 des Debug-Keys anlegen."
                    else "Google-Anmeldung fehlgeschlagen (${e.statusCode}): ${e.message}",
                )
            } catch (e: Exception) {
                melde("Google-Anmeldung fehlgeschlagen: ${e.message}")
            }
        }
    }

    private fun driveVerbunden() {
        einst.quelle = "drive"
        quelle = "drive"
        pruefe()
    }

    private fun hatBenachrichtigungsRecht() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun aktualisiereRechte() {
        darfInstallieren = packageManager.canRequestPackageInstalls()
        darfBenachrichtigen = hatBenachrichtigungsRecht()
    }

    private fun melde(text: String) {
        lifecycleScope.launch { snackbar.showSnackbar(text) }
    }
}

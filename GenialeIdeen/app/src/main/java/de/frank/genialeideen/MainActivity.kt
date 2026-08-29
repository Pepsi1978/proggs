package de.frank.genialeideen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.frank.genialeideen.backup.Sicherung
import de.frank.genialeideen.observability.IdeenCrashHandler
import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.ui.GenialeIdeenApp
import de.frank.genialeideen.ui.GoldKnopf
import de.frank.genialeideen.ui.IdeenViewModel
import de.frank.genialeideen.ui.Meldung
import de.frank.genialeideen.ui.theme.GenialeIdeenTheme
import de.frank.genialeideen.ui.theme.LocalGold

class MainActivity : FragmentActivity() {
    private val container get() = (application as GenialeIdeenApplication).container

    private val viewModel: IdeenViewModel by viewModels {
        viewModelFactory {
            initializer { IdeenViewModel(application, container) }
        }
    }

    private var mikrofonErlaubt by mutableStateOf(false)
    private lateinit var sperrPrompt: BiometricPrompt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        mikrofonErlaubt = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

        sperrPrompt = baueSperrPrompt()

        IdeenLog.info(
            "App",
            "onCreate",
            "Start",
            mapOf("version" to BuildConfig.VERSION_NAME, "gebaut" to BuildConfig.VERSION_BUMPED_AT),
        )

        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            val schrift by viewModel.schriftgroesse.collectAsStateWithLifecycle()
            val gesperrt by container.appLockManager.locked.collectAsStateWithLifecycle()

            GenialeIdeenTheme(themeWahl = theme, schriftSkalierung = schrift) {
                // Solange gesperrt ist, bleibt der Inhalt verdeckt — kein Durchblitzen.
                LaunchedEffect(gesperrt) {
                    if (gesperrt) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                val mikrofonAnfrage = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { erlaubt ->
                    mikrofonErlaubt = erlaubt
                    if (!erlaubt) {
                        viewModel.zeige(
                            Meldung(
                                "Ohne Mikrofon kann die App nichts aufnehmen. Du kannst es in den " +
                                    "System-Einstellungen der App nachträglich freigeben.",
                                istFehler = true,
                            ),
                        )
                    }
                }

                val meldungsAnfrage = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hat = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        // Ohne dieses Recht gibt es beim Vorlesen keine Bedienknöpfe.
                        if (!hat) meldungsAnfrage.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val exportZiel = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json"),
                ) { ziel: Uri? -> ziel?.let(viewModel::exportiere) }

                val importQuelle = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { quelle: Uri? ->
                    quelle?.let { adresse ->
                        viewModel.vorschau(adresse) { vorschau ->
                            if (vorschau == null) return@vorschau
                            viewModel.zeige(
                                Meldung(
                                    "${vorschau.ideen} Ideen werden eingespielt, " +
                                        "${vorschau.bestehende} bestehende bleiben. " +
                                        "Sicherung vom ${vorschau.erstelltAm}.",
                                    wiederholen = { viewModel.importiere(adresse, ersetzen = false) },
                                ),
                            )
                        }
                    }
                }

                if (gesperrt) {
                    SperrBildschirm(aufEntsperren = ::frageSperreAb)
                    LaunchedEffect(Unit) { frageSperreAb() }
                } else {
                    GenialeIdeenApp(
                        viewModel = viewModel,
                        mikrofonErlaubt = mikrofonErlaubt,
                        aufMikrofonFragen = { mikrofonAnfrage.launch(Manifest.permission.RECORD_AUDIO) },
                        aufAnmelden = { viewModel.meldeAn(this@MainActivity) },
                        aufExport = { exportZiel.launch(Sicherung.dateiName()) },
                        aufImport = { importQuelle.launch(arrayOf("application/json", "text/plain")) },
                        aufAppSperreUmschalten = ::schalteAppSperre,
                        aufSeiteOeffnen = ::oeffneSeite,
                    )
                }
            }
        }

        IdeenCrashHandler.letzteAktion = "App gestartet"
    }

    /** Öffnet die Bestätigungsseite der Anmeldung im Browser (Baustein O.1). */
    private fun oeffneSeite(adresse: String?) {
        val ziel = adresse?.takeIf(String::isNotBlank) ?: "https://auth.openai.com/codex/device"
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ziel))) }
            .onFailure {
                viewModel.zeige(
                    Meldung("Es liess sich kein Browser öffnen: ${it.message}", istFehler = true),
                )
            }
    }

    private fun baueSperrPrompt(): BiometricPrompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                container.appLockManager.finishAuthentication(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                container.appLockManager.finishAuthentication(false)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    viewModel.zeige(Meldung("Entsperren ging nicht: $errString", istFehler = true))
                }
            }
        },
    )

    private fun frageSperreAb() {
        if (!container.appLockManager.beginPrompt(enabling = false)) return
        sperrPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Geniale Ideen entsperren")
                .setSubtitle("Mit Fingerabdruck, Gesicht oder der Bildschirmsperre")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build(),
        )
    }

    /** Beim Einschalten wird einmal bestätigt — sonst sperrt man sich versehentlich aus. */
    private fun schalteAppSperre(an: Boolean) {
        if (!an) {
            container.appLockManager.disable()
            return
        }
        val moeglich = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        if (moeglich != BiometricManager.BIOMETRIC_SUCCESS) {
            viewModel.zeige(
                Meldung(
                    "Dieses Gerät hat keine eingerichtete Sperre. Leg im System erst eine PIN " +
                        "oder einen Fingerabdruck an.",
                    istFehler = true,
                ),
            )
            return
        }
        if (!container.appLockManager.beginPrompt(enabling = true)) return
        sperrPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("App-Sperre einschalten")
                .setSubtitle("Einmal bestätigen, damit die Sperre wirklich zu dir passt")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
                .build(),
        )
    }
}

@Composable
private fun SperrBildschirm(aufEntsperren: () -> Unit) {
    val gold = LocalGold.current
    Box(
        modifier = Modifier.fillMaxSize().background(gold.hintergrund),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Outlined.Fingerprint,
                contentDescription = null,
                tint = gold.primaer,
                modifier = Modifier.size(64.dp),
            )
            Text(
                "Geniale Ideen ist gesperrt",
                style = MaterialTheme.typography.titleMedium,
                color = gold.textPrimaer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            Text(
                "Entsperr mit Fingerabdruck, Gesicht oder deiner Bildschirmsperre.",
                style = MaterialTheme.typography.bodySmall,
                color = gold.textGedaempft,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.padding(top = 24.dp)) {
                GoldKnopf(text = "Entsperren", aufTipp = aufEntsperren)
            }
        }
    }
}

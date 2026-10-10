package de.frank.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.ui.AppViewModel
import de.frank.jarvis.ui.JarvisApp

/** FragmentActivity, weil die Abfrage des Fingerabdrucks sie verlangt. */
class MainActivity : FragmentActivity() {
    private val vm: AppViewModel by viewModels()

    private val hinweisErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.lagePruefen()
        JarvisDienst.abgleichen(this)
    }

    private val kalenderErlaubnis = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.lagePruefen() }

    /** Erst der Standort während der Nutzung, danach in einem zweiten Schritt „Immer zulassen“ (Android verlangt die Trennung). */
    private val standortErlaubnis = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { ergebnis ->
        if (ergebnis.values.any { it } && android.os.Build.VERSION.SDK_INT >= 29 && !de.frank.jarvis.fahrt.Standort.immerErlaubt(this)) standortImmerErlaubnis.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        vm.lagePruefen()
    }
    private val standortImmerErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.lagePruefen() }

    private val mikroErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.mikrofonErlaubt(it) }

    /** Fingerabdruck, ersatzweise die Gerätesperre (PIN, Muster). */
    private val nachweis = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

    private fun sperreMoeglich(): Boolean = BiometricManager.from(this).canAuthenticate(nachweis) == BiometricManager.BIOMETRIC_SUCCESS

    private fun entsperren() {
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                vm.gesperrt = false
                vm.zuletztSichtbar = System.currentTimeMillis()
            }
        }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Jarvis entsperren").setSubtitle("Mit Fingerabdruck oder Gerätesperre").setAllowedAuthenticators(nachweis).build())
    }

    override fun onStart() {
        super.onStart()
        // Sperren beim Start und nach mehr als 30 Sekunden außerhalb der App. Ohne eingerichteten Fingerabdruck
        // und ohne Gerätesperre bleibt die App offen, sonst käme niemand mehr hinein.
        val zuLangeWeg = (vm.zuletztSichtbar == 0L || System.currentTimeMillis() - vm.zuletztSichtbar > 30_000) && System.currentTimeMillis() > vm.sperrAufschubBis
        vm.sperrAufschubBis = 0L
        if (vm.einstellungen.appSperre && sperreMoeglich() && (vm.gesperrt || zuLangeWeg)) {
            vm.gesperrt = true
            entsperren()
        } else if (!vm.gesperrt) {
            vm.zuletztSichtbar = System.currentTimeMillis()
        }
    }

    override fun onStop() {
        if (!vm.gesperrt) vm.zuletztSichtbar = System.currentTimeMillis()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        vm.hinweiseAnfragen = {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                hinweisErlaubnis.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        vm.entsperrenAnfragen = { entsperren() }
        vm.mikrofonAnfragen = {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.mikrofonErlaubt(true)
            else mikroErlaubnis.launch(Manifest.permission.RECORD_AUDIO)
        }
        vm.kalenderAnfragen = { kalenderErlaubnis.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) }
        vm.standortAnfragen = {
            vm.sperrAufschubBis = System.currentTimeMillis() + 120_000
            if (!de.frank.jarvis.fahrt.Standort.erlaubt(this)) standortErlaubnis.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            else if (android.os.Build.VERSION.SDK_INT >= 29) standortImmerErlaubnis.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (savedInstanceState == null) vm.hinweiseAnfragen()
        JarvisDienst.abgleichen(this)
        setContent { JarvisApp(vm, this) }
    }

    override fun onResume() {
        super.onResume()
        vm.lagePruefen()
    }
}

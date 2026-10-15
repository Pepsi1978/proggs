package de.frank.jarvis

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
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
import de.frank.jarvis.weckwort.WeckwortDienst

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

    /** Mikrofon für „Hey Jarvis“: danach gleich lauschen. */
    private val weckwortErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.lagePruefen()
        WeckwortDienst.abgleichen(this)
    }

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

    /** Jarvis hat selbst eine andere App geöffnet (Teilen, Datei, Anmeldung im Browser): Die Rückkehr von dort ist kein neuer Start. */
    private var eigenerAbsprung = false
    /** Seit dem letzten onStop kam „Hey Jarvis“: Das beginnt selbst ein neues Gespräch oder führt das sichtbare fort. */
    private var weckwortSeitStopp = false
    /** Diese Rückkehr zeigt den Startbildschirm mit neuem Gespräch, ausgeführt in onResume (nach einem möglichen Weckwort-Intent). */
    private var rueckkehrNeu = false

    @Deprecated("Fängt alle Absprünge der App ab, auch startActivity und die Ergebnis-Launcher.")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        eigenerAbsprung = true
        @Suppress("DEPRECATION")
        super.startActivityForResult(intent, requestCode, options)
    }

    @Deprecated("Fängt Absprünge über IntentSender ab (etwa Google-Anmeldung).")
    override fun startIntentSenderForResult(intent: android.content.IntentSender, requestCode: Int, fillInIntent: Intent?, flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?) {
        eigenerAbsprung = true
        @Suppress("DEPRECATION")
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun onStart() {
        super.onStart()
        sichtbar = true
        vm.sichtbarMerken(true)
        // War Jarvis nicht auf dem Bildschirm (andere App, Startbildschirm, Display aus), beginnt die Rückkehr neu. Nicht beim
        // Kaltstart (das regelt das ViewModel samt Neustart nach Update), nicht nach Auf- oder Zuklappen, nicht nach einem
        // Absprung, den Jarvis selbst geöffnet hat, und nicht über dem Sperrbildschirm.
        rueckkehrNeu = vm.hintergrundSeit != 0L && !eigenerAbsprung && System.currentTimeMillis() > vm.sperrAufschubBis && !vm.ueberSperre
        vm.hintergrundSeit = 0L
        eigenerAbsprung = false
        // Sperren beim Start und nach mehr als 30 Sekunden außerhalb der App. Ohne eingerichteten Fingerabdruck
        // und ohne Gerätesperre bleibt die App offen, sonst käme niemand mehr hinein.
        // Über dem Sperrbildschirm nicht nach dem Fingerabdruck fragen: Die Sperre bleibt, Jarvis hört und antwortet nur.
        if (vm.ueberSperre) return
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
        sichtbar = false
        weckwortSeitStopp = false
        if (!isChangingConfigurations) {
            vm.sichtbarMerken(false)
            vm.hintergrundSeit = System.currentTimeMillis()
        }
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
        vm.weckwortMikroAnfragen = { weckwortErlaubnis.launch(Manifest.permission.RECORD_AUDIO) }
        // Immer die gerade lebende Activity: Wird sie neu aufgebaut (Fold auf- oder zugeklappt), beendet die neue das Zeigen über der Sperre.
        vm.ueberSperreBeenden = {
            ueberSperreZeigen(false)
            // Ohne App-Sperre gilt die Sperre nur für die Zeit über dem Sperrbildschirm.
            if (!vm.einstellungen.appSperre || !sperreMoeglich()) vm.gesperrt = false
        }
        // Neu aufgebaut, während „Hey Jarvis“ über dem Sperrbildschirm läuft: dort bleiben.
        if (vm.ueberSperre) ueberSperreZeigen(true)
        if (savedInstanceState == null) vm.hinweiseAnfragen()
        JarvisDienst.abgleichen(this)
        setContent { JarvisApp(vm, this) }
        if (savedInstanceState == null) weckwortPruefen(intent, neu = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        weckwortPruefen(intent, neu = false)
    }

    /** „Hey Jarvis“ wurde erkannt: aufnehmen, piepen, antworten; bei gesperrtem Handy über dem Sperrbildschirm. */
    private fun weckwortPruefen(intent: Intent?, neu: Boolean) {
        if (intent?.getBooleanExtra(WeckwortDienst.EXTRA_WECKWORT, false) != true) return
        weckwortSeitStopp = true
        // Sofort melden, dass die App übernommen hat: Sonst greift der Wächter des Weckworts während des Starts ein.
        de.frank.jarvis.weckwort.Weckwort.uebernommen()
        // Zählt, ob Jarvis schon vorn war, bevor das Weckwort die App geöffnet hat (der Dienst hält es beim Ruf fest).
        // Eine eben erst erzeugte Activity war nie sichtbar: Kaltstart beginnt immer ein neues Gespräch.
        val fortsetzen = !neu && intent.getBooleanExtra(WeckwortDienst.EXTRA_WAR_SICHTBAR, false)
        intent.removeExtra(WeckwortDienst.EXTRA_WECKWORT)
        intent.removeExtra(WeckwortDienst.EXTRA_WAR_SICHTBAR)
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
            vm.ueberSperre = true
            ueberSperreZeigen(true)
            // Über dem Sperrbildschirm nie das Gespräch zeigen, auch ohne App-Sperre.
            vm.gesperrt = true
        }
        // Nur das ViewModel festhalten, nicht diese Activity: Sie kann während des Gesprächs neu entstehen.
        val modell = vm
        modell.weckwortStart(fortsetzen) {
            // Fertig: Der Sperrbildschirm legt sich wieder darüber.
            if (modell.ueberSperre) {
                modell.ueberSperre = false
                modell.ueberSperreBeenden()
            }
        }
    }

    private fun ueberSperreZeigen(an: Boolean) {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(an)
            setTurnScreenOn(an)
        } else {
            @Suppress("DEPRECATION")
            val flaggen = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            if (an) window.addFlags(flaggen) else window.clearFlags(flaggen)
        }
    }

    companion object {
        /** Ist Jarvis gerade sichtbar (zwischen onStart und onStop)? Liest der Weckwort-Dienst, bevor er die App öffnet. */
        @Volatile var sichtbar = false; private set
    }

    override fun onResume() {
        super.onResume()
        // Bewusste Wechsel innerhalb der sichtbaren App sind davon nie betroffen: Das greift nur nach einer Rückkehr von außen.
        if (rueckkehrNeu && !weckwortSeitStopp && !vm.ueberSperre) vm.startbildschirmNeu()
        rueckkehrNeu = false
        // Gedrosselt: Die Prüfung weckt die Partner-Apps, das muss nicht bei jedem Zurückkehren sein.
        vm.lagePruefen(sofort = false)
        // Nicht in lagePruefen: Das läuft schon im Aufbau des ViewModels, bevor dessen spätere Felder bestehen.
        vm.abfahrtLaden()
        // Hier ist Jarvis sichtbar: Nur dann darf Android den Mikrofon-Dienst starten.
        WeckwortDienst.abgleichen(this)
    }
}

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
import androidx.core.content.ContextCompat
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.ui.AppViewModel
import de.frank.jarvis.ui.JarvisApp
import de.frank.jarvis.weckwort.WeckwortDienst

/** FragmentActivity, weil die Abfrage des Fingerabdrucks sie verlangt. */
class MainActivity : FragmentActivity() {
    // Prozessweit, nicht an diese Activity gebunden: Ein Sprachmodus aus dem Hintergrund (Sprachblase) läuft darin weiter.
    private val vm: AppViewModel by lazy { AppViewModel.hole(this) }

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
    private val liveErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { vm.liveErlaubt(it) }

    /** Plus-Menü der Eingabeblase: Foto aufnehmen, Fotos wählen, Dateien wählen. Die Kamera schreibt in den Cache (FileProvider). */
    private val kamera = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok -> vm.kameraZiel?.takeIf { ok }?.let { vm.anhangHinzu(listOf(it)) } }
    private val fotos = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { vm.anhangHinzu(it) }
    private val dateien = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { vm.anhangHinzu(it) }

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

    override fun onStart() {
        super.onStart()
        sichtbar = true
        de.frank.jarvis.ui.SprachBlase.abgleichen(this)
        vm.sichtbarMerken(true)
        // Die Rückkehr aus dem Hintergrund zeigt denselben Bildschirm samt Gespräch wie beim Verlassen (Franks Wunsch vom
        // 22.10.2026; früher begann sie mit dem Startbildschirm neu). Nur „Hey Jarvis“ aus dem Hintergrund beginnt ein neues
        // Gespräch auf dem Hauptbildschirm (AppViewModel.weckwortStart mit fortsetzen=false).
        // Mit App-Sperre zeigt die Übersicht der letzten Apps kein Bild des Gesprächs (sonst läse man es dort ohne Fingerabdruck).
        // Bewusst kein FLAG_SECURE: Screenshots und Bildschirmaufnahmen zum Testen bleiben möglich.
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!vm.einstellungen.appSperre)
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
        // Beim Auf- oder Zuklappen bleibt Jarvis sichtbar: Die Sprachblase blitzte sonst kurz auf.
        if (!isChangingConfigurations) {
            sichtbar = false
            // Läuft der Sprachmodus weiter, während Frank in eine andere App wechselt, erscheint die Sprachblase.
            de.frank.jarvis.ui.SprachBlase.abgleichen(this)
        }
        // Die App-Sperre kann in den Einstellungen umgeschaltet worden sein: Das Bild für die Übersicht entsteht erst jetzt.
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!vm.einstellungen.appSperre)
        if (!isChangingConfigurations) vm.sichtbarMerken(false)
        if (!vm.gesperrt) vm.zuletztSichtbar = System.currentTimeMillis()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        vm.kameraZiel?.let { outState.putString(KAMERA_ZIEL, it.toString()) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        // Die Kamera-App beendet Jarvis oft im Hintergrund: Das Ziel des Fotos kommt dann aus dem gesicherten Zustand zurück.
        if (vm.kameraZiel == null) savedInstanceState?.getString(KAMERA_ZIEL)?.let { vm.kameraZiel = android.net.Uri.parse(it) }
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
        vm.liveMikroAnfragen = {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) vm.liveErlaubt(true)
            else liveErlaubnis.launch(Manifest.permission.RECORD_AUDIO)
        }
        vm.kameraAnfragen = {
            vm.sperrAufschubBis = System.currentTimeMillis() + 300_000
            val datei = de.frank.jarvis.data.Anhaenge.kameraDatei(this)
            val ziel = androidx.core.content.FileProvider.getUriForFile(this, "de.frank.jarvis.dateien", datei)
            vm.kameraZiel = ziel
            runCatching { kamera.launch(ziel) }.onFailure { vm.meldung = "Keine Kamera-App gefunden." }
        }
        vm.fotosAnfragen = {
            vm.sperrAufschubBis = System.currentTimeMillis() + 300_000
            fotos.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        vm.dateienAnfragen = {
            vm.sperrAufschubBis = System.currentTimeMillis() + 300_000
            dateien.launch(arrayOf("*/*"))
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
        if (savedInstanceState == null) rueckfragePruefen(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        weckwortPruefen(intent, neu = false)
        rueckfragePruefen(intent)
    }

    /** Tipp auf die Benachrichtigung einer Rückfrage: ihr Gespräch öffnen und die Frage im Vollbild zeigen. */
    private fun rueckfragePruefen(intent: Intent?) {
        val gespraech = intent?.getStringExtra(de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.EXTRA_GESPRAECH) ?: return
        val frageId = intent.getStringExtra(de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.EXTRA_RUECKFRAGE).orEmpty()
        intent.removeExtra(de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.EXTRA_GESPRAECH)
        intent.removeExtra(de.frank.jarvis.faehigkeit.ProgrammierFaehigkeit.EXTRA_RUECKFRAGE)
        // Die Rückfrage lebt im Jarvis-Reiter: Stand die App zuletzt auf einem anderen, erschiene sonst kein Vollbild.
        vm.reiter = de.frank.jarvis.ui.Reiter.JARVIS
        // Eine ältere offene Rückfrage (auch aus einer anderen Session) bleibt vorn; diese reiht sich dahinter ein.
        if (frageId.isNotEmpty()) vm.naechsteRueckfrage(gespraech to frageId) else vm.sitzungOeffnen(gespraech)
    }

    /** „Hey Jarvis“ wurde erkannt: aufnehmen, piepen, antworten; bei gesperrtem Handy über dem Sperrbildschirm. */
    private fun weckwortPruefen(intent: Intent?, neu: Boolean) {
        if (intent?.getBooleanExtra(WeckwortDienst.EXTRA_WECKWORT, false) != true) return
        // Nur der eigene Weckwort-Dienst darf so eine Aufnahme starten: Er gibt einen Einmal-Code mit, den nur er kennt.
        val code = intent.getStringExtra(de.frank.jarvis.weckwort.Weckwort.EXTRA_RUF_CODE)
        intent.removeExtra(de.frank.jarvis.weckwort.Weckwort.EXTRA_RUF_CODE)
        if (!de.frank.jarvis.weckwort.Weckwort.rufCodeEinloesen(code)) {
            intent.removeExtra(WeckwortDienst.EXTRA_WECKWORT)
            intent.removeExtra(WeckwortDienst.EXTRA_WAR_SICHTBAR)
            return
        }
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
        private const val KAMERA_ZIEL = "kamera_ziel"
    }

    override fun onResume() {
        super.onResume()
        // Jarvis ist offen: Frank hat die klingelnde Meldung gesehen (Antippen öffnet meist Jarvis), der Weckton darf aufhören.
        de.frank.jarvis.meldung.WeckerTon.alleStopp()
        // Gedrosselt: Die Prüfung weckt die Partner-Apps, das muss nicht bei jedem Zurückkehren sein.
        vm.lagePruefen(sofort = false)
        // Nicht in lagePruefen: Das läuft schon im Aufbau des ViewModels, bevor dessen spätere Felder bestehen.
        vm.abfahrtLaden()
        // Verpasste Rückfragen der Programmier-Sitzungen und ihren Ausgang vom Server holen.
        vm.rueckfragenAbgleichen()
        // Hier ist Jarvis sichtbar: Nur dann darf Android den Mikrofon-Dienst starten.
        WeckwortDienst.abgleichen(this)
    }

    /**
     * Endgültig geschlossen (etwa aus der Übersicht weggewischt), der Prozess läuft für den Dienst weiter: Das prozessweite
     * ViewModel darf diese Activity nicht über ihre Anfrage-Funktionen festhalten. Beim Auf- oder Zuklappen ersetzt die neue
     * Activity sie gleich selbst in onCreate.
     */
    override fun onDestroy() {
        if (!isChangingConfigurations) {
            val modell = vm
            val app = applicationContext
            val nachweisArt = nachweis
            modell.hinweiseAnfragen = {}
            modell.entsperrenAnfragen = {}
            modell.mikrofonAnfragen = {}
            modell.liveMikroAnfragen = {}
            modell.kameraAnfragen = {}
            modell.fotosAnfragen = {}
            modell.dateienAnfragen = {}
            modell.kalenderAnfragen = {}
            modell.standortAnfragen = {}
            modell.weckwortMikroAnfragen = {}
            // Ohne Activity gibt es nichts mehr über der Sperre zu zeigen; die Sperre selbst wird wie bisher aufgehoben.
            modell.ueberSperreBeenden = {
                if (!modell.einstellungen.appSperre || BiometricManager.from(app).canAuthenticate(nachweisArt) != BiometricManager.BIOMETRIC_SUCCESS) modell.gesperrt = false
            }
        }
        super.onDestroy()
    }
}

package de.frank.jarvis.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.Ansage
import de.frank.jarvis.speech.GespraechsFokus
import de.frank.jarvis.ui.theme.JarvisTheme
import de.frank.jarvis.ui.theme.LocalFarben
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fragt vor einer adb-Installation, wann sie starten soll, damit sie keine laufende Aufnahme abbricht: als Overlay über
 * jeder App, ohne den Fokus zu nehmen, sodass die App darunter bedienbar bleibt. Werkzeuge/jarvis-install startet den
 * Dienst, liest aus [DATEI] erst „offen“ oder „fehler“ und dann die Antwort und installiert selbst, ohne Antwort nach
 * [FRIST_S] Sekunden; Warten und Installieren laufen am Rechner, weil Jarvis dabei beendet wird. Bringt der Start einen
 * PendingIntent in [EXTRA_ANTWORT] mit ([InstallFrageBruecke], UpdateStation), geht die Antwort dorthin statt in die Datei.
 */
open class InstallFrageDienst : Service(), SavedStateRegistryOwner {

    private val lebenszyklus = LifecycleRegistry(this)
    private val speicher = SavedStateRegistryController.create(this)
    private var ansicht: ComposeView? = null
    private var kennung by mutableStateOf("")
    private var antwort: PendingIntent? = null
    private var ansage: Job? = null

    override val lifecycle: Lifecycle get() = lebenszyklus
    override val savedStateRegistry: SavedStateRegistry get() = speicher.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        speicher.performRestore(null)
        lebenszyklus.currentState = Lifecycle.State.RESUMED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val neueKennung = intent?.getStringExtra(EXTRA_KENNUNG).orEmpty()
        val neueFrage = ansicht == null || neueKennung != kennung
        kennung = neueKennung
        antwort = antwortAus(intent)
        // Ein abgelehnter Vordergrundstart darf nie den Prozess und damit eine laufende Aufnahme mitreißen.
        val gestartet = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(BENACHRICHTIGUNG, hinweis(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else {
                startForeground(BENACHRICHTIGUNG, hinweis())
            }
        }.onFailure { Logbuch.fehler("Installationsfrage: Vordergrundstart abgelehnt", it) }.isSuccess
        when {
            !gestartet -> scheitere()
            !SprachBlase.darf(this) -> {
                Logbuch.fehler("Installationsfrage: Overlay-Recht fehlt", null)
                scheitere()
            }
            ansicht != null || zeige() -> {
                if (neueFrage) kuendigeAn()
                schreibe("offen")
            }
            else -> scheitere()
        }
        return START_NOT_STICKY
    }

    private fun hinweis(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(KANAL, "Installationsfrage", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, KANAL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Neue Jarvis-Version wartet auf deine Antwort")
            .build()
    }

    /** Meldet dem Skript, dass keine Frage erscheint: Es installiert dann nicht. */
    private fun scheitere() {
        schreibe("fehler")
        stopSelf()
    }

    private fun zeige(): Boolean {
        val einst = Einstellungen.get(this)
        val neu = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@InstallFrageDienst)
            setViewTreeSavedStateRegistryOwner(this@InstallFrageDienst)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@InstallFrageDienst))
            setContent {
                JarvisTheme(einst.modus, einst.design) {
                    Frage(kennung, onWahl = ::antworte, onFristAbgelaufen = ::fristAbgelaufen)
                }
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            title = "Jarvis-Installationsfrage"
        }
        return runCatching { getSystemService(WindowManager::class.java).addView(neu, lp) }
            .onSuccess { ansicht = neu }
            .onFailure { Logbuch.fehler("Installationsfrage ließ sich nicht einblenden", it) }
            .isSuccess
    }

    /**
     * Stimme, Lautstärke und Satz wie unter Einstellungen → Benachrichtigungen. Bei laufender Aufnahme oder laufendem Gespräch
     * bleibt es beim Fenster: Die Stimme landete sonst in der Mitschrift, und genau die soll die Frage schützen. Beginnt so etwas
     * während der Ansage oder wird sie abgeschaltet, verstummt sie. Sie unterbricht ein laufendes Vorlesen: Ohne Antwort beendete
     * die Installation es ohnehin. Ein Fehler hier darf nie die Frage oder eine laufende Aufnahme mitreißen.
     */
    private fun kuendigeAn() {
        runCatching {
            val art = Meldungsarten.INSTALLFRAGE
            if (!sollSprechen() || Meldungen.inRuhe(this, art) || ansage?.isActive == true) return
            val profil = Meldungen.profil(this, art)
            ansage = lifecycleScope.launch {
                val stimme = launch {
                    runCatching { Ansage.sprichUndWarte(this@InstallFrageDienst, "ansage", "Jarvis-Update", profil.sprechtext(""), profil.lautstaerke, profil.stimmWahl) }
                        .onFailure { if (it !is CancellationException) Logbuch.fehler("Installationsfrage: Ansage gescheitert", it) }
                }
                while (stimme.isActive) {
                    if (!sollSprechen()) stimme.cancel()
                    delay(200)
                }
            }
        }.onFailure { Logbuch.fehler("Installationsfrage: Ansage gescheitert", it) }
    }

    private fun sollSprechen() = Meldungen.an(this, Meldungsarten.INSTALLFRAGE) && !GespraechsFokus.aufnahme && !GespraechsFokus.stilleHinweise

    private fun antwortAus(intent: Intent?): PendingIntent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_ANTWORT, PendingIntent::class.java)
        } else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_ANTWORT)
        }

    /** Das Skript zählt selbst mit; UpdateStation erfährt das Ende der Frist hier. */
    private fun fristAbgelaufen() {
        if (antwort != null) schreibe("frist")
        stopSelf()
    }

    /** Das Fenster bleibt offen, wenn die Antwort nicht ankäme: Sonst installierte das Skript trotz gewählter Verzögerung. */
    private fun antworte(minuten: Int) {
        if (schreibe("$minuten")) stopSelf()
    }

    /** Erst vollständig schreiben, dann umbenennen: Das Skript fragt laufend ab und darf nie eine halbe Zeile lesen. */
    private fun schreibe(wert: String): Boolean = antwort?.let { ziel ->
        runCatching { ziel.send(this, 0, Intent().putExtra(EXTRA_WERT, wert)) }
            .onFailure { Logbuch.fehler("Installationsfrage: Antwort nicht zugestellt", it) }.isSuccess
    } ?: runCatching {
        val ordner = checkNotNull(getExternalFilesDir(null)) { "kein externer Speicher" }
        val teil = File(ordner, "$DATEI.teil")
        teil.writeText("$kennung $wert")
        check(teil.renameTo(File(ordner, DATEI))) { "Umbenennen gescheitert" }
    }.onFailure { Logbuch.fehler("Installationsfrage: Datei nicht geschrieben", it) }.isSuccess

    override fun onDestroy() {
        ansicht?.let { v -> runCatching { getSystemService(WindowManager::class.java).removeViewImmediate(v) } }
        ansicht = null
        // Bricht auch die Ansage ab: Nach der Antwort ist sie überholt, sie verstummt, und die Lautstärke geht zurück.
        lebenszyklus.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private companion object {
        const val EXTRA_KENNUNG = "kennung"
        const val EXTRA_ANTWORT = "antwort"
        const val EXTRA_WERT = "wert"
        const val DATEI = "install-antwort.txt"
        const val KANAL = "jarvis_installationsfrage"
        const val BENACHRICHTIGUNG = 4310
        const val FRIST_S = 30
    }

    @Composable
    private fun Frage(kennung: String, onWahl: (Int) -> Unit, onFristAbgelaufen: () -> Unit) {
        val f = LocalFarben.current
        val form = RoundedCornerShape(f.radius)
        var rest by remember { mutableIntStateOf(FRIST_S) }
        // Eine neue Anfrage (andere Kennung) beginnt die Frist von vorn.
        LaunchedEffect(kennung) {
            rest = FRIST_S
            while (rest > 0) {
                delay(1_000)
                rest--
            }
            onFristAbgelaufen()
        }
        // Der Rand um die Karte gibt dem Schatten Platz; das Fenster schneidet sonst ab.
        Box(Modifier.padding(16.dp)) {
            Column(
                Modifier
                    .widthIn(max = 360.dp)
                    .shadow(16.dp, form)
                    .background(f.hgUnten, form)
                    .border(1.5.dp, f.primaer, form)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Eine neue Jarvis-Version kann installiert werden. Jetzt installieren?", color = f.text)
                Text("Ohne Antwort startet die Installation in $rest Sekunden.", color = f.textLeise)
                (0..5).chunked(2).forEach { reihe ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        reihe.forEach { minuten ->
                            val breite = Modifier.weight(1f)
                            val rand = PaddingValues(horizontal = 4.dp)
                            val text = when (minuten) {
                                0 -> "Sofort"
                                1 -> "In 1 Minute"
                                else -> "In $minuten Minuten"
                            }
                            if (minuten == 0) {
                                Button(
                                    { onWahl(0) }, breite, contentPadding = rand,
                                    colors = ButtonDefaults.buttonColors(containerColor = f.primaer, contentColor = f.aufPrimaer),
                                ) { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            } else {
                                OutlinedButton(
                                    { onWahl(minuten) }, breite, contentPadding = rand,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = f.text),
                                ) { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Eingang für UpdateStation: eigene Klasse, weil ein Dienst nur eine Erlaubnis tragen kann (siehe Manifest). */
class InstallFrageBruecke : InstallFrageDienst()

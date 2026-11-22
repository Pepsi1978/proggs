package de.frank.jarvis.ablage

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.ui.theme.JarvisTheme
import de.frank.jarvis.ui.theme.LocalFarben

/**
 * Die Wecker-Karte einer klingelnden Erinnerung: Text und ein großer Knopf „Wecker ausschalten“. Sie erscheint als Vollbild-Overlay über jeder App
 * (aus [ErinnerungKlingelDienst]) und, bei gesperrtem Handy, als eigene Ansicht über dem Sperrbildschirm ([ErinnerungWeckerActivity]).
 * Ein Tipp bestätigt alles, was gerade klingelt; die App muss dafür nicht geöffnet werden.
 */
object ErinnerungWeckerAnsicht {
    /** Die Sperrbildschirm-Ansicht ist vorn: Das Overlay zeigt dann nichts, damit das Vollbild nicht doppelt steht. */
    val vorn = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Kurzfassung in höchstens zwei Sätzen. */
    fun kurz(text: String): String {
        val saetze = text.trim().split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val k = saetze.take(2).joinToString(" ").ifBlank { "Erinnerung" }
        return if (k.length > 220) k.take(217).trimEnd() + " …" else k
    }

    /** Ganzer Bildschirm: oben groß die Kurzfassung, unten der große Knopf „Wecker ausschalten“. */
    @Composable
    fun Vollbild(liste: List<Faellig>, onAus: () -> Unit) {
        val f = LocalFarben.current
        Column(
            Modifier.fillMaxSize().background(f.hgUnten).safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Alarm, null, tint = f.primaer, modifier = Modifier.size(34.dp))
                Text(if (liste.size > 1) "${liste.size} Erinnerungen" else "Erinnerung", color = f.primaer, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Spacer(Modifier.height(28.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                liste.take(3).forEach { e ->
                    Text(kurz(e.erinnerung.kurz), color = f.text, fontSize = if (liste.size > 1) 26.sp else 34.sp, lineHeight = if (liste.size > 1) 33.sp else 42.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onAus, Modifier.fillMaxWidth().widthIn(max = 520.dp).height(84.dp),
                colors = ButtonDefaults.buttonColors(containerColor = f.primaer, contentColor = f.aufPrimaer),
            ) { Text("Wecker ausschalten", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            if (liste.any { it.erinnerung.ortsgebunden }) {
                val c = androidx.compose.ui.platform.LocalContext.current
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedButton({ ErinnerungPlaner.erledigtKlingelnde(c) }, Modifier.fillMaxWidth().widthIn(max = 520.dp).height(64.dp)) {
                    Text("Erledigt", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = f.text)
                }
            }
        }
    }
}

/** Über dem Sperrbildschirm: schaltet den Bildschirm ein, zeigt die Karte und schließt sich, sobald nichts mehr klingelt. */
class ErinnerungWeckerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        val einst = Einstellungen.get(this)
        setContent {
            JarvisTheme(einst.modus, einst.design) {
                val liste by ErinnerungPlaner.klingelt.collectAsState()
                LaunchedEffect(liste.isEmpty()) { if (liste.isEmpty()) finish() }
                BackHandler { }
                if (liste.isNotEmpty()) ErinnerungWeckerAnsicht.Vollbild(liste) { ErinnerungPlaner.ausschalten(this@ErinnerungWeckerActivity) }
            }
        }
    }

    override fun onResume() { super.onResume(); ErinnerungWeckerAnsicht.vorn.value = true }
    override fun onPause() { ErinnerungWeckerAnsicht.vorn.value = false; super.onPause() }

    companion object {
        fun gesperrt(context: android.content.Context): Boolean {
            val app = context.applicationContext
            return app.getSystemService(KeyguardManager::class.java).isKeyguardLocked || !app.getSystemService(android.os.PowerManager::class.java).isInteractive
        }
    }
}

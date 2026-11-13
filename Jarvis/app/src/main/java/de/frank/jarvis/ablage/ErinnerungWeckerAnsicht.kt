package de.frank.jarvis.ablage

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.ui.theme.JarvisTheme
import de.frank.jarvis.ui.theme.LocalFarben
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Die Wecker-Karte einer klingelnden Erinnerung: Text und ein großer Knopf „Wecker ausschalten“. Sie erscheint als kleines Overlay über jeder App
 * (aus [ErinnerungKlingelDienst]) und, bei gesperrtem Handy, als eigene Ansicht über dem Sperrbildschirm ([ErinnerungWeckerActivity]).
 * Ein Tipp bestätigt alles, was gerade klingelt; die App muss dafür nicht geöffnet werden.
 */
object ErinnerungWeckerAnsicht {
    /** Die Sperrbildschirm-Ansicht ist vorn: Das Overlay hält sich dann zurück, damit die Karte nicht doppelt steht. */
    val vorn = MutableStateFlow(false)

    @Composable
    fun Karte(liste: List<Faellig>, onAus: () -> Unit) {
        val f = LocalFarben.current
        val form = RoundedCornerShape(f.radius)
        Box(Modifier.padding(16.dp)) {
            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth().shadow(16.dp, form).background(f.hgUnten, form).border(2.dp, f.primaer, form).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Alarm, null, tint = f.primaer, modifier = Modifier.size(26.dp))
                    Text(if (liste.size > 1) "${liste.size} Erinnerungen" else "Erinnerung", color = f.primaer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                liste.take(3).forEach { e ->
                    Text(e.erinnerung.kurz, color = f.text, fontSize = 17.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
                Button(
                    onAus, Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = f.primaer, contentColor = f.aufPrimaer),
                ) { Text("Wecker ausschalten", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val einst = Einstellungen.get(this)
        setContent {
            JarvisTheme(einst.modus, einst.design) {
                val liste by ErinnerungPlaner.klingelt.collectAsState()
                LaunchedEffect(liste.isEmpty()) { if (liste.isEmpty()) finish() }
                Box(Modifier.fillMaxSize().background(LocalFarben.current.hgUnten), contentAlignment = Alignment.Center) {
                    if (liste.isNotEmpty()) ErinnerungWeckerAnsicht.Karte(liste) { ErinnerungPlaner.ausschalten(this@ErinnerungWeckerActivity) }
                }
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

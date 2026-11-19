package de.frank.aufgaben.erinnerung

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.aufgaben.data.Aufgabe
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Vollbild einer Erinnerung „als Wecker“: oben groß die Aufgabe in höchstens zwei Sätzen, unten „Ausschalten“.
 * Erscheint immer, auch bei entsperrtem Handy (direkt gestartet, dafür braucht die App „Über anderen Apps einblenden“),
 * gesperrt über den Full-Screen-Intent der Benachrichtigung. Zurück schließt nichts; wer mit Home weggeht, bekommt
 * das Vollbild zurück, solange der Wecker läuft (nicht bei Anrufen). Die Knöpfe gehen denselben Weg wie die der Benachrichtigung.
 */
class WeckerVollbildActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        // Dunkel von Anfang an: kein weißes Aufblitzen, helle Symbole in den Systemleisten.
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0xFF101418.toInt()))
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            val a by WeckerVollbild.klingelt.collectAsState()
            LaunchedEffect(a == null) { if (a == null) finish() }
            BackHandler { }
            a?.let { Ansicht(it) }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Ansicht(a: Aufgabe) {
        val gold = Color(0xFFFFC857)
        Column(
            Modifier.fillMaxSize().background(Color(0xFF101418)).safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Alarm, null, tint = gold, modifier = Modifier.size(34.dp))
                Text("Erinnerung", color = gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                WeckerVollbild.kurz(a), color = Color.White, fontSize = 34.sp, lineHeight = 42.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 7, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Button(
                { sende(Planer.AKTION_AUS, a.id) }, Modifier.fillMaxWidth().height(84.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black),
            ) { Text("Ausschalten", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton({ sende(Planer.AKTION_ERLEDIGT, a.id) }, Modifier.weight(1f).height(56.dp)) { Text("Erledigt", fontSize = 18.sp, color = Color.White) }
                OutlinedButton({ sende(Planer.AKTION_SCHLUMMERN, a.id) }, Modifier.weight(1f).height(56.dp)) { Text("In 10 Min.", fontSize = 18.sp, color = Color.White) }
            }
        }
    }

    private fun sende(aktion: String, id: Long) {
        sendBroadcast(Intent(this, AktionsEmpfaenger::class.java).setAction(aktion).putExtra(Planer.ID, id))
    }

    override fun onResume() { super.onResume(); WeckerVollbild.vorn = true }
    override fun onPause() { WeckerVollbild.vorn = false; super.onPause() }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Bewusst mit Home oder Übersicht verlassen, während der Wecker läuft: kurz danach wieder nach vorn.
        // Ein Anruf oder ein anderer Wecker löst das nicht aus.
        if (isFinishing) return
        val app = applicationContext
        Handler(Looper.getMainLooper()).postDelayed({ WeckerVollbild.zurueckholen(app) }, 700)
    }
}

object WeckerVollbild {
    /** Die Aufgabe, deren Wecker gerade klingelt; null, sobald er aus ist. */
    val klingelt = MutableStateFlow<Aufgabe?>(null)

    /** Das Vollbild ist gerade sichtbar vorn. */
    @Volatile var vorn = false

    /** Das direkt gestartete Vollbild kam nicht nach vorn: Meldung wieder laut mit Full-Screen-Intent. */
    @Volatile var rueckfall = false

    fun gesperrt(context: Context): Boolean {
        val app = context.applicationContext
        return app.getSystemService(KeyguardManager::class.java).isKeyguardLocked || !app.getSystemService(PowerManager::class.java).isInteractive
    }

    /** Entsperrt mit Overlay-Recht startet die App das Vollbild selbst, sonst übernimmt der Full-Screen-Intent. */
    fun direkt(context: Context): Boolean = !gesperrt(context) && Settings.canDrawOverlays(context)

    fun absicht(context: Context): Intent = Intent(context, WeckerVollbildActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    /** true, wenn der direkte Start versucht wurde. */
    fun zeigen(context: Context): Boolean {
        if (klingelt.value == null || !direkt(context) || rueckfall) return false
        return runCatching { context.startActivity(absicht(context)) }.isSuccess
    }

    fun zurueckholen(context: Context) {
        val app = context.applicationContext
        if (klingelt.value == null || !app.getSystemService(PowerManager::class.java).isInteractive) return
        if (Settings.canDrawOverlays(app)) runCatching { app.startActivity(absicht(app)) }
    }

    /** Titel plus Text, höchstens zwei Sätze und rund 200 Zeichen. */
    fun kurz(a: Aufgabe): String {
        val titel = a.titel.trim()
        val saetze = a.text.trim().split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val teile = buildList {
            if (titel.isNotEmpty()) add(titel)
            saetze.forEach { s -> if (size < 2 && !titel.contains(s, ignoreCase = true)) add(s) }
        }.take(2)
        val text = teile.joinToString(" ") { t -> if (t == titel && teile.size > 1 && t.last() !in ".!?:") "$t." else t }
        return if (text.length > 220) text.take(217).trimEnd() + " …" else text.ifBlank { "Erinnerung" }
    }
}

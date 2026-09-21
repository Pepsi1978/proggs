package de.frank.wecker

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model: WeckerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideStatusBar()
        setContent {
            val ausrichtung by model.settings.ausrichtungFlow.collectAsStateWithLifecycle()
            AusrichtungsSperre(ausrichtung) { WeckerApp(model, this) }
        }
        // Die gewählte Ausrichtung folgt der Einstellung; nur eine echte Abweichung wird gesetzt,
        // damit daraus keine Kette aus Neukonfigurationen entsteht.
        lifecycleScope.launch {
            model.settings.ausrichtungFlow.collect { Ausrichtung.anwenden(this@MainActivity, it) }
        }
        lifecycleScope.launch {
            AlarmService.state.collect { state ->
                if (state.alarm != null && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                    startActivity(Intent(this@MainActivity, AlarmActivity::class.java))
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // Returning from system notification settings or after a long pause: replan the sleep reminders.
        SchlafErinnerung.syncAll(this)
        if (AlarmService.state.value.alarm != null) startActivity(Intent(this, AlarmActivity::class.java))
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }
    override fun onStop() {
        if (model.recording.value) model.stopRecording()
        model.stopPreview()
        super.onStop()
    }
}

internal fun ComponentActivity.hideStatusBar() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.statusBars())
    }
}

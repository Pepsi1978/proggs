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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val model: WeckerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WeckerApp(model, this) }
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
        if (AlarmService.state.value.alarm != null) startActivity(Intent(this, AlarmActivity::class.java))
    }
    override fun onStop() {
        if (model.recording.value) model.stopRecording()
        model.stopPreview()
        super.onStop()
    }
}

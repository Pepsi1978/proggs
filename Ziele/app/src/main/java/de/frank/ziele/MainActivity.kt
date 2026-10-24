package de.frank.ziele

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import de.frank.ziele.ui.AppViewModel
import de.frank.ziele.ui.ZieleApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private val mikroErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        vm.mikrofonErlaubnisErgebnis(ok)
    }

    private val meldungsErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        vm.mikrofonAnfragen = {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                vm.mikrofonErlaubnisErgebnis(true)
            } else {
                mikroErlaubnis.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        // Ohne diese Erlaubnis meldet sich eine im Hintergrund fertige Auswertung nicht (Android 13+).
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) meldungsErlaubnis.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { ZieleApp(vm, this) }
    }

    override fun onResume() {
        super.onResume()
        vm.wiederDa()
    }
}

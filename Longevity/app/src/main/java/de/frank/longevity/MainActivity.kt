package de.frank.longevity

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
import de.frank.longevity.ui.AppViewModel
import de.frank.longevity.ui.LongevityApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private val mikroErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        vm.mikrofonErlaubnisErgebnis(ok)
    }
    private val hinweisErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        vm.hinweiseAnfragen = {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) hinweisErlaubnis.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { LongevityApp(vm, this) }
    }

    override fun onResume() {
        super.onResume()
        vm.pruefen()
    }
}

package de.frank.aufgaben

import android.Manifest
import android.content.Intent
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
import de.frank.aufgaben.ui.AppViewModel
import de.frank.aufgaben.ui.AufgabenApp

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
        if (savedInstanceState == null) verarbeite(intent)
        setContent { AufgabenApp(vm, this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        verarbeite(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.tagPruefen()
    }

    private fun verarbeite(intent: Intent?) {
        intent ?: return
        val id = intent.getLongExtra(EXTRA_AUFGABE, -1)
        when {
            intent.getBooleanExtra(EXTRA_NEU, false) -> vm.neueAufgabe(mitMikro = true)
            id >= 0 -> vm.oeffne(id)
        }
    }

    companion object {
        const val EXTRA_AUFGABE = "aufgabe_id"
        const val EXTRA_NEU = "neu"
    }
}

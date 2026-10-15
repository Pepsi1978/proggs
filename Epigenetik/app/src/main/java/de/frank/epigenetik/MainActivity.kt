package de.frank.epigenetik

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import de.frank.epigenetik.ui.AppViewModel
import de.frank.epigenetik.ui.EpigenetikApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private val mikroErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        vm.mikrofonErlaubnisErgebnis(ok)
    }
    private val hinweisErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val sprache = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { ergebnis ->
        if (ergebnis.resultCode == Activity.RESULT_OK) {
            vm.handyDiktatErgebnis(ergebnis.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull())
        }
    }

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
        vm.sprachIntentStarten = { sprache.launch(it) }
        oeffneAusMeldung(intent)
        setContent { EpigenetikApp(vm, this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        oeffneAusMeldung(intent)
    }

    /** Tipp auf „… ist fertig“ öffnet direkt die Auswertung. */
    private fun oeffneAusMeldung(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L
        if (id > 0) { vm.oeffne(id); intent?.removeExtra(EXTRA_ID) }
    }

    override fun onResume() {
        super.onResume()
        vm.pruefen()
    }

    companion object {
        const val EXTRA_ID = "recherche_id"
    }
}

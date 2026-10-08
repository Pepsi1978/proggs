package de.frank.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.ui.AppViewModel
import de.frank.jarvis.ui.JarvisApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    private val hinweisErlaubnis = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.lagePruefen()
        JarvisDienst.abgleichen(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        vm.hinweiseAnfragen = {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                hinweisErlaubnis.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (savedInstanceState == null) vm.hinweiseAnfragen()
        JarvisDienst.abgleichen(this)
        setContent { JarvisApp(vm, this) }
    }

    override fun onResume() {
        super.onResume()
        vm.lagePruefen()
    }
}

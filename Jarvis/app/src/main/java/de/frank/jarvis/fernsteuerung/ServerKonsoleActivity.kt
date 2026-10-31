package de.frank.jarvis.fernsteuerung

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.Intent
import android.widget.Toast

/**
 * Die Konsole des Hostinger-Servers (OpenLauncherServer/web): Sitzungen, laufende CLI, OpenLauncher, System. Nur im WireGuard-Tunnel erreichbar.
 * Anmeldung mit demselben Zugang wie die Fernsteuerung; HTTP ist nur für 10.8.0.1 erlaubt (res/xml/netz_regeln.xml), der Tunnel verschlüsselt.
 * Adressen außerhalb des Tunnels (Anmeldeseiten der Anbieter) öffnet der Browser des Handys; ohne Mehrfenster-Unterstützung kommt auch
 * target=_blank bei shouldOverrideUrlLoading an.
 */
class ServerKonsoleActivity : ComponentActivity() {
    private lateinit var ansicht: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val zugang = MacSteuerung.token(this)
        ansicht = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#10131A"))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.host == Fernziel.SERVER.adresse) return false
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                    return true
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) Toast.makeText(this@ServerKonsoleActivity, "Server-Konsole nicht erreichbar: ${error.description}", Toast.LENGTH_LONG).show()
                }
            }
        }
        // Vollbild ohne Statusleiste: Sie lag sonst über den Reitern und schluckte jeden Tipper dort. Der Rahmen hält die Seite frei von
        // Kamera-Aussparung, App-Griff (Fold, oben Mitte) und Tastatur; wegen des Vollbilds übernimmt das hier nicht adjustResize.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rahmen = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#10131A"))
            addView(ansicht)
        }
        ViewCompat.setOnApplyWindowInsetsListener(rahmen) { v, insets ->
            val r = insets.getInsets(WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.captionBar() or WindowInsetsCompat.Type.ime())
            v.setPadding(r.left, r.top, r.right, r.bottom)
            WindowInsetsCompat.CONSUMED
        }
        setContentView(rahmen)
        leistenAusblenden()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        // POST statt Adresse: So steht der Zugang in keinem Verlauf; danach gilt ein davon abgeleitetes Cookie.
        // Mit gültigem Cookie direkt zur Seite: spart einen Weg über den Tunnel.
        val adresse = "http://${Fernziel.SERVER.adresse}:${Fernziel.SERVER.port}"
        if (CookieManager.getInstance().getCookie(adresse)?.contains("konsole=") == true) ansicht.loadUrl("$adresse/")
        else ansicht.postUrl("$adresse/anmelden", ("token=" + Uri.encode(zugang)).toByteArray())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Nach dem Herunterwischen oder der Tastatur wieder ausblenden.
        if (hasFocus) leistenAusblenden()
    }

    private fun leistenAusblenden() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }

    override fun onDestroy() {
        ansicht.destroy()
        super.onDestroy()
    }
}

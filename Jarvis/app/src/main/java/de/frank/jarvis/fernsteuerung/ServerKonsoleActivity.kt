package de.frank.jarvis.fernsteuerung

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import android.content.Intent
import android.widget.Toast

/**
 * Die Konsole des Hostinger-Servers (OpenLauncherServer/web): Sitzungen, laufende CLI, OpenLauncher, System. Nur im WireGuard-Tunnel erreichbar.
 * Anmeldung mit demselben Zugang wie die Fernsteuerung; HTTP ist nur für 10.8.0.1 erlaubt (res/xml/netz_regeln.xml), der Tunnel verschlüsselt.
 * Adressen außerhalb des Tunnels (Anmeldeseiten der Anbieter) öffnet der Browser des Handys.
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
            // target=_blank (Anmelde-Adressen in der Konsole) kommt hier an und geht ebenfalls in den Browser.
            settings.setSupportMultipleWindows(true)
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
                    val adresse = view.hitTestResult.extra ?: return false
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(adresse))) }
                    return false
                }
            }
        }
        setContentView(ansicht)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        ansicht.loadUrl("http://${Fernziel.SERVER.adresse}:${Fernziel.SERVER.port}/?token=" + Uri.encode(zugang))
    }

    override fun onDestroy() {
        ansicht.destroy()
        super.onDestroy()
    }
}

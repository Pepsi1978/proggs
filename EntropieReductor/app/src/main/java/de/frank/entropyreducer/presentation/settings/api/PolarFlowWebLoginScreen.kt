package de.frank.entropyreducer.presentation.settings.api

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * JavaScript-Interface fuer Polar-Workout-Daten. Wird im WebView injiziert
 * und vom JS-Fetch aus aufgerufen.
 */
class PolarFlowJsBridge(
    private val onWorkoutJson: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    @JavascriptInterface
    fun receiveJson(json: String) {
        Log.i("PolarFlowJsBridge", "JS-fetch lieferte JSON-Daten (${json.length} bytes), preview=${json.take(300)}")
        if (json.isNotBlank()) onWorkoutJson(json)
    }

    @JavascriptInterface
    fun receiveError(msg: String) {
        Log.w("PolarFlowJsBridge", "JS-fetch Fehler: $msg")
        onError(msg)
    }
}

/**
 * Polar Flow Login via eingebettetem WebView.
 *
 * Hintergrund: Polar's Login nutzt OAuth2-SSO via auth.polar.com mit
 * mehreren Redirects. HTTP-only-Implementation scheitert daran (Captcha,
 * CSRF, JavaScript-Rendering). Ein WebView umgeht das — Frank loggt sich
 * im Browser-View ein, Android's CookieManager faengt die Polar-Cookies
 * automatisch ab.
 *
 * Nach erfolgreichem Login redirected Polar auf flow.polar.com/. Sobald
 * die URL diese Domain erreicht UND der Cookie-Manager einen Session-
 * Cookie enthaelt: rufen wir onLoginSuccess(cookieHeader) auf.
 *
 * @param onLoginSuccess Callback mit dem Cookie-Header-String. Aufrufer
 *                       persistiert ihn in EncryptedSecretsStore.polarFlowCookieJar
 * @param onDismiss      User hat den Login-Screen verlassen ohne Login.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PolarFlowWebLoginScreen(
    onLoginSuccess: (cookieHeader: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var progress by remember { mutableStateOf(0) }
    var loginDone by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Polar Flow Login") },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        // CookieManager fuer flow.polar.com aktivieren
                        CookieManager.getInstance().setAcceptCookie(true)
                        WebView(ctx).apply {
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                userAgentString = (settings.userAgentString
                                    ?: "Mozilla/5.0 (Linux; Android 14)")
                                    .replace("; wv", "") // kein WebView-Tag
                                cacheMode = WebSettings.LOAD_DEFAULT
                                builtInZoomControls = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    val u = request.url.toString()
                                    Log.d(TAG, "WebView nav: $u")
                                    return false // immer im WebView laden
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    super.onPageFinished(view, url)
                                    Log.d(TAG, "WebView pageFinished: $url")
                                    // Erfolg: Dashboard oder eine flow.polar.com-Seite
                                    // OHNE /login im Pfad. Polar redirected nach Login
                                    // auf flow.polar.com/ (Dashboard) oder
                                    // flow.polar.com/training oder /sportprofiles.
                                    if (loginDone) return
                                    val isFlowDomain = url.contains("flow.polar.com")
                                    val isLoginPath = url.contains("/login") ||
                                        url.contains("auth.polar.com") ||
                                        url.contains("/flowSso/")
                                    if (isFlowDomain && !isLoginPath) {
                                        // WICHTIG: flush() persistiert die WebView-Cookies
                                        // damit sie nach App-Restart noch da sind.
                                        CookieManager.getInstance().flush()
                                        val cookieHeader = CookieManager.getInstance()
                                            .getCookie("https://flow.polar.com/")
                                            .orEmpty()
                                        Log.i(TAG, "PolarFlow Login erfolgreich — Cookie-Laenge=${cookieHeader.length}")
                                        if (cookieHeader.isNotBlank()) {
                                            loginDone = true
                                            onLoginSuccess(cookieHeader)
                                        }
                                    }
                                }
                            }
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }
                            }
                            loadUrl("https://flow.polar.com/login")
                        }
                    },
                )
                if (progress in 0..0) {
                    Text(
                        "Lade Polar-Login…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private const val TAG = "PolarFlowLogin"

/**
 * Workout-Loader-Screen: oeffnet WebView auf Polar's Workout-Detail-Seite
 * und fuehrt JavaScript-fetch zu /api/training/{id} aus. Da der WebView
 * voll authentifiziert ist (alle Cookies inkl. HttpOnly), bekommt der
 * fetch echte JSON-Daten zurueck.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun PolarFlowWorkoutLoaderScreen(
    exerciseId: Long,
    onJsonReceived: (String) -> Unit,
    onError: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var status by remember { mutableStateOf("Lade Polar-Seite…") }
    BackHandler { onDismiss() }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Polar Workout $exerciseId laden") })
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(status, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    CookieManager.getInstance().setAcceptCookie(true)
                    WebView(ctx).apply {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            userAgentString = (settings.userAgentString
                                ?: "Mozilla/5.0 (Linux; Android 14)")
                                .replace("; wv", "")
                        }
                        addJavascriptInterface(
                            PolarFlowJsBridge(
                                onWorkoutJson = { json ->
                                    post {
                                        status = "Workout-Daten empfangen (${json.length} bytes)"
                                        onJsonReceived(json)
                                    }
                                },
                                onError = { msg ->
                                    post {
                                        status = "Fehler: $msg"
                                        onError(msg)
                                    }
                                },
                            ),
                            "Android",
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                Log.i(TAG, "WorkoutLoader: pageFinished $url")
                                if (url.contains("/training/analysis/$exerciseId") ||
                                    url.contains("/training/$exerciseId")) {
                                    status = "Seite geladen — fetch wird ausgefuehrt…"
                                    // JS-fetch ausfuehren — der hat alle Cookies + Browser-Kontext
                                    val js = """
                                        (function(){
                                            const urls = [
                                                '/api/training/$exerciseId',
                                                '/api/training-sessions/$exerciseId',
                                                '/api/exercise/$exerciseId',
                                                '/api/export/training/tcx/$exerciseId',
                                                '/api/export/training/gpx/$exerciseId'
                                            ];
                                            (async () => {
                                                for (const u of urls) {
                                                    try {
                                                        const r = await fetch(u, {credentials:'include',headers:{'Accept':'application/json,application/xml,*/*'}});
                                                        const text = await r.text();
                                                        console.log('PolarFetch ' + u + ' HTTP ' + r.status + ' bytes=' + text.length);
                                                        if (r.ok && text.length > 100) {
                                                            const trimmed = text.trim();
                                                            if (trimmed.startsWith('{') || trimmed.startsWith('[') || trimmed.startsWith('<?xml') || trimmed.startsWith('<TrainingCenterDatabase') || trimmed.startsWith('<gpx')) {
                                                                Android.receiveJson(text);
                                                                return;
                                                            }
                                                        }
                                                    } catch(e) {
                                                        console.error('Polar fetch ' + u + ' Fehler: ' + e.message);
                                                    }
                                                }
                                                Android.receiveError('Kein Endpoint lieferte verwertbare Daten');
                                            })();
                                        })();
                                    """.trimIndent()
                                    view.evaluateJavascript(js, null)
                                }
                            }
                        }
                        loadUrl("https://flow.polar.com/training/analysis/$exerciseId")
                    }
                },
            )
        }
    }
}

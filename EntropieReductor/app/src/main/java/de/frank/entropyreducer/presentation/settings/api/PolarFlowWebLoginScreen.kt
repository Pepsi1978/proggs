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
                                // Wenn auf Diary: Workout-Links extrahieren und an
                                // Android-Bridge schicken, damit Frank den richtigen
                                // Workout sieht.
                                if (url.contains("/diary")) {
                                    status = "Diary geladen — suche nach Workouts der letzten 7 Tage…"
                                    val diaryJs = """
                                        (function(){
                                            try {
                                                const links = document.querySelectorAll('a[href*="/training/"]');
                                                console.log('PolarDiary found ' + links.length + ' workout links');
                                                const seen = new Set();
                                                let count = 0;
                                                links.forEach(a => {
                                                    if (count >= 20) return;
                                                    const href = a.getAttribute('href');
                                                    if (!href || seen.has(href)) return;
                                                    seen.add(href);
                                                    const text = (a.textContent || '').replace(/\s+/g,' ').trim().substring(0,80);
                                                    console.log('PolarDiary link[' + count + '] ' + href + ' "' + text + '"');
                                                    count++;
                                                });
                                                // Auto-klick auf den ERSTEN Trail/Lauf-Link
                                                for (const a of links) {
                                                    const text = (a.textContent || '').toLowerCase();
                                                    if (text.indexOf('lauf') >= 0 || text.indexOf('run') >= 0 || text.indexOf('trail') >= 0) {
                                                        console.log('PolarDiary auto-click on: ' + a.getAttribute('href'));
                                                        a.click();
                                                        return;
                                                    }
                                                }
                                                // Wenn nichts gefunden: ersten Workout-Link klicken
                                                if (links.length > 0) {
                                                    console.log('PolarDiary auto-click on first link');
                                                    links[0].click();
                                                }
                                            } catch(e) { console.log('PolarDiary err: ' + e.message); }
                                        })();
                                    """.trimIndent()
                                    view.evaluateJavascript(diaryJs, null)
                                    return
                                }
                                // Beim Login-Redirect direkt zur Diary navigieren
                                if (url.contains("flow.polar.com") && !url.contains("/training/")
                                    && !url.contains("/diary")
                                    && !url.contains("/login") && !url.contains("auth.polar.com")) {
                                    status = "Eingeloggt — navigiere zu Diary…"
                                    view.loadUrl("https://flow.polar.com/diary")
                                    return
                                }
                                if (url.contains("/training/")) {
                                    status = "Workout-Seite geladen — Polar wird durchsucht…"
                                    // PASSIVE Interception: Polar's eigene App macht XHR-
                                    // Calls. Wir interceptieren window.fetch + XMLHttpRequest
                                    // und melden alle Trainings-Daten-relevante Responses
                                    // an Android. So muessen wir NICHT raten welche URL.
                                    val js = """
                                        (function(){
                                            let found = null;

                                            // Hilfsfunktion: ist das ein echter Training-Datensatz?
                                            function isTrainingData(text, url) {
                                                if (url && url.indexOf('consentcdn') >= 0) return false;
                                                if (url && url.indexOf('cookiebot') >= 0) return false;
                                                if (url && url.indexOf('googletagmanager') >= 0) return false;
                                                if (text.length < 1000) return false;
                                                // Muss mindestens 2 Training-Felder enthalten
                                                let hits = 0;
                                                if (text.indexOf('"$exerciseId"') >= 0) hits += 2;
                                                if (text.indexOf('"exerciseId"') >= 0) hits += 1;
                                                if (text.indexOf('"samples"') >= 0) hits += 1;
                                                if (text.indexOf('"heartRate"') >= 0) hits += 1;
                                                if (text.indexOf('"speed"') >= 0) hits += 1;
                                                if (text.indexOf('"altitude"') >= 0) hits += 1;
                                                if (text.indexOf('"latitude"') >= 0) hits += 1;
                                                if (text.indexOf('"longitude"') >= 0) hits += 1;
                                                if (text.indexOf('"sport"') >= 0) hits += 1;
                                                if (text.indexOf('"trainingSession"') >= 0) hits += 2;
                                                if (text.indexOf('"distanceKilometers"') >= 0) hits += 2;
                                                if (text.indexOf('"distance"') >= 0 && text.indexOf('"duration"') >= 0) hits += 1;
                                                return hits >= 2;
                                            }

                                            // 1) __INITIAL_STATE__ direkt pruefen
                                            try {
                                                const cands = [window.__INITIAL_STATE__, window.__PRELOADED_STATE__, window.__APOLLO_STATE__, window.__NEXT_DATA__, window.__NUXT__];
                                                for (const c of cands) {
                                                    if (c) {
                                                        const s = JSON.stringify(c);
                                                        if (isTrainingData(s, '__INITIAL_STATE__')) {
                                                            console.log('PolarInit INITIAL_STATE found len=' + s.length);
                                                            Android.receiveJson(s);
                                                            return;
                                                        }
                                                    }
                                                }
                                            } catch(e){}

                                            // 2) Polar's eigene App's Endpoints abklopfen — VIELE Pfad-Varianten
                                            (async () => {
                                                const probes = [
                                                    '/api/training/analysis/$exerciseId',
                                                    '/api/training/analysis/$exerciseId/summary',
                                                    '/api/training/analysis/$exerciseId/details',
                                                    '/api/training/analysis/$exerciseId/samples',
                                                    '/api/training/analysis/$exerciseId/route',
                                                    '/api/training/$exerciseId',
                                                    '/api/training/$exerciseId/full',
                                                    '/api/exercises/$exerciseId',
                                                    '/api/exercises/$exerciseId/full',
                                                    '/api/exercises/$exerciseId/data',
                                                    '/api/exercises/$exerciseId/samples',
                                                    '/api/exercises/$exerciseId/route',
                                                    '/api/sport-activities/$exerciseId',
                                                    '/api/sport-activities/$exerciseId/data',
                                                    '/api/training-session/$exerciseId',
                                                    '/api/training-sessions/$exerciseId',
                                                    '/api/training-sessions/$exerciseId/full',
                                                    '/api/training-sessions/$exerciseId/details',
                                                    '/training/api/sample/heartRate/$exerciseId',
                                                    '/training/api/sample/speed/$exerciseId',
                                                    '/training/api/sample/altitude/$exerciseId',
                                                    '/training/api/sample/distance/$exerciseId',
                                                    '/training/api/sample/cadence/$exerciseId',
                                                    '/api/training/route/$exerciseId',
                                                    '/api/training/$exerciseId/route',
                                                    '/api/training/sample/$exerciseId',
                                                    '/api/training/route?id=$exerciseId',
                                                    '/api/training/analyseTab/$exerciseId',
                                                    '/api/analyseTab/$exerciseId',
                                                ];
                                                for (const u of probes) {
                                                    try {
                                                        const r = await fetch(u, {credentials:'include',headers:{'Accept':'application/json,*/*'}});
                                                        const text = await r.text();
                                                        const t = text.trim();
                                                        const isJson = t.startsWith('{') || t.startsWith('[');
                                                        console.log('PolarProbe ' + u + ' HTTP ' + r.status + ' bytes=' + text.length + ' isJson=' + isJson);
                                                        if (r.ok && isJson && text.length > 200) {
                                                            // Heuristik: Workout-Daten?
                                                            if (text.indexOf('"heartRate"') >= 0 || text.indexOf('"samples"') >= 0 ||
                                                                text.indexOf('"distance"') >= 0 || text.indexOf('"sport"') >= 0 ||
                                                                text.indexOf('"latitude"') >= 0 || text.indexOf('"altitude"') >= 0) {
                                                                console.log('PolarProbe ' + u + ' = MATCH! Sending to Android');
                                                                Android.receiveJson(text);
                                                                return;
                                                            }
                                                        }
                                                    } catch(e) {
                                                        console.log('PolarProbe ' + u + ' EXCEPTION: ' + e.message);
                                                    }
                                                }
                                                console.log('PolarProbe ALL endpoints scanned, no match');
                                            })();

                                            const origFetch = window.fetch;
                                            window.fetch = async function(...args) {
                                                const u = (typeof args[0] === 'string') ? args[0] : (args[0] && args[0].url) || '';
                                                let r;
                                                try { r = await origFetch.apply(this, args); } catch(e) { throw e; }
                                                try {
                                                    const text = await r.clone().text();
                                                    const ok = r.ok && isTrainingData(text, u);
                                                    console.log('PolarFetch ' + u + ' HTTP ' + r.status + ' bytes=' + text.length + ' isData=' + ok);
                                                    if (ok && !found) {
                                                        found = text;
                                                        Android.receiveJson(text);
                                                    }
                                                } catch(e){}
                                                return r;
                                            };
                                            const proto = window.XMLHttpRequest.prototype;
                                            const origOpen = proto.open;
                                            const origSend = proto.send;
                                            proto.open = function(m, u) { this._purl = u; this._pmth = m; return origOpen.apply(this, arguments); };
                                            proto.send = function() {
                                                const xhr = this;
                                                xhr.addEventListener('load', function(){
                                                    try {
                                                        const text = xhr.responseText || '';
                                                        const ok = xhr.status >= 200 && xhr.status < 300 && isTrainingData(text, xhr._purl);
                                                        console.log('PolarXHR ' + xhr._pmth + ' ' + xhr._purl + ' HTTP ' + xhr.status + ' bytes=' + text.length + ' isData=' + ok);
                                                        if (ok && !found) {
                                                            found = text;
                                                            Android.receiveJson(text);
                                                        }
                                                    } catch(e){}
                                                });
                                                return origSend.apply(this, arguments);
                                            };

                                            // Periodisch __INITIAL_STATE__ und scripts nochmal pruefen
                                            // (Polar's React-App schreibt evtl. spaeter rein)
                                            const recheckInterval = setInterval(function(){
                                                if (found) { clearInterval(recheckInterval); return; }
                                                try {
                                                    const cands = [window.__INITIAL_STATE__, window.__PRELOADED_STATE__, window.__APOLLO_STATE__];
                                                    for (const c of cands) {
                                                        if (c) {
                                                            const s = JSON.stringify(c);
                                                            if (isTrainingData(s, '__INITIAL_STATE__')) {
                                                                console.log('PolarPeriodic INITIAL_STATE found len=' + s.length);
                                                                found = s;
                                                                Android.receiveJson(s);
                                                                clearInterval(recheckInterval);
                                                                return;
                                                            }
                                                        }
                                                    }
                                                } catch(e){}
                                            }, 1000);

                                            setTimeout(function(){
                                                clearInterval(recheckInterval);
                                                if (!found) Android.receiveError('Polar hat 30s keine Workout-Daten ausgespuckt. Vielleicht klicke im WebView auf das Workout — das triggert die AJAX-Calls.');
                                            }, 30000);
                                        })();
                                    """.trimIndent()
                                    view.evaluateJavascript(js, null)
                                }
                            }
                        }
                        // 2026-05-17: Polar Flow Web's `/training/analysis/{id}`
                        // gibt 404 zurueck obwohl AccessLink V3 die ID liefert.
                        // Polar's Web-System nutzt andere IDs. Wir laden
                        // stattdessen Polar's Diary — Frank sieht dort seine
                        // ECHTEN Workouts mit den Polar-Flow-IDs.
                        // Tipp aus dem WebView: Frank klickt auf sein 17:22-Workout
                        // und die URL zeigt die echte ID.
                        loadUrl("https://flow.polar.com/diary")
                    }
                },
            )
        }
    }
}

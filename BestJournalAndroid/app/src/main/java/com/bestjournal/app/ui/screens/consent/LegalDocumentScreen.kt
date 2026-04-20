package com.bestjournal.app.ui.screens.consent

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

// Matches the SplashScreen palette for visual consistency
private val LegalBg = Color(0xFF131313)
private val LegalAccent = Color(0xFFFFB689)
private val LegalOnSurface = Color(0xFFE5E2E1)

enum class LegalDocument(val fileName: String, val titleRes: Int) {
    Datenschutz("DATENSCHUTZ.html", com.bestjournal.app.R.string.legal_title_datenschutz),
    Nutzungsbedingungen(
        "NUTZUNGSBEDINGUNGEN.html",
        com.bestjournal.app.R.string.legal_title_nutzungsbedingungen,
    ),
    Impressum("IMPRESSUM.html", com.bestjournal.app.R.string.legal_title_impressum),
}

@Composable
fun LegalDocumentScreen(document: LegalDocument, onBack: () -> Unit) {
    val title = androidx.compose.ui.res.stringResource(document.titleRes)

    Box(modifier = Modifier.fillMaxSize().background(LegalBg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top bar
            Box(modifier = Modifier.fillMaxSize().weight(0f).height(56.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Zurück",
                        tint = LegalAccent,
                    )
                }
                Text(
                    text = title,
                    color = LegalOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            // WebView with bundled HTML asset
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(0xFF131313.toInt())
                        settings.javaScriptEnabled = false
                        settings.defaultTextEncodingName = "utf-8"
                        // External links open in the system browser
                        webViewClient =
                            object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url ?: return false
                                    val intent = Intent(Intent.ACTION_VIEW, url)
                                    ctx.startActivity(intent)
                                    return true
                                }
                            }
                        loadUrl("file:///android_asset/legal/${document.fileName}")
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// Helper for ScreenNav routing
fun legalDocumentFromRoute(route: String?): LegalDocument? =
    when (route) {
        "legal/datenschutz" -> LegalDocument.Datenschutz
        "legal/nutzungsbedingungen" -> LegalDocument.Nutzungsbedingungen
        "legal/impressum" -> LegalDocument.Impressum
        else -> null
    }

// Open system browser fallback if WebView fails
fun openInBrowser(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        // no-op
    }
}

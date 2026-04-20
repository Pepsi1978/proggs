package com.bestjournal.app.ui.screens.consent

import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

// Matches the SplashScreen palette for visual consistency
private val LegalBg = Color(0xFF131313)
private val LegalAccent = Color(0xFFFFB689)
private val LegalOnSurface = Color(0xFFE5E2E1)

enum class LegalDocument(
    val deFileName: String,
    val enFileName: String,
    val titleRes: Int,
) {
    Datenschutz(
        "DATENSCHUTZ.html",
        "PRIVACY.html",
        com.bestjournal.app.R.string.legal_title_datenschutz,
    ),
    Nutzungsbedingungen(
        "NUTZUNGSBEDINGUNGEN.html",
        "TERMS.html",
        com.bestjournal.app.R.string.legal_title_nutzungsbedingungen,
    ),
    Impressum(
        "IMPRESSUM.html",
        "IMPRINT.html",
        com.bestjournal.app.R.string.legal_title_impressum,
    );

    /**
     * Picks the locale-appropriate asset path. German is the only non-English
     * locale with dedicated legal translations, for every other system language
     * we fall back to the English set which covers CCPA, UK GDPR, Quebec, APPs etc.
     */
    fun assetPath(): String {
        val lang = Locale.getDefault().language
        return if (lang == "de") {
            "legal/de/$deFileName"
        } else {
            "legal/en/$enFileName"
        }
    }
}

@Composable
fun LegalDocumentScreen(document: LegalDocument, onBack: () -> Unit) {
    val title = stringResource(document.titleRes)

    Column(modifier = Modifier.fillMaxSize().background(LegalBg).statusBarsPadding()) {
        // Top bar (Row, not weight-misused Box)
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(com.bestjournal.app.R.string.action_back),
                    tint = LegalAccent,
                )
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    color = LegalOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )
            }
            // Balance the back-button so the title stays centered
            Spacer(Modifier.width(48.dp))
        }

        // WebView fills remaining space
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(0xFFFFFFFF.toInt())
                    settings.javaScriptEnabled = false
                    settings.defaultTextEncodingName = "utf-8"
                    settings.useWideViewPort = false
                    settings.loadWithOverviewMode = false
                    // External links (mailto, https) open in system browser
                    webViewClient =
                        object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                val url = request?.url ?: return false
                                if (url.scheme == "file") return false
                                return try {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW, url).addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                    )
                                    true
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }
                    loadUrl("file:///android_asset/${document.assetPath()}")
                }
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

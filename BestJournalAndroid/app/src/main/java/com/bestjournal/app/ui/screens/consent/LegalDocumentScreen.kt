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
     * Picks the locale-appropriate asset path.
     *
     * Three tiers of legal coverage:
     *  1. German (de) — full legally binding versions (DATENSCHUTZ / NUTZUNGSBEDINGUNGEN / IMPRESSUM).
     *  2. English (en) — full legally binding versions covering CCPA, UK GDPR, APPs, LGPD, PIPL, etc.
     *  3. Other supported UI languages — three localized *summaries* (PRIVACY.html /
     *     TERMS.html / IMPRINT.html in the language-specific folder). Each summary links
     *     prominently to its corresponding full English and German version at the top.
     *     This satisfies GDPR Art. 12 transparency ("clear and plain language the data
     *     subject understands") while keeping the legally binding text in the
     *     professionally drafted EN/DE originals.
     *  4. Any other language — falls back to the English full versions.
     */
    fun assetPath(): String {
        val locale = Locale.getDefault()
        val lang = locale.language
        val country = locale.country

        // Tier 1: German — separate full documents
        if (lang == "de") return "legal/de/$deFileName"

        // Tier 2: English — separate full documents
        if (lang == "en") return "legal/en/$enFileName"

        // Tier 3: Other supported UI languages — three separate localized summaries per folder,
        // filenames identical to the English versions (PRIVACY.html / TERMS.html / IMPRINT.html)
        val summaryFolder = summaryFolderFor(lang, country)
        if (summaryFolder != null) return "legal/$summaryFolder/$enFileName"

        // Tier 4: Fallback to English full documents
        return "legal/en/$enFileName"
    }

    companion object {
        /**
         * Maps an Android locale (language + country) to the asset folder that contains
         * its LEGAL_SUMMARY.html. Returns null when no localized summary exists — the caller
         * then falls back to the English full version.
         *
         * Only languages that already ship a LEGAL_SUMMARY.html may appear here, otherwise
         * users would see an empty WebView. Extend this map together with adding the asset.
         */
        internal fun summaryFolderFor(lang: String, country: String): String? =
            when (lang) {
                // Variant-sensitive languages first (country matters)
                "pt" -> when (country) {
                    "BR" -> "pt-BR".onlyIfTranslated()
                    "PT" -> "pt-PT".onlyIfTranslated()
                    else -> "pt-PT".onlyIfTranslated() // European Portuguese as default for pt-*
                }
                "zh" -> when (country) {
                    "CN", "SG" -> "zh-CN".onlyIfTranslated()
                    "TW", "HK", "MO" -> "zh-TW".onlyIfTranslated()
                    else -> "zh-CN".onlyIfTranslated()
                }
                // "in" is Android's legacy code for Indonesian (ISO 639-1 changed to "id" in 1989
                // but Java/Android kept the old code for backwards compatibility).
                "in", "id" -> "id".onlyIfTranslated()
                // Simple language-only matches
                "fr" -> "fr".onlyIfTranslated()
                "es" -> "es".onlyIfTranslated()
                "it" -> "it".onlyIfTranslated()
                "nl" -> "nl".onlyIfTranslated()
                "pl" -> "pl".onlyIfTranslated()
                "uk" -> "uk".onlyIfTranslated()
                "tr" -> "tr".onlyIfTranslated()
                "ja" -> "ja".onlyIfTranslated()
                "ko" -> "ko".onlyIfTranslated()
                "ar" -> "ar".onlyIfTranslated()
                "hi" -> "hi".onlyIfTranslated()
                "th" -> "th".onlyIfTranslated()
                "bn" -> "bn".onlyIfTranslated()
                "te" -> "te".onlyIfTranslated()
                "mr" -> "mr".onlyIfTranslated()
                "ta" -> "ta".onlyIfTranslated()
                "ur" -> "ur".onlyIfTranslated()
                "gu" -> "gu".onlyIfTranslated()
                "kn" -> "kn".onlyIfTranslated()
                "ml" -> "ml".onlyIfTranslated()
                else -> null
            }

        /**
         * Gate for languages whose LEGAL_SUMMARY.html has actually been created in assets.
         * Add the folder name here the moment its asset is committed; until then the caller
         * falls back to the English full versions, so users never see a blank WebView.
         */
        private val TRANSLATED_SUMMARIES = setOf(
            "fr",
            "es",
            "pt-BR",
            "pt-PT",
            "it",
            "nl",
            "pl",
            "uk",
            "tr",
            "ja",
            "ko",
            "zh-CN",
            "zh-TW",
            "ar",
            "hi",
            "th",
            "id",
            "bn",
            "te",
            "mr",
            "ta",
            "ur",
            "gu",
            "kn",
            "ml",
        )

        private fun String.onlyIfTranslated(): String? =
            if (this in TRANSLATED_SUMMARIES) this else null
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

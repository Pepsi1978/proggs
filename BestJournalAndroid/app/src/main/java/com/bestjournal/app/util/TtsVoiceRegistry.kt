package com.bestjournal.app.util

import java.util.Locale

/**
 * Complete Edge TTS voice registry for international use. Auto-detects the device language and
 * provides matching voices. Multilingual voices (best quality) are listed first per locale.
 */
object TtsVoiceRegistry {

    enum class Gender {
        FEMALE,
        MALE,
    }

    enum class Tier {
        MULTILINGUAL,
        STANDARD,
    }

    data class Voice(
        val id: String,
        val name: String,
        val gender: Gender,
        val tier: Tier = Tier.STANDARD,
    )

    data class LocaleVoices(
        val localeCode: String,
        val defaultVoiceId: String,
        val voices: List<Voice>,
    )

    /** Get the locale code from a voice ID: "de-DE-KatjaNeural" -> "de-DE" */
    fun extractLocale(voiceId: String): String {
        val parts = voiceId.split("-")
        return if (parts.size >= 2) "${parts[0]}-${parts[1]}" else "en-US"
    }

    /** Find the best matching locale config for the device language. */
    fun getLocaleVoices(): LocaleVoices {
        val locale = Locale.getDefault()
        val lang = locale.language.lowercase()
        val country = locale.country.uppercase()

        // Try exact match first (e.g., "pt-BR")
        if (country.isNotEmpty()) {
            ALL_LOCALES["$lang-$country"]?.let {
                return it
            }
        }

        // Try language-only match (e.g., "pt" -> "pt-BR")
        LANGUAGE_FALLBACK[lang]?.let { code ->
            ALL_LOCALES[code]?.let {
                return it
            }
        }

        // Default to English US (fallback if ALL_LOCALES map is incomplete, e.g. after R8 strip)
        return ALL_LOCALES["en-US"]
            ?: LocaleVoices("en-US", "en-US-AvaMultilingualNeural", emptyList())
    }

    /** Get gender label in the voice's own language. */
    fun genderLabel(localeCode: String, gender: Gender): String {
        val lang = if (localeCode.length >= 2) localeCode.substring(0, 2) else "en"
        return when (lang) {
            "de" -> if (gender == Gender.FEMALE) "weiblich" else "männlich"
            "en" -> if (gender == Gender.FEMALE) "female" else "male"
            "es" -> if (gender == Gender.FEMALE) "femenina" else "masculino"
            "fr" -> if (gender == Gender.FEMALE) "féminine" else "masculin"
            "it" -> if (gender == Gender.FEMALE) "femminile" else "maschile"
            "pt" -> if (gender == Gender.FEMALE) "feminina" else "masculino"
            "nl" -> if (gender == Gender.FEMALE) "vrouwelijk" else "mannelijk"
            "pl" -> if (gender == Gender.FEMALE) "kobieta" else "m\u0119\u017cczyzna"
            "ru" ->
                if (gender == Gender.FEMALE) "\u0436\u0435\u043d\u0441\u043a\u0438\u0439"
                else "\u043c\u0443\u0436\u0441\u043a\u043e\u0439"
            "tr" -> if (gender == Gender.FEMALE) "kad\u0131n" else "erkek"
            "sv" -> if (gender == Gender.FEMALE) "kvinna" else "man"
            "nb",
            "no" -> if (gender == Gender.FEMALE) "kvinne" else "mann"
            "da" -> if (gender == Gender.FEMALE) "kvinde" else "mand"
            "fi" -> if (gender == Gender.FEMALE) "nainen" else "mies"
            "uk" ->
                if (gender == Gender.FEMALE) "\u0436\u0456\u043d\u043e\u0447\u0438\u0439"
                else "\u0447\u043e\u043b\u043e\u0432\u0456\u0447\u0438\u0439"
            "ja" -> if (gender == Gender.FEMALE) "\u5973\u6027" else "\u7537\u6027"
            "ko" -> if (gender == Gender.FEMALE) "\u5973\u6027" else "\u7537\u6027"
            "zh" -> if (gender == Gender.FEMALE) "\u5973\u6027" else "\u7537\u6027"
            "hi" ->
                if (gender == Gender.FEMALE) "\u092e\u0939\u093f\u0932\u093e"
                else "\u092a\u0941\u0930\u0941\u0937"
            "ar" ->
                if (gender == Gender.FEMALE) "\u0623\u0646\u062b\u0649" else "\u0630\u0643\u0631"
            "th" ->
                if (gender == Gender.FEMALE) "\u0e2b\u0e0d\u0e34\u0e07" else "\u0e0a\u0e32\u0e22"
            "vi" -> if (gender == Gender.FEMALE) "n\u1eef" else "nam"
            "id",
            "ms" -> if (gender == Gender.FEMALE) "perempuan" else "laki-laki"
            else -> if (gender == Gender.FEMALE) "female" else "male"
        }
    }

    /** Get "very natural" quality label for Multilingual voices. */
    fun qualityLabel(localeCode: String): String {
        val lang = if (localeCode.length >= 2) localeCode.substring(0, 2) else "en"
        return when (lang) {
            "de" -> "sehr natürlich"
            "en" -> "very natural"
            "es" -> "muy natural"
            "fr" -> "très naturelle"
            "it" -> "molto naturale"
            "pt" -> "muito natural"
            "nl" -> "zeer natuurlijk"
            "pl" -> "bardzo naturalny"
            "ru" ->
                "\u043e\u0447\u0435\u043d\u044c \u0435\u0441\u0442\u0435\u0441\u0442\u0432\u0435\u043d\u043d\u044b\u0439"
            "tr" -> "\u00e7ok do\u011fal"
            "sv" -> "mycket naturlig"
            "nb",
            "no" -> "veldig naturlig"
            "da" -> "meget naturlig"
            "fi" -> "hyvin luonnollinen"
            "ja" -> "\u975e\u5e38\u306b\u81ea\u7136"
            "ko" -> "\u8D85\u81EA\u7136"
            "zh" -> "\u975e\u5e38\u81ea\u7136"
            "hi" ->
                "\u092c\u0939\u0941\u0924 \u0938\u094d\u0935\u093e\u092d\u093e\u0935\u093f\u0915"
            "ar" -> "\u0637\u0628\u064a\u0639\u064a \u062c\u062f\u0627\u064b"
            "th" ->
                "\u0e40\u0e1b\u0e47\u0e19\u0e18\u0e23\u0e23\u0e21\u0e0a\u0e32\u0e15\u0e34\u0e21\u0e32\u0e01"
            "vi" -> "r\u1ea5t t\u1ef1 nhi\u00ean"
            "id",
            "ms" -> "sangat alami"
            "uk" ->
                "\u0434\u0443\u0436\u0435 \u043f\u0440\u0438\u0440\u043e\u0434\u043d\u0438\u0439"
            else -> "very natural"
        }
    }

    /** Format a voice for display: "★ Seraphina — weiblich" (premium) or "Florian — männlich" */
    fun displayName(voice: Voice, localeCode: String): String {
        val g = genderLabel(localeCode, voice.gender)
        return if (voice.tier == Tier.MULTILINGUAL) {
            "\u2605 ${voice.name} \u2014 $g"
        } else {
            "${voice.name} \u2014 $g"
        }
    }

    // Language code -> default locale when country not specified
    private val LANGUAGE_FALLBACK =
        mapOf(
            "de" to "de-DE",
            "en" to "en-US",
            "es" to "es-ES",
            "fr" to "fr-FR",
            "it" to "it-IT",
            "pt" to "pt-BR",
            "nl" to "nl-NL",
            "pl" to "pl-PL",
            "ru" to "ru-RU",
            "tr" to "tr-TR",
            "sv" to "sv-SE",
            "nb" to "nb-NO",
            "no" to "nb-NO",
            "da" to "da-DK",
            "fi" to "fi-FI",
            "uk" to "uk-UA",
            "ja" to "ja-JP",
            "ko" to "ko-KR",
            "zh" to "zh-CN",
            "hi" to "hi-IN",
            "ar" to "ar-SA",
            "th" to "th-TH",
            "vi" to "vi-VN",
            "id" to "id-ID",
            "ms" to "ms-MY",
        )

    private fun ml(id: String, name: String, g: Gender) = Voice(id, name, g, Tier.MULTILINGUAL)

    private fun st(id: String, name: String, g: Gender) = Voice(id, name, g, Tier.STANDARD)

    private val F = Gender.FEMALE
    private val M = Gender.MALE

    val ALL_LOCALES: Map<String, LocaleVoices> =
        mapOf(
            // ── German ──
            "de-DE" to
                LocaleVoices(
                    "de-DE",
                    "de-DE-SeraphinaMultilingualNeural",
                    listOf(
                        ml("de-DE-SeraphinaMultilingualNeural", "Seraphina", F),
                        ml("de-DE-FlorianMultilingualNeural", "Florian", M),
                        st("de-DE-KatjaNeural", "Katja", F),
                        st("de-DE-KillianNeural", "Killian", M),
                        st("de-DE-ConradNeural", "Conrad", M),
                        st("de-DE-AmalaNeural", "Amala", F),
                    ),
                ),
            "de-AT" to
                LocaleVoices(
                    "de-AT",
                    "de-AT-IngridNeural",
                    listOf(
                        st("de-AT-IngridNeural", "Ingrid", F),
                        st("de-AT-JonasNeural", "Jonas", M),
                    ),
                ),
            "de-CH" to
                LocaleVoices(
                    "de-CH",
                    "de-CH-LeniNeural",
                    listOf(st("de-CH-LeniNeural", "Leni", F), st("de-CH-JanNeural", "Jan", M)),
                ),

            // ── English ──
            "en-US" to
                LocaleVoices(
                    "en-US",
                    "en-US-AvaMultilingualNeural",
                    listOf(
                        ml("en-US-AvaMultilingualNeural", "Ava", F),
                        ml("en-US-AndrewMultilingualNeural", "Andrew", M),
                        ml("en-US-EmmaMultilingualNeural", "Emma", F),
                        ml("en-US-BrianMultilingualNeural", "Brian", M),
                        st("en-US-JennyNeural", "Jenny", F),
                        st("en-US-GuyNeural", "Guy", M),
                        st("en-US-AriaNeural", "Aria", F),
                    ),
                ),
            "en-GB" to
                LocaleVoices(
                    "en-GB",
                    "en-GB-SoniaNeural",
                    listOf(
                        st("en-GB-SoniaNeural", "Sonia", F),
                        st("en-GB-RyanNeural", "Ryan", M),
                        st("en-GB-LibbyNeural", "Libby", F),
                    ),
                ),
            "en-AU" to
                LocaleVoices(
                    "en-AU",
                    "en-AU-WilliamMultilingualNeural",
                    listOf(
                        ml("en-AU-WilliamMultilingualNeural", "William", M),
                        st("en-AU-NatashaNeural", "Natasha", F),
                    ),
                ),
            "en-IN" to
                LocaleVoices(
                    "en-IN",
                    "en-IN-NeerjaNeural",
                    listOf(
                        st("en-IN-NeerjaNeural", "Neerja", F),
                        st("en-IN-PrabhatNeural", "Prabhat", M),
                    ),
                ),

            // ── Spanish ──
            "es-ES" to
                LocaleVoices(
                    "es-ES",
                    "es-ES-ElviraNeural",
                    listOf(
                        st("es-ES-ElviraNeural", "Elvira", F),
                        st("es-ES-AlvaroNeural", "Alvaro", M),
                    ),
                ),
            "es-MX" to
                LocaleVoices(
                    "es-MX",
                    "es-MX-DaliaNeural",
                    listOf(st("es-MX-DaliaNeural", "Dalia", F), st("es-MX-JorgeNeural", "Jorge", M)),
                ),
            "es-US" to
                LocaleVoices(
                    "es-US",
                    "es-US-PalomaNeural",
                    listOf(
                        st("es-US-PalomaNeural", "Paloma", F),
                        st("es-US-AlonsoNeural", "Alonso", M),
                    ),
                ),
            "es-AR" to
                LocaleVoices(
                    "es-AR",
                    "es-AR-ElenaNeural",
                    listOf(st("es-AR-ElenaNeural", "Elena", F), st("es-AR-TomasNeural", "Tomas", M)),
                ),

            // ── French ──
            "fr-FR" to
                LocaleVoices(
                    "fr-FR",
                    "fr-FR-VivienneMultilingualNeural",
                    listOf(
                        ml("fr-FR-VivienneMultilingualNeural", "Vivienne", F),
                        ml("fr-FR-RemyMultilingualNeural", "Rémy", M),
                        st("fr-FR-DeniseNeural", "Denise", F),
                        st("fr-FR-HenriNeural", "Henri", M),
                    ),
                ),
            "fr-CA" to
                LocaleVoices(
                    "fr-CA",
                    "fr-CA-SylvieNeural",
                    listOf(
                        st("fr-CA-SylvieNeural", "Sylvie", F),
                        st("fr-CA-JeanNeural", "Jean", M),
                        st("fr-CA-AntoineNeural", "Antoine", M),
                    ),
                ),

            // ── Italian ──
            "it-IT" to
                LocaleVoices(
                    "it-IT",
                    "it-IT-ElsaNeural",
                    listOf(
                        ml("it-IT-GiuseppeMultilingualNeural", "Giuseppe", M),
                        st("it-IT-ElsaNeural", "Elsa", F),
                        st("it-IT-DiegoNeural", "Diego", M),
                    ),
                ),

            // ── Portuguese ──
            "pt-BR" to
                LocaleVoices(
                    "pt-BR",
                    "pt-BR-ThalitaMultilingualNeural",
                    listOf(
                        ml("pt-BR-ThalitaMultilingualNeural", "Thalita", F),
                        st("pt-BR-FranciscaNeural", "Francisca", F),
                        st("pt-BR-AntonioNeural", "Antonio", M),
                    ),
                ),
            "pt-PT" to
                LocaleVoices(
                    "pt-PT",
                    "pt-PT-RaquelNeural",
                    listOf(
                        st("pt-PT-RaquelNeural", "Raquel", F),
                        st("pt-PT-DuarteNeural", "Duarte", M),
                    ),
                ),

            // ── Chinese ──
            "zh-CN" to
                LocaleVoices(
                    "zh-CN",
                    "zh-CN-XiaoxiaoNeural",
                    listOf(
                        st("zh-CN-XiaoxiaoNeural", "Xiaoxiao Classic", F),
                        st("zh-CN-YunxiNeural", "Yunxi", M),
                    ),
                ),
            "zh-TW" to
                LocaleVoices(
                    "zh-TW",
                    "zh-TW-HsiaoChenNeural",
                    listOf(
                        st("zh-TW-HsiaoChenNeural", "HsiaoChen", F),
                        st("zh-TW-YunJheNeural", "YunJhe", M),
                    ),
                ),
            "zh-HK" to
                LocaleVoices(
                    "zh-HK",
                    "zh-HK-HiuGaaiNeural",
                    listOf(
                        st("zh-HK-HiuGaaiNeural", "HiuGaai", F),
                        st("zh-HK-WanLungNeural", "WanLung", M),
                    ),
                ),

            // ── Japanese ──
            "ja-JP" to
                LocaleVoices(
                    "ja-JP",
                    "ja-JP-NanamiNeural",
                    listOf(
                        st("ja-JP-NanamiNeural", "Nanami", F),
                        st("ja-JP-KeitaNeural", "Keita", M),
                    ),
                ),

            // ── Korean ──
            "ko-KR" to
                LocaleVoices(
                    "ko-KR",
                    "ko-KR-SunHiNeural",
                    listOf(
                        ml("ko-KR-HyunsuMultilingualNeural", "Hyunsu", M),
                        st("ko-KR-SunHiNeural", "SunHi", F),
                        st("ko-KR-InJoonNeural", "InJoon", M),
                    ),
                ),

            // ── Hindi ──
            "hi-IN" to
                LocaleVoices(
                    "hi-IN",
                    "hi-IN-SwaraNeural",
                    listOf(
                        st("hi-IN-SwaraNeural", "Swara", F),
                        st("hi-IN-MadhurNeural", "Madhur", M),
                    ),
                ),

            // ── Arabic ──
            "ar-SA" to
                LocaleVoices(
                    "ar-SA",
                    "ar-SA-ZariyahNeural",
                    listOf(
                        st("ar-SA-ZariyahNeural", "Zariyah", F),
                        st("ar-SA-HamedNeural", "Hamed", M),
                    ),
                ),
            "ar-EG" to
                LocaleVoices(
                    "ar-EG",
                    "ar-EG-SalmaNeural",
                    listOf(
                        st("ar-EG-SalmaNeural", "Salma", F),
                        st("ar-EG-ShakirNeural", "Shakir", M),
                    ),
                ),

            // ── Russian ──
            "ru-RU" to
                LocaleVoices(
                    "ru-RU",
                    "ru-RU-SvetlanaNeural",
                    listOf(
                        st("ru-RU-SvetlanaNeural", "Svetlana", F),
                        st("ru-RU-DmitryNeural", "Dmitry", M),
                    ),
                ),

            // ── Turkish ──
            "tr-TR" to
                LocaleVoices(
                    "tr-TR",
                    "tr-TR-EmelNeural",
                    listOf(st("tr-TR-EmelNeural", "Emel", F), st("tr-TR-AhmetNeural", "Ahmet", M)),
                ),

            // ── Polish ──
            "pl-PL" to
                LocaleVoices(
                    "pl-PL",
                    "pl-PL-ZofiaNeural",
                    listOf(st("pl-PL-MarekNeural", "Marek", M), st("pl-PL-ZofiaNeural", "Zofia", F)),
                ),

            // ── Dutch ──
            "nl-NL" to
                LocaleVoices(
                    "nl-NL",
                    "nl-NL-FennaNeural",
                    listOf(
                        st("nl-NL-FennaNeural", "Fenna", F),
                        st("nl-NL-MaartenNeural", "Maarten", M),
                        st("nl-NL-ColetteNeural", "Colette", F),
                    ),
                ),

            // ── Swedish ──
            "sv-SE" to
                LocaleVoices(
                    "sv-SE",
                    "sv-SE-SofieNeural",
                    listOf(
                        st("sv-SE-SofieNeural", "Sofie", F),
                        st("sv-SE-MattiasNeural", "Mattias", M),
                    ),
                ),

            // ── Norwegian ──
            "nb-NO" to
                LocaleVoices(
                    "nb-NO",
                    "nb-NO-PernilleNeural",
                    listOf(
                        st("nb-NO-PernilleNeural", "Pernille", F),
                        st("nb-NO-FinnNeural", "Finn", M),
                    ),
                ),

            // ── Danish ──
            "da-DK" to
                LocaleVoices(
                    "da-DK",
                    "da-DK-ChristelNeural",
                    listOf(
                        st("da-DK-ChristelNeural", "Christel", F),
                        st("da-DK-JeppeNeural", "Jeppe", M),
                    ),
                ),

            // ── Finnish ──
            "fi-FI" to
                LocaleVoices(
                    "fi-FI",
                    "fi-FI-HarriNeural",
                    listOf(st("fi-FI-HarriNeural", "Harri", M), st("fi-FI-NooraNeural", "Noora", F)),
                ),

            // ── Thai ──
            "th-TH" to
                LocaleVoices(
                    "th-TH",
                    "th-TH-PremwadeeNeural",
                    listOf(
                        st("th-TH-PremwadeeNeural", "Premwadee", F),
                        st("th-TH-NiwatNeural", "Niwat", M),
                    ),
                ),

            // ── Vietnamese ──
            "vi-VN" to
                LocaleVoices(
                    "vi-VN",
                    "vi-VN-HoaiMyNeural",
                    listOf(
                        st("vi-VN-HoaiMyNeural", "HoaiMy", F),
                        st("vi-VN-NamMinhNeural", "NamMinh", M),
                    ),
                ),

            // ── Indonesian ──
            "id-ID" to
                LocaleVoices(
                    "id-ID",
                    "id-ID-GadisNeural",
                    listOf(st("id-ID-GadisNeural", "Gadis", F), st("id-ID-ArdiNeural", "Ardi", M)),
                ),

            // ── Malay ──
            "ms-MY" to
                LocaleVoices(
                    "ms-MY",
                    "ms-MY-YasminNeural",
                    listOf(
                        st("ms-MY-YasminNeural", "Yasmin", F),
                        st("ms-MY-OsmanNeural", "Osman", M),
                    ),
                ),

            // ── Ukrainian ──
            "uk-UA" to
                LocaleVoices(
                    "uk-UA",
                    "uk-UA-PolinaNeural",
                    listOf(
                        st("uk-UA-PolinaNeural", "Polina", F),
                        st("uk-UA-OstapNeural", "Ostap", M),
                    ),
                ),
        )
}

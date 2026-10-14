package de.frank.jarvis.tts

/**
 * Packt einen Vorlese-Text so in SSML, dass die Stimme durchgehend deutsch bleibt.
 *
 * Mehrsprachige Stimmen (Edge-„Multilingual", Google Wavenet/Neural2) bestimmen die Sprache
 * sonst je Wortgruppe neu und sprechen englisch aussehende Wörter — gerade in kurzen
 * Ideentiteln — plötzlich englisch aus. Der `<lang>`-Rahmen bindet den ganzen Text an de-DE;
 * `xml:lang` am `<speak>` allein genügt dafür nicht.
 */
fun deutschesSsml(text: String): String =
    "<speak xml:lang=\"de-DE\"><lang xml:lang=\"de-DE\">${ssmlEscape(text)}</lang></speak>"

/** Nur die drei Zeichen, die SSML zerbrechen — Umlaute bleiben unangetastet. */
fun ssmlEscape(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

/**
 * Feste deutsche Aussprachen (IPA) für Wörter, die Google Chirp 3 HD sonst englisch spricht.
 * Chirp versteht kein SSML; diese Liste geht als `customPronunciations` mit.
 */
private val DEUTSCHE_AUSSPRACHEN = mapOf(
    "Frank" to "fʁaŋk",
    "Franks" to "fʁaŋks",
)

/** Die Aussprachen für die Wörter, die in [text] vorkommen, oder null, wenn keines dabei ist. */
fun chirpAussprachen(text: String): org.json.JSONObject? {
    val liste = org.json.JSONArray()
    DEUTSCHE_AUSSPRACHEN.forEach { (wort, ipa) ->
        if (Regex("\\b" + Regex.escape(wort) + "\\b").containsMatchIn(text)) liste.put(
            org.json.JSONObject().put("phrase", wort).put("phoneticEncoding", "PHONETIC_ENCODING_IPA").put("pronunciation", ipa),
        )
    }
    return if (liste.length() == 0) null else org.json.JSONObject().put("pronunciations", liste)
}

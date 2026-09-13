package de.frank.genialeideen.tts

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

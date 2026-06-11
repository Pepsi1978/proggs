package de.frank.voicekey.data

/**
 * Eine Ziel-App, die per Wake-Word ausgeloest wird. Von Anfang an als Liste modelliert,
 * damit spaeter weitere Apps mit eigenen Wake-Woertern hinzukommen koennen (NEXT-SESSION §8).
 * Start: nur der feste Eintrag "ChatGPT Voice".
 */
data class AppTarget(
    val id: String,
    val name: String,
    val subtitle: String,
    val packageName: String,
    val activityClass: String,
) {
    companion object {
        val CHATGPT = AppTarget(
            id = "chatgpt-voice",
            name = "ChatGPT Voice",
            subtitle = "Startet den Voice-Mode, genau wie der Side-Key",
            packageName = "com.openai.chatgpt",
            // Am Fold 6 verifizierter Component-Start (NEXT-SESSION §1) — NICHT aendern,
            // ohne am Geraet neu zu ermitteln, welche Activity der Side-Key oeffnet.
            activityClass = "com.openai.voice.assistant.AssistantActivity",
        )
    }
}

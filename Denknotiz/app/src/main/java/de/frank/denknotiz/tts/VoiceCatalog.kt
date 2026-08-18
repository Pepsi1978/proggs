package de.frank.denknotiz.tts

data class SelectableVoice(val id: String, val name: String)

object VoiceCatalog {
    val edge = listOf(
        SelectableVoice("de-DE-SeraphinaMultilingualNeural", "Seraphina"),
        SelectableVoice("de-DE-FlorianMultilingualNeural", "Florian"),
        SelectableVoice("de-DE-KatjaNeural", "Katja"),
        SelectableVoice("de-DE-KillianNeural", "Killian"),
        SelectableVoice("de-DE-ConradNeural", "Conrad"),
        SelectableVoice("de-DE-AmalaNeural", "Amala"),
    )

    val chirp = listOf(
        "Achernar", "Aoede", "Autonoe", "Callirrhoe", "Despina", "Erinome", "Gacrux", "Kore",
        "Laomedeia", "Leda", "Pulcherrima", "Sulafat", "Vindemiatrix", "Zephyr", "Achird",
        "Algenib", "Algieba", "Alnilam", "Charon", "Enceladus", "Fenrir", "Iapetus", "Orus",
        "Puck", "Rasalgethi", "Sadachbia", "Sadaltager", "Schedar", "Umbriel", "Zubenelgenubi",
    ).map { SelectableVoice("de-DE-Chirp3-HD-$it", it) }
}

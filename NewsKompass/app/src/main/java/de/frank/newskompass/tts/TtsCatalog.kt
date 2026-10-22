package de.frank.newskompass.tts

import de.frank.newskompass.AppProfil

import de.frank.newskompass.data.model.Geschlecht
import de.frank.newskompass.data.model.Stimme
import de.frank.newskompass.data.model.TtsAnbieter

/**
 * Der vollständige Stimmen-Katalog (Referenz, Baustein D).
 *
 * Die Listen sind bewusst vollständig und nicht auf „die drei schönsten" gekürzt: Welche
 * Stimme angenehm klingt, entscheidet sich beim Hören, nicht beim Programmieren. Deshalb
 * bekommt jede Stimme in den Einstellungen einen Probe-Knopf.
 */
object TtsCatalog {

    const val STANDARD_EDGE_STIMME = "de-DE-SeraphinaMultilingualNeural"
    const val STANDARD_GOOGLE_STIMME = "de-DE-Chirp3-HD-Kore"

    val edgeStimmen = listOf(
        Stimme("de-DE-SeraphinaMultilingualNeural", "Seraphina", Geschlecht.WEIBLICH),
        Stimme("de-DE-KatjaNeural", "Katja", Geschlecht.WEIBLICH),
        Stimme("de-DE-AmalaNeural", "Amala", Geschlecht.WEIBLICH),
        Stimme("de-DE-FlorianMultilingualNeural", "Florian", Geschlecht.MAENNLICH),
        Stimme("de-DE-KillianNeural", "Killian", Geschlecht.MAENNLICH),
        Stimme("de-DE-ConradNeural", "Conrad", Geschlecht.MAENNLICH),
    )

    val googleStimmen = listOf(
        Stimme("de-DE-Chirp3-HD-Achernar", "Achernar", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Aoede", "Aoede", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Autonoe", "Autonoe", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Callirrhoe", "Callirrhoe", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Despina", "Despina", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Erinome", "Erinome", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Gacrux", "Gacrux", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Kore", "Kore", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Laomedeia", "Laomedeia", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Leda", "Leda", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Pulcherrima", "Pulcherrima", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Sulafat", "Sulafat", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Vindemiatrix", "Vindemiatrix", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Zephyr", "Zephyr", Geschlecht.WEIBLICH),
        Stimme("de-DE-Chirp3-HD-Achird", "Achird", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Algenib", "Algenib", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Algieba", "Algieba", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Alnilam", "Alnilam", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Charon", "Charon", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Enceladus", "Enceladus", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Fenrir", "Fenrir", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Iapetus", "Iapetus", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Orus", "Orus", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Puck", "Puck", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Rasalgethi", "Rasalgethi", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Sadachbia", "Sadachbia", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Sadaltager", "Sadaltager", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Schedar", "Schedar", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Umbriel", "Umbriel", Geschlecht.MAENNLICH),
        Stimme("de-DE-Chirp3-HD-Zubenelgenubi", "Zubenelgenubi", Geschlecht.MAENNLICH),
    )

    /**
     * Gemini 3.8 TTS hat dieselben 30 Stimmnamen wie Chirp 3 HD. Die Kennung trägt den Anbieter vorn
     * („gemini_flash:Leda“), damit [anbieterZu] Flash, Flash-Lite und Chirp auseinanderhält.
     */
    val geminiFlashStimmen = geminiStimmen(TtsAnbieter.GEMINI_FLASH)
    val geminiLiteStimmen = geminiStimmen(TtsAnbieter.GEMINI_LITE)
    val STANDARD_GEMINI_FLASH_STIMME = "${TtsAnbieter.GEMINI_FLASH.id}:Leda"
    val STANDARD_GEMINI_LITE_STIMME = "${TtsAnbieter.GEMINI_LITE.id}:Leda"

    private fun geminiStimmen(anbieter: TtsAnbieter) =
        googleStimmen.map { Stimme("${anbieter.id}:${it.name}", it.name, it.geschlecht) }

    /** Der Stimmname, den Gemini erwartet („Leda“). */
    fun geminiName(stimmeId: String): String = stimmeId.substringAfter(':')

    /** Dieselbe Stimme bei Chirp 3 HD — für den Rückfall, wenn der kostenlose Gemini-Schlüssel scheitert. */
    fun chirpZu(stimmeId: String): String = "de-DE-Chirp3-HD-${geminiName(stimmeId)}"

    /**
     * Azure AI Speech (eigenes Konto, Schlüssel + Region): dieselben Neural-Stimmen wie Edge, offiziell und
     * mit allen deutschen Stimmen, dazu die HD-Stimmen (DragonHD). Die Kennung trägt „azure:“ vorn, damit
     * [anbieterZu] sie von den gleichnamigen Edge-Stimmen unterscheidet. HD zählt nicht zum kostenlosen F0-Kontingent.
     */
    val azureStimmen = listOf(
        Stimme("azure:de-DE-Seraphina:DragonHDLatestNeural", "Seraphina HD", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-Florian:DragonHDLatestNeural", "Florian HD", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-SeraphinaMultilingualNeural", "Seraphina", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-KatjaNeural", "Katja", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-AmalaNeural", "Amala", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-ElkeNeural", "Elke", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-GiselaNeural", "Gisela (Kind)", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-KlarissaNeural", "Klarissa", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-LouisaNeural", "Louisa", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-MajaNeural", "Maja", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-TanjaNeural", "Tanja", Geschlecht.WEIBLICH),
        Stimme("azure:de-DE-FlorianMultilingualNeural", "Florian", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-ConradNeural", "Conrad", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-KillianNeural", "Killian", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-BerndNeural", "Bernd", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-ChristophNeural", "Christoph", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-KasperNeural", "Kasper", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-KlausNeural", "Klaus", Geschlecht.MAENNLICH),
        Stimme("azure:de-DE-RalfNeural", "Ralf", Geschlecht.MAENNLICH),
    )
    const val STANDARD_AZURE_STIMME = "azure:de-DE-SeraphinaMultilingualNeural"
    const val STANDARD_AZURE_REGION = "westeurope"

    /** Der Stimmname, den Azure erwartet („de-DE-KatjaNeural“). */
    fun azureName(stimmeId: String): String = stimmeId.removePrefix("azure:")

    fun istAzureHd(stimmeId: String): Boolean = stimmeId.contains("DragonHD")

    /**
     * Der Regionsname für die Adresse („westeurope“). Nimmt auch den eingefügten Endpunkt aus dem Portal
     * („https://westeurope.api.cognitive.microsoft.com/“); null, wenn nichts Brauchbares darin steht.
     */
    fun azureRegion(eingabe: String): String? {
        val roh = eingabe.trim().lowercase().ifBlank { STANDARD_AZURE_REGION }
        val ausEndpunkt = Regex("^(?:https?://)?([a-z0-9]+)\\.(?:api\\.cognitive|tts\\.speech)\\.")
            .find(roh)?.groupValues?.get(1)
        return (ausEndpunkt ?: roh).takeIf { Regex("^[a-z0-9]{2,40}$").matches(it) }
    }

    /** Findet den Anbieter zu einer Stimm-Kennung aus dem festen Katalog. */
    fun anbieterZu(stimmeId: String): TtsAnbieter? = when {
        edgeStimmen.any { it.id == stimmeId } -> TtsAnbieter.EDGE
        azureStimmen.any { it.id == stimmeId } -> TtsAnbieter.AZURE
        googleStimmen.any { it.id == stimmeId } -> TtsAnbieter.GOOGLE
        geminiFlashStimmen.any { it.id == stimmeId } -> TtsAnbieter.GEMINI_FLASH
        geminiLiteStimmen.any { it.id == stimmeId } -> TtsAnbieter.GEMINI_LITE
        else -> null
    }

    /** Die feste Stimmenliste eines Anbieters; leer bei der eigenen Alibaba-Stimme. */
    fun stimmenZu(anbieter: TtsAnbieter): List<Stimme> = when (anbieter) {
        TtsAnbieter.GOOGLE -> googleStimmen
        TtsAnbieter.EDGE -> edgeStimmen
        TtsAnbieter.GEMINI_FLASH -> geminiFlashStimmen
        TtsAnbieter.GEMINI_LITE -> geminiLiteStimmen
        TtsAnbieter.AZURE -> azureStimmen
        TtsAnbieter.QWEN -> emptyList()
    }

    /** Kurzer Satz zum Probehören — lang genug, um die Stimme zu beurteilen. */
    const val PROBETEXT =
        "Guten Morgen. So klinge ich, wenn ich dir die Nachrichten des Tages vorlese."
}

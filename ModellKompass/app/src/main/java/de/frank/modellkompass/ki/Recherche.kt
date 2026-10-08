package de.frank.modellkompass.ki

import de.frank.modellkompass.auth.ChatTurn
import de.frank.modellkompass.auth.CodexAuthManager
import de.frank.modellkompass.data.Bereich
import de.frank.modellkompass.data.Einstellungen
import de.frank.modellkompass.data.Ergebnis
import de.frank.modellkompass.data.Modellfund
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** Lässt das verbundene ChatGPT-Modell im Internet nach den besten lokalen Modellen eines Bereichs suchen. */
class Recherche(private val auth: CodexAuthManager, private val einstellungen: Einstellungen) {

    /** [status] bekommt kurze Zwischenstände für die Anzeige (kann aus einem Hintergrund-Thread kommen). */
    suspend fun suche(bereich: Bereich, status: (String) -> Unit): Ergebnis {
        val modell = einstellungen.modell
        val stufe = einstellungen.denkstufe
        var suchen = 0
        status("sucht im Internet …")
        val antwort = auth.streamChat(
            instructions = anweisung(bereich),
            turns = listOf(ChatTurn("user", "Recherchiere jetzt den Bereich „${bereich.titel}“ und antworte nur mit dem JSON-Objekt.")),
            model = modell,
            reasoningEffort = stufe,
            webSuche = true,
            prioritaet = einstellungen.prioritaet,
            onSuche = { begriff ->
                suchen++
                status(if (begriff.isBlank()) "$suchen. Suche …" else "$suchen. Suche: $begriff")
            },
            onDelta = { status("schreibt das Ergebnis …") },
        )
        val kennung = "${modell.label} · ${stufe.label}"
        val roh = runCatching { werteAus(antwort) }.getOrNull()
            ?: return Ergebnis(bereich.id, System.currentTimeMillis(), kennung, "", emptyList(), rohtext = antwort.trim())
        status("prüft die Namen auf Hugging Face …")
        val geprueft = coroutineScope {
            roh.second.map { m -> async(Dispatchers.IO) { m.copy(geprueft = aufHuggingFace(m.hfRepo)) } }.awaitAll()
        }
        return Ergebnis(bereich.id, System.currentTimeMillis(), kennung, roh.first, geprueft)
    }

    private fun anweisung(b: Bereich): String {
        val heute = SimpleDateFormat("d. MMMM yyyy", Locale.GERMANY).format(Date())
        val lmFeld = if (b.inLmStudio) {
            "\"lm_studio\": genau der Text, den man in die Modellsuche von LM Studio eintippt, in der Form herausgeber/repo-name " +
                "des GGUF-Repos (bevorzugt lmstudio-community, bartowski oder unsloth, sofern dort vorhanden)"
        } else {
            "\"lm_studio\": leerer Text, außer das Modell lässt sich tatsächlich in LM Studio laden"
        }
        return """
Du recherchierst lokale KI-Modelle für einen einzelnen Anwender. Heute ist der $heute.

Rechner: ${einstellungen.hardware.ifBlank { Einstellungen.HARDWARE_STANDARD }}. Die Modelle laufen in: ${b.laufzeit}.

Bereich: ${b.titel} – ${b.beschreibung}
Gesucht: ${b.auftrag}

So gehst du vor:
- Nutze die Websuche gründlich und mehrfach. Dein Vorwissen ist veraltet; in diesem Feld erscheinen jede Woche neue Modelle. Suche gezielt nach Veröffentlichungen der letzten Wochen und Monate.
- Quellen: Hugging Face (Modellseiten, Trending, Sammlungen), aktuelle unabhängige Benchmarks und Bestenlisten, Reddit r/LocalLLaMA und r/StableDiffusion, GitHub, Fachblogs und Vergleichstests.
- Maßstab ist die Qualität der Ergebnisse. Geschwindigkeit muss nur reichen, um vernünftig damit zu arbeiten: Das Modell soll ganz oder überwiegend in den Grafikspeicher passen. Modelle, die auf dem Rechner nur quälend langsam laufen, scheiden aus.
- Nur frei herunterladbare Modelle, die es wirklich auf Hugging Face gibt. Öffne die Hugging-Face-Seite und übernimm die Repo-Kennung buchstabengenau (Groß-/Kleinschreibung, Bindestriche, Punkte). Erfinde keine Kennung; findest du keine belegte, lass das Modell weg.
- Wähle die Quantisierung bzw. Variante, die auf diesem Rechner die beste Qualität bei noch brauchbarem Tempo liefert, und nenne die konkrete Datei.
- Liefere die ${einstellungen.anzahl} besten Modelle, das beste zuerst. Lieber weniger als unbelegte.

Antworte ausschließlich mit einem JSON-Objekt, ohne Markdown und ohne Text davor oder danach:
{
  "zusammenfassung": "zwei bis drei Sätze: Was ist derzeit Stand der Technik in diesem Bereich, und welches Modell empfiehlst du zuerst?",
  "modelle": [
    {
      "name": "Anzeigename des Modells mit Größe, z. B. Beispielmodell 32B",
      "herausgeber": "Firma oder Person, die das Originalmodell veröffentlicht hat",
      "hf_repo": "herausgeber/repo-name des Repos, aus dem man die empfohlene Datei lädt",
      "datei": "empfohlene Datei bzw. Quantisierung, z. B. Q4_K_M oder der genaue Dateiname",
      "parameter": "Modellgröße, z. B. 32 Mrd. Parameter",
      "vram_gb": 19.5,
      "laeuft_in": "Programm, in dem es läuft",
      $lmFeld,
      "staerken": "ein bis zwei Sätze: worin es am besten ist, mit Beleg (Benchmark oder Test)",
      "hinweis": "ein Satz zum Betrieb auf diesem Rechner: Tempo, nutzbarer Kontext bzw. Auflösung, Besonderheiten, Lizenz",
      "quelle": "wichtigste Internetadresse als Beleg"
    }
  ]
}
"vram_gb" ist eine Zahl: der ungefähre Grafikspeicherbedarf der empfohlenen Variante in Gigabyte.
        """.trim()
    }

    /** Liest Zusammenfassung und Modelle aus der Antwort; verträgt Markdown-Zäune und Text drumherum. */
    private fun werteAus(antwort: String): Pair<String, List<Modellfund>> {
        val text = antwort.trim()
        val anfang = text.indexOfFirst { it == '{' || it == '[' }
        require(anfang >= 0)
        val (zusammenfassung, liste) = if (text[anfang] == '[') {
            "" to JSONArray(text.substring(anfang, text.lastIndexOf(']') + 1))
        } else {
            val objekt = JSONObject(text.substring(anfang, text.lastIndexOf('}') + 1))
            objekt.optString("zusammenfassung") to (objekt.optJSONArray("modelle") ?: JSONArray())
        }
        val modelle = (0 until liste.length()).mapNotNull { liste.optJSONObject(it) }.map { m ->
            Modellfund(
                name = m.optString("name").trim(),
                herausgeber = m.optString("herausgeber").trim(),
                hfRepo = repoKennung(m.optString("hf_repo")),
                datei = m.optString("datei").trim(),
                parameter = m.optString("parameter").trim(),
                vramGb = Regex("""\d+(?:[.,]\d+)?""").find(m.optString("vram_gb"))?.value?.replace(',', '.')?.toDoubleOrNull(),
                laeuftIn = m.optString("laeuft_in").trim(),
                lmStudio = repoKennung(m.optString("lm_studio")),
                staerken = m.optString("staerken").trim(),
                hinweis = m.optString("hinweis").trim(),
                quelle = m.optString("quelle").trim(),
            )
        }.filter { it.name.isNotBlank() }
        require(modelle.isNotEmpty())
        return zusammenfassung.trim() to modelle
    }

    /** Macht aus einer Adresse oder Kennung die reine Form herausgeber/name. */
    private fun repoKennung(wert: String): String = wert.trim()
        .removePrefix("https://huggingface.co/").removePrefix("http://huggingface.co/").removePrefix("hf.co/")
        .substringBefore("/tree/").substringBefore("/blob/").substringBefore("/resolve/")
        .trim('/', ' ', '`')

    /** Fragt Hugging Face, ob es das Repo gibt. Unbekannte Repos beantwortet der Dienst ohne Anmeldung mit 401 oder 404. */
    private suspend fun aufHuggingFace(repo: String): Boolean? = withContext(Dispatchers.IO) {
        if (!Regex("""[\w.\-]+/[\w.\-]+""").matches(repo)) return@withContext false
        runCatching {
            HF_CLIENT.newCall(Request.Builder().url("https://huggingface.co/api/models/$repo").build()).execute().use { antwort ->
                when (antwort.code) {
                    200 -> true
                    401, 404 -> false
                    else -> null
                }
            }
        }.getOrNull()
    }

    private companion object {
        val HF_CLIENT: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}

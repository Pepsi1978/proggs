package de.frank.newskompass.ai

import de.frank.newskompass.data.EinstellungenStore
import de.frank.newskompass.data.model.Denkstufen
import de.frank.newskompass.observability.KompassLog
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/** Was „Modelle aktualisieren“ gefunden hat. */
data class ModellSuchErgebnis(
    /** Modelle laut Live-Katalog des Kontos. */
    val katalog: Int,
    /** Neu hinzugekommen und mit einer Probeanfrage bestätigt. */
    val neu: List<CodexModell>,
    /** Vorgeschlagen, aber vom Dienst abgelehnt — sie kommen nicht in die Liste. */
    val abgelehnt: List<String>,
    /** Die KI-Websuche selbst ist gescheitert (Katalog trotzdem aktuell). */
    val kiFehler: String?,
)

/**
 * Holt die neuesten OpenAI-Modelle in die App — auch solche, die der Live-Katalog dieser App noch
 * nicht zeigt:
 *
 * 1. Live-Katalog des Kontos laden (wie bisher).
 * 2. Den Katalog noch einmal mit einer sehr hohen Codex-Version abfragen: Der Dienst blendet neue
 *    Modelle aus, solange er die App für zu alt hält.
 * 3. Die KI per Websuche nach den aktuell in Codex wählbaren Modellen fragen.
 *
 * Jeder Kandidat aus 2 und 3, der noch nicht in der Liste steht, bekommt eine winzige Probeanfrage.
 * Nur was der Dienst wirklich beantwortet, kommt als „per KI gefunden“ dazu — ein falsch geratener
 * Modellname kann also nie die Recherche lahmlegen. Die Proben laufen nacheinander, damit Codex
 * nie mehrere Anfragen gleichzeitig bekommt.
 */
class ModellSuche(private val codex: CodexClient, private val einstellungen: EinstellungenStore) {

    suspend fun suche(fortschritt: (String) -> Unit): ModellSuchErgebnis {
        fortschritt("Lade den Modellkatalog deines Kontos …")
        val katalog = codex.ladeModelle()
        einstellungen.setzeModelle(katalog)

        val kandidaten = linkedMapOf<String, CodexModell>()
        fortschritt("Suche Modelle, die der Katalog noch ausblendet …")
        try {
            codex.ladeModelle(HOHE_VERSION).forEach { kandidaten.putIfAbsent(it.id, it) }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.warn("ModellSuche", "suche", "Erweiterter Katalog nicht abrufbar", mapOf("grund" to fehler.message))
        }

        fortschritt("Die KI sucht im Netz nach den neuesten OpenAI-Modellen …")
        var kiFehler: String? = null
        try {
            frageKi(einstellungen.stand.value.modelle).forEach { kandidaten.putIfAbsent(it.id, it) }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            kiFehler = fehler.message ?: "Unbekannter Fehler"
            KompassLog.warn("ModellSuche", "suche", "KI-Suche gescheitert", mapOf("grund" to fehler.message))
        }

        val bekannt = einstellungen.stand.value.modelle.map { it.id.lowercase() }.toSet()
        val offen = kandidaten.values.filter { it.id.lowercase() !in bekannt }.take(MAX_PROBEN)
        val neu = mutableListOf<CodexModell>()
        val abgelehnt = mutableListOf<String>()
        offen.forEachIndexed { i, kandidat ->
            fortschritt("Prüfe ${kandidat.name} (${i + 1} von ${offen.size}) …")
            val gefunden = varianten(kandidat.id).firstNotNullOfOrNull { id -> probiere(kandidat.copy(id = id)) }
            if (gefunden != null) {
                if (neu.none { it.id == gefunden.id }) neu += gefunden
            } else {
                abgelehnt += kandidat.name
            }
        }
        if (neu.isNotEmpty()) einstellungen.merkeKiModelle(neu)
        KompassLog.info("ModellSuche", "suche", "Modellsuche fertig", mapOf("katalog" to katalog.size, "neu" to neu.size, "abgelehnt" to abgelehnt.size))
        return ModellSuchErgebnis(katalog.size, neu, abgelehnt, kiFehler)
    }

    /** Fragt die KI mit Websuche nach den aktuell in Codex wählbaren Modellen. */
    private suspend fun frageKi(bekannt: List<CodexModell>): List<CodexModell> {
        val stand = einstellungen.stand.value
        val antwort = codex.frage(
            anweisung = "Du recherchierst per Websuche, welche OpenAI-Modelle heute in Codex mit einem ChatGPT-Abo " +
                "(Plus/Pro) wählbar sind — also über die Codex-CLI bzw. die Codex-App. Nutze offizielle Quellen wie " +
                "openai.com, developers.openai.com, help.openai.com und die Codex-Versionshinweise auf github.com/openai/codex. " +
                "Antworte ausschließlich mit JSON ohne Erklärung in genau dieser Form: " +
                "{\"modelle\":[{\"id\":\"exakte Modellkennung wie in Codex, kleingeschrieben\",\"name\":\"Anzeigename\"," +
                "\"stufen\":[\"low\",\"medium\",\"high\"]}]}. " +
                "Die id muss die exakte technische Kennung sein, die man bei `codex --model` angibt (z. B. gpt-5.5). " +
                "Nimm nur Modelle auf, für die es einen Beleg gibt, und nichts Erfundenes. " +
                "Zulässige Stufen: ${Denkstufen.alle.joinToString()}.",
            eingabe = "Heute ist ${java.time.LocalDate.now()}. Diese Modelle kennt die App schon: " +
                bekannt.joinToString { "${it.id} (${it.name})" } +
                ". Liste alle aktuell wählbaren Codex-Modelle auf, besonders neue, die hier noch fehlen.",
            modellId = stand.modellId,
            denktiefe = stand.modelle.firstOrNull { it.id == stand.modellId }?.stufen
                ?.let { stufen -> "medium".takeIf { it in stufen } ?: stand.denktiefe } ?: stand.denktiefe,
            werkzeuge = JSONArray().put(JSONObject().put("type", "web_search")),
        )
        return leseKiAntwort(antwort.text)
    }

    /** Eine winzige Anfrage ohne Werkzeuge: Antwortet der Dienst, gibt es das Modell für dieses Konto. */
    private suspend fun probiere(kandidat: CodexModell): CodexModell? {
        // Erst die übliche Stufe, dann die niedrigste genannte — manche Modelle kennen nicht jede.
        val stufen = (listOf("medium") + kandidat.stufen).distinct().take(2)
        for (stufe in stufen) {
            try {
                codex.frage(anweisung = "Antworte nur mit OK.", eingabe = "OK?", modellId = kandidat.id, denktiefe = stufe)
                return kandidat.copy(
                    stufen = kandidat.stufen.ifEmpty { STANDARD_STUFEN },
                    standardStufe = kandidat.standardStufe.takeIf { it in kandidat.stufen } ?: stufe,
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: CodexFehler) {
                // Kontingent oder Anmeldung: Die Probe sagt dann nichts über das Modell — abbrechen statt verwerfen.
                if (fehler.art != CodexFehlerArt.NETZ) throw fehler
                KompassLog.info("ModellSuche", "probiere", "Modell abgelehnt", mapOf("modell" to kandidat.id, "stufe" to stufe, "grund" to fehler.message))
            } catch (fehler: Exception) {
                KompassLog.info("ModellSuche", "probiere", "Probe gescheitert", mapOf("modell" to kandidat.id, "grund" to fehler.message))
            }
        }
        return null
    }

    companion object {
        /** So hoch, dass der Dienst kein Modell mehr wegen einer zu alten App ausblendet. */
        private const val HOHE_VERSION = "999.0.0"
        private const val MAX_PROBEN = 8
        private val STANDARD_STUFEN = listOf("low", "medium", "high", "xhigh")

        /**
         * Liest die JSON-Antwort der KI, auch wenn sie in einen Codeblock gepackt ist. Stufen, die der
         * Dienst nicht kennt, und offensichtlich falsche Kennungen fallen weg.
         */
        internal fun leseKiAntwort(text: String): List<CodexModell> {
            val start = text.indexOf('{')
            val ende = text.lastIndexOf('}')
            if (start < 0 || ende <= start) return emptyList()
            val liste = runCatching { JSONObject(text.substring(start, ende + 1)).optJSONArray("modelle") }.getOrNull()
                ?: return emptyList()
            return (0 until liste.length()).mapNotNull { i ->
                val m = liste.optJSONObject(i) ?: return@mapNotNull null
                val id = m.optString("id").trim().lowercase().takeIf { KENNUNG.matches(it) } ?: return@mapNotNull null
                val stufenRoh = m.optJSONArray("stufen")
                val stufen = (0 until (stufenRoh?.length() ?: 0)).mapNotNull { stufenRoh?.optString(it)?.trim()?.lowercase() }
                    .filter { it in Denkstufen.alle }.distinct()
                CodexModell(id, m.optString("name").trim().ifBlank { id }, stufen, "medium")
            }.distinctBy { it.id }
        }

        /** Kennungen wie gpt-6.1-sol oder o4-mini: nur Kleinbuchstaben, Ziffern, Punkt, Strich, Unterstrich. */
        private val KENNUNG = Regex("[a-z0-9][a-z0-9._-]{1,63}")

        /**
         * Schreibweisen derselben Kennung: Die KI schreibt „gpt-6.1-sol“, der Dienst womöglich
         * „gpt-6-1-sol“. Höchstens drei, die erste ist immer die genannte.
         */
        internal fun varianten(id: String): List<String> =
            listOf(id, id.replace('.', '-'), id.replace(' ', '-').replace('_', '-')).distinct().take(3)
    }
}

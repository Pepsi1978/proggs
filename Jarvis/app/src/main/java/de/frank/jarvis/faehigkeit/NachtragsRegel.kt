package de.frank.jarvis.faehigkeit

import java.util.Locale

/**
 * Wann ein Wunsch als Nachtrag in eine bestehende Programmier-Sitzung darf und in welche. Grundsatz (Franks Regel): nur, wenn
 * Frank es ausdrücklich verlangt, und nur in die Sitzung, die er bezeichnet. Gleiche App, ähnliches Thema, ein gerade fertiger
 * Auftrag oder eine vorhandene Sitzung sind nie ein Grund. Ohne eindeutiges Ziel wird nachgefragt, nichts gesendet.
 * Rein und ohne Android, damit die Fälle als Unit-Test laufen (NachtragsRegelTest).
 */
object NachtragsRegel {
    sealed interface Ziel {
        /** Genau dieser Auftrag ist das Ziel. */
        data class Auftrag(val id: String) : Ziel
        /** Nichts senden, Frank fragen; [grund] ist eine der Konstanten unten. */
        data class Nachfrage(val grund: String) : Ziel
    }

    const val KEIN_WUNSCH = "kein_wunsch"
    const val MEHRDEUTIG = "mehrdeutig"
    const val WIDERSPRUCH = "widerspruch"
    const val KEIN_ZIEL = "kein_ziel"

    private val nachtragWort = Regex("nachtr[aä]g|nach trag|nachtrack")
    private val ohneNachtrag = Regex("\\b(kein|keine|keinen|nicht|ohne)\\s+(\\S+\\s+){0,2}?(nachtr[aä]g|nach trag|nachtrack)")
    private val neuWunsch = Regex("\\b(neue|eigene|getrennte|separate|eigenständige)\\S*[\\s,]+(\\S+[\\s,]+){0,2}?\\S*(session|sitzung|auftrag)")
    private val ohneNeu = Regex("\\b(kein|keine|keinen|nicht|ohne)\\s+(\\S+\\s+)?(neue|eigene|getrennte|separate|eigenständige)")
    private val bestehendeSitzung = Regex("\\b(gleiche|selbe|dieselbe|derselbe|demselbe|bestehende|laufende|vorherige|alte|bisherige|diese|letzte|andere)\\S*\\s+(\\S+\\s+)?\\S*(session|sitzung)")
    private val bestehenderAuftrag = Regex("\\b(gleiche|selbe|dieselbe|derselbe|demselbe|bestehende|laufende|vorherige|alte|bisherige)\\S*\\s+(\\S+\\s+)?\\S*auftrag")
    private val nochSagen = Regex("programmierer\\s+(noch|zusätzlich|außerdem|auch noch)|\\b(sag|schreib|gib)\\s+(ihm|der sitzung|der session)\\s+(noch|zusätzlich)")
    private val dieseSitzung = Regex("\\b(gleiche|selbe|dieselbe|derselbe|demselbe|diese)\\S*\\s+(\\S+\\s+)?\\S*(session|sitzung)")

    private fun normiert(text: String) = text.lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ").trim()

    /**
     * Verlangt Frank mit [worte] ausdrücklich einen Nachtrag in eine bestehende Sitzung („Schick einen Nachtrag in die gleiche
     * Session“, „sag dem Programmierer noch …“, „in die laufende Sitzung“)? Will er ausdrücklich etwas Neues oder sagt er
     * „kein Nachtrag“, nie. Ein bloß thematisch passender Wunsch („programmiere in Jarvis noch …“) ist es nicht.
     */
    fun ausdruecklich(worte: String): Boolean {
        val t = normiert(worte)
        if (t.isEmpty()) return false
        val nachtrag = nachtragWort.containsMatchIn(t) && !ohneNachtrag.containsMatchIn(t)
        val neu = neuWunsch.containsMatchIn(t) && !ohneNeu.containsMatchIn(t)
        // Will er ausdrücklich etwas Neues, ist es kein Nachtrag; nennt er beides, ist es unklar: dann fragen.
        if (neu) return false
        return nachtrag || bestehendeSitzung.containsMatchIn(t) || bestehenderAuftrag.containsMatchIn(t) || nochSagen.containsMatchIn(t)
    }

    /** Die Auftragsnummern aus [bekannte], die in [worte] vorkommen (Groß-/Kleinschreibung egal). */
    fun genannteIds(worte: String, bekannte: Collection<String>): Set<String> {
        val woerter = Regex("[A-Za-z0-9]+").findAll(worte).map { it.value.uppercase(Locale.ROOT) }.toSet()
        return bekannte.filter { it.uppercase(Locale.ROOT) in woerter }.toSet()
    }

    /**
     * Das Ziel eines Nachtrags. [auftragId] = was das Modell angibt (leer = keins), [worte] = Franks eigene Worte, [bekannte] =
     * alle Auftragsnummern, [imGespraech] = alle Aufträge des Jarvis-Gesprächs, aus dem der Aufruf kommt (leer über das Plugin).
     * Ohne aufgerufenes Gespräch bezeichnet nur eine Nummer das Ziel, im Gespräch auch „die gleiche Session“ bei genau einem Auftrag.
     */
    fun ziel(auftragId: String, worte: String, bekannte: Collection<String>, imGespraech: List<String>): Ziel {
        if (!ausdruecklich(worte)) return Ziel.Nachfrage(KEIN_WUNSCH)
        val genannt = genannteIds(worte, bekannte)
        if (genannt.size > 1) return Ziel.Nachfrage(MEHRDEUTIG)
        if (genannt.size == 1) {
            val id = genannt.first()
            return if (auftragId.isNotEmpty() && !auftragId.equals(id, ignoreCase = true)) Ziel.Nachfrage(WIDERSPRUCH) else Ziel.Auftrag(id)
        }
        val diese = dieseSitzung.containsMatchIn(normiert(worte))
        if (auftragId.isNotEmpty()) {
            // „In die gleiche Session“ aus einem Gespräch mit Auftrag: Dann ist nur einer von dessen Aufträgen gemeint.
            if (diese && imGespraech.isNotEmpty() && imGespraech.none { it.equals(auftragId, ignoreCase = true) }) return Ziel.Nachfrage(WIDERSPRUCH)
            return Ziel.Auftrag(auftragId)
        }
        return when (imGespraech.size) {
            1 -> Ziel.Auftrag(imGespraech.first())
            0 -> Ziel.Nachfrage(KEIN_ZIEL)
            else -> Ziel.Nachfrage(MEHRDEUTIG)
        }
    }

    /**
     * Kann ein Nachtrag die Sitzung noch erreichen? Eine beendete Cloud-Sitzung nicht (dann offen melden, kein Ersatzauftrag);
     * eine fertige Sitzung am PC schon, ihr Hook wartet nach dem Abschluss weiter auf Nachträge.
     */
    fun zustellbar(cloud: Boolean, stand: String): Boolean = !(cloud && stand in listOf("fertig", "fehler"))

    /**
     * Bekommt ein neuer Auftrag ein eigenes Jarvis-Gespräch? Über das Plugin immer; in der App, wenn Frank ausdrücklich eine
     * neue Session will ([neu]) oder das Gespräch schon einen Auftrag hat: Ein neuer Auftrag hängt sich nie an einen alten.
     */
    fun eigenesGespraech(ausChatGpt: Boolean, neu: Boolean, auftraegeImGespraech: Int): Boolean = ausChatGpt || neu || auftraegeImGespraech > 0
}

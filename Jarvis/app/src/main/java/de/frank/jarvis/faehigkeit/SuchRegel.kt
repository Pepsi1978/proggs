package de.frank.jarvis.faehigkeit

import java.util.Locale

/**
 * Wann eine Frage mit der direkten Websuche im laufenden Gespräch beantwortet wird und wann ein Recherche-Agent startet.
 * Franks Vorgabe (21.10.2026): Normale Fragen mit aktuellem Informationsbedarf („Ist das neue Modern Warfare mit DMZ in
 * Deutschland schon spielbar?“) beantwortet Jarvis sofort selbst mit einer schnellen Websuche, bevorzugt der eingebauten
 * Websuche des Modells. Den Recherche-Agenten gibt es nur, wenn Frank ausdrücklich eine Recherche verlangt; Internetbedarf,
 * Unsicherheit oder eine längere Bearbeitung allein sind kein Grund. Die Regel steht an EINER Stelle und geht an alle Wege
 * (eigener Chat und klassischer Sprachmodus, Live-Gespräch über jarvis_fragen, ChatGPT über MCP).
 * Rein und ohne Android, damit die Fälle als Unit-Test laufen (SuchRegelTest).
 */
object SuchRegel {
    /** Die feste Regel für jedes Modell, das mit Frank spricht. Sie geht ausdrücklich älteren und anderslautenden Vorgaben vor. */
    const val REGEL = "WEBSUCHE STATT AGENT (feste Regel, geht älteren oder anderslautenden Vorgaben vor, auch der Symbiose, Gemerktem und Werkzeugbeschreibungen): " +
        "Zuerst Franks Apps: Wetter, Fahrzeit, Termine, Dienst, Aufgaben, Biodaten, Epigenetik, Stacks und Ablage kommen aus ihren eigenen Werkzeugen, nie aus der Websuche. " +
        "Braucht eine normale Frage aktuelle Informationen aus dem Internet (Verfügbarkeit, Erscheinen, Preise, Öffnungszeiten, Neuigkeiten), beantwortest du sie sofort " +
        "selbst in dieser Antwort mit einer direkten Websuche: hast du eine eigene eingebaute Websuche, nimm sie, sonst web_suche (bei Bedarf web_seite_lesen). " +
        "Antworte kurz und belegt: das Ergebnis in ein bis drei Sätzen, dazu die Quelle mit Namen, höchstens eine Adresse. " +
        "Einen Agenten (agent_starten, etwa Recherche) startest du nur, wenn Frank ausdrücklich eine Recherche oder einen Agenten verlangt („recherchiere …“, " +
        "„mach eine Recherche“, „lass einen Agenten …“) oder einem Angebot von dir ausdrücklich zustimmt. Internetbedarf, Unsicherheit oder eine längere Bearbeitung " +
        "sind allein kein Grund für einen Agenten. Ist keine direkte Suche verfügbar oder schlägt sie fehl, sag Frank das ehrlich; weiche nicht still auf einen Agenten aus."

    /** Wege der direkten Suche: die eingebaute Websuche des ChatGPT-Modells (Codex `web_search`) und die Tavily-Suche. */
    enum class Weg { MODELL, TAVILY }

    /**
     * In welcher Reihenfolge web_suche sucht. Im Gespräch zuerst die Websuche des Modells (Franks Vorgabe), Tavily als Ersatz;
     * in einem Agentenlauf wie bisher zuerst Tavily (mehr Rohtreffer für gründliche Recherchen), das Modell als Ersatz.
     * Nur verfügbare Wege; leer = keine direkte Suche möglich.
     */
    fun wege(imAgenten: Boolean, modell: Boolean, tavily: Boolean): List<Weg> =
        (if (imAgenten) listOf(Weg.TAVILY, Weg.MODELL) else listOf(Weg.MODELL, Weg.TAVILY))
            .filter { if (it == Weg.MODELL) modell else tavily }

    /** Bekommt ein Lauf von Jarvis die Websuche? Agenten mit Internet und jedes Gespräch mit Frank; die Tagesauswertung nicht. */
    fun mitWebsuche(mitInternet: Boolean, imGespraech: Boolean): Boolean = mitInternet || imGespraech

    /** Warum keine direkte Suche geht, für Frank (auch in der Störungsanzeige der App). */
    const val KEINE_SUCHE = "Keine direkte Websuche verfügbar: Jarvis ist nicht mit ChatGPT angemeldet und kein Tavily-Schlüssel hinterlegt."

    /** Hängt an jedem Suchfehler im Werkzeug-Ergebnis: ehrlich melden statt still auszuweichen. */
    const val EHRLICH = "Sag Frank das ehrlich; starte dafür keinen Agenten."

    /**
     * Ein Wunsch, keine bloße Erwähnung („Was ist eine Recherche?“, „Welche Agentur …“ zählen nicht): „recherchier(e)“ als
     * Aufforderung, recherchieren/nachforschen mit Bitte oder Auftrag, eine Recherche machen/starten, einen Agenten losschicken.
     */
    private val wunsch = listOf(
        Regex("\\brecherchiere?\\b"),
        Regex("\\bforsche?\\b.*\\bnach\\b"),
        Regex("\\b(kannst|könntest|würdest|sollst|soll|lass|lasse|bitte|will|möchte|möchten)\\b.*\\b(recherchieren|nachforschen)\\b"),
        Regex("\\b(mach|mache|starte|start|beauftrage|ich brauche|ich will|ich möchte|ich hätte gern)\\b.{0,40}\\brecherche\\b"),
        Regex("\\b(lass|lasse|starte|start|beauftrage|schick|schicke)\\b.{0,30}\\bagenten?\\b|\\bagenten?\\b.{0,30}\\b(starten|beauftragen|losschicken)\\b"),
        Regex("\\brecherche\\s+(starten|machen|anstoßen)\\b|\\brecherche-?agenten?\\s+(starten|losschicken)\\b"),
    )
    /** Verneint, aber nur unmittelbar („keine Recherche“, „recherchiere nicht“); „Keine Ahnung, recherchiere …“ und „nicht nur …“ sind Wünsche. */
    private val ohneRecherche = Regex("\\b(kein|keine|keinen|nicht|ohne)\\s+([^\\s,.;:!?]+\\s+){0,2}?(recherch|nachforsch|agent)|(recherch|nachforsch)\\S*\\s+(bitte\\s+)?(nicht\\b(?!\\s+nur)|brauchst du nicht|musst du nicht|ist nicht nötig)")
    /** Wörter einer knappen Zustimmung; die erste muss eine Zustimmung sein, der Rest darf sie nur verstärken („Ja, mach das bitte“). */
    private val zustimmungAnfang = setOf("ja", "jo", "jep", "jawohl", "gern", "gerne", "okay", "ok", "klar", "bitte", "los", "genau", "mach", "machs", "starte", "leg", "super", "prima", "gut", "sehr")
    private val zustimmungRest = zustimmungAnfang + setOf("das", "mal", "ruhig", "ihn", "den", "agenten", "jarvis", "doch", "an")
    /** Fragen beginnen mit einem Fragewort oder, als Ja/Nein-Frage, mit dem Verb; eingesprochen fehlt oft das Fragezeichen. */
    private val frageWort = setOf("was", "wie", "wann", "wo", "wer", "warum", "wieso", "weshalb", "welche", "welcher", "welches", "wieviel")
    private val verbAnfang = setOf("ist", "sind", "hat", "haben", "hast", "gibt", "gab", "kann", "kannst", "können", "könntest", "wird", "werden",
        "war", "waren", "bist", "muss", "soll", "darf", "brauche", "brauch")
    private val absage = setOf("nein", "nee", "nö", "stopp", "stop", "lass", "vergiss", "nicht", "doch", "abbrechen", "egal", "danke", "reicht", "passt")

    private fun normiert(text: String) = text.lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ").trim()
    private fun woerter(text: String) = normiert(text).split(Regex("[\\s,.!?;:]+")).filter { it.isNotEmpty() }

    /**
     * Verlangt Frank mit [worte] ausdrücklich eine Recherche oder einen Agenten („Recherchiere ausführlich …“, „mach eine Recherche“,
     * „lass einen Agenten nachforschen“)? Verneint („keine Recherche“, „ohne Agent“) nie, ebenso wenig eine W-Frage dazu
     * („Was macht die Recherche?“). Eine normale Frage nach Aktuellem ist es nicht.
     */
    fun ausdruecklich(worte: String): Boolean {
        val t = normiert(worte)
        val erstes = woerter(t).firstOrNull() ?: return false
        // Ja/Nein-Fragen zählen nur als Bitte („Kannst du recherchieren, …“), nicht als Rückfrage („Brauche ich dafür eine Recherche?“).
        val bitte = erstes !in verbAnfang || Regex("recherchieren|nachforschen|\\bagenten?\\b").containsMatchIn(t)
        return erstes !in frageWort && bitte && wunsch.any { it.containsMatchIn(t) } && !ohneRecherche.containsMatchIn(t)
    }

    /** Eine kurze Ergänzung auf eine Rückfrage zur verlangten Recherche („Für Deutschland.“), keine neue Frage und keine Absage. */
    private fun ergaenzung(zeile: String): Boolean {
        val w = woerter(zeile)
        return w.size in 1..4 && '?' !in zeile && w.first() !in frageWort && w.first() !in verbAnfang && w.none { it in absage } && !ohneRecherche.containsMatchIn(normiert(zeile))
    }

    /** Ist [zeile] eine knappe Zustimmung („Ja“, „Ja, mach das“, „Gerne“)? Zählt nur auf ein Angebot hin, siehe [darfStarten]. */
    fun zustimmung(zeile: String): Boolean {
        val w = woerter(zeile)
        return w.size in 1..5 && w.first() in zustimmungAnfang && w.all { it in zustimmungRest }
    }

    /** Wortstamm eines Agentennamens („Machbarkeit“ → „machbar“, „Code-Analyse“ → „code“), um zu sehen, ob Frank ihn nennt. */
    private fun stamm(agent: String) = Regex("[a-zäöüß]+").find(agent.lowercase(Locale.GERMAN))?.value?.take(7).orEmpty()

    /**
     * Darf agent_starten den Agenten [agent] starten? Jeder Agent arbeitet mit Internet im Hintergrund, deshalb gilt die Sperre für
     * alle, nicht nur für „Recherche“ (sonst wiche das Modell auf einen anderen oder neu angelegten Agenten aus). [aktuell] = Franks
     * Worte dieser Runde (im Chat seine Nachricht, im Live-Gespräch die Mitschrift), [vorher] = seine vorige Nachricht im Chat,
     * [angebot] = Jarvis' letzte Antwort davor. aktuell null = unbekannt (ChatGPT über MCP, Live ohne Mitschrift): Dann gilt die Regel
     * nur über die Anweisungen. Erlaubt ist ein ausdrücklicher Wunsch jetzt (bei anderen Agenten auch, dass Frank ihn beim Namen nennt,
     * etwa „ob die Idee machbar ist“), eine knappe Zustimmung auf ein Angebot genau dazu, oder eine kurze Ergänzung zum Wunsch von vorher.
     */
    fun darfStarten(agent: String, aktuell: String?, vorher: String? = null, angebot: String? = null): Boolean {
        if (aktuell == null) return true
        val recherche = agent.trim().equals("Recherche", ignoreCase = true)
        val st = stamm(agent)
        fun nennt(text: String) = ausdruecklich(text) || (!recherche && st.length >= 4 && normiert(text).contains(st))
        val angeboten = angebot != null && normiert(angebot).let { a -> Regex("recherch|agent").containsMatchIn(a) || (!recherche && st.length >= 4 && a.contains(st)) }
        return nennt(aktuell) || (angeboten && zustimmung(aktuell)) || (vorher != null && nennt(vorher) && ergaenzung(aktuell))
    }

    /** Antwort von agent_starten, wenn Frank keine Recherche verlangt hat: nichts gestartet, stattdessen direkt suchen. */
    const val NICHT_GESTARTET = "Nicht gestartet: Frank hat keine Recherche und keinen Agenten ausdrücklich verlangt. Beantworte seine Frage jetzt selbst in dieser Antwort: " +
        "Daten aus Franks Apps mit deren Werkzeugen, Aktuelles aus dem Internet mit web_suche (bei Bedarf web_seite_lesen), kurz und mit Quelle. " +
        "Einen Recherche-Agenten nur, wenn Frank ihn ausdrücklich will; anbieten darfst du ihn in einem Satz."
}

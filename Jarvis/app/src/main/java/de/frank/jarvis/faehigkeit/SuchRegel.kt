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
     * Ein ausdrücklicher Wunsch, keine bloße Erwähnung: das Verb („recherchiere“, „kannst du das recherchieren“, „lass nachforschen“),
     * die Recherche mit einem Wunschwort („mach eine Recherche“, „Recherche zu …“) oder ein Agent mit einem Startwort („starte einen
     * Agenten“, „lass einen Agenten …“). „Was ist eine systematische Recherche?“ oder „Welche Agentur …“ sind keiner.
     */
    private val wunsch = listOf(
        Regex("\\brecherchier(e|en|st|t)?\\b|\\bnachforsch(e|en)?\\b"),
        Regex("\\b(mach|mache|macht|starte|start|starten|brauche|brauch|will|möchte|hätte gern|hätte gerne|bitte um|gib)\\b[^.?!]{0,40}\\brecherche"),
        Regex("\\brecherche\\s+(zu|über|zum|zur|starten|machen|anstoßen)\\b|\\brecherche-?agent"),
        Regex("\\b(starte|start|starten|schick|schicke|lass|beauftrag\\w*|gib|setz\\w*)\\b[^.?!]{0,40}\\bagent(en)?\\b|\\bagent(en)?\\b[^.?!]{0,30}\\b(starten|beauftragen|losschicken)\\b"),
    )
    private val ohneRecherche = Regex("\\b(kein|keine|keinen|nicht|ohne)\\s+(\\S+\\s+){0,2}?(recherch|nachforsch|agent)|(recherch|nachforsch)\\S*\\s+(bitte\\s+)?(nicht\\b|brauchst du nicht|musst du nicht|ist nicht nötig)")
    /** Wörter einer knappen Zustimmung; die erste muss eine Zustimmung sein, der Rest darf sie nur verstärken („Ja, mach das bitte“). */
    private val zustimmungAnfang = setOf("ja", "jo", "jep", "jawohl", "gern", "gerne", "okay", "ok", "klar", "bitte", "los", "genau", "mach", "machs", "starte", "leg", "super", "prima", "gut", "sehr")
    private val zustimmungRest = zustimmungAnfang + setOf("das", "mal", "ruhig", "ihn", "den", "agenten", "jarvis", "danke", "doch", "an")

    private fun normiert(text: String) = text.lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ").trim()

    /**
     * Verlangt Frank mit [worte] ausdrücklich eine Recherche oder einen Agenten („Recherchiere ausführlich …“, „mach eine Recherche“,
     * „lass einen Agenten nachforschen“)? Verneint („keine Recherche“, „ohne Agent“) nie. Eine normale Frage nach Aktuellem ist es nicht.
     */
    fun ausdruecklich(worte: String): Boolean {
        val t = normiert(worte)
        return t.isNotEmpty() && wunsch.any { it.containsMatchIn(t) } && !ohneRecherche.containsMatchIn(t)
    }

    /** Ist [zeile] eine knappe Zustimmung („Ja“, „Ja, mach das“, „Gerne“), etwa auf ein Angebot, eine Recherche zu starten? */
    fun zustimmung(zeile: String): Boolean {
        val woerter = normiert(zeile).split(Regex("[\\s,.!]+")).filter { it.isNotEmpty() }
        return woerter.size in 1..5 && woerter.first() in zustimmungAnfang && woerter.all { it in zustimmungRest }
    }

    /**
     * Darf agent_starten den Agenten [agent] starten? Nur der Recherche-Agent wird geprüft. [juengste] = Franks jüngste eigene
     * Äußerung (im Chat seine aktuelle Nachricht, im Live-Gespräch die Mitschrift), null = unbekannt (ChatGPT über MCP, Live ohne
     * Mitschrift): Dann gilt die Regel nur über die Anweisungen. Erlaubt ist eine ausdrückliche Recherche oder eine knappe Zustimmung
     * auf ein Angebot. Nur die jüngste Äußerung zählt: Ein Wunsch von vorhin gilt nicht für die nächste normale Frage.
     */
    fun darfStarten(agent: String, juengste: String?): Boolean {
        if (!agent.trim().equals("Recherche", ignoreCase = true) || juengste == null) return true
        return ausdruecklich(juengste) || zustimmung(juengste)
    }

    /** Antwort von agent_starten, wenn Frank keine Recherche verlangt hat: nichts gestartet, stattdessen direkt suchen. */
    const val NICHT_GESTARTET = "Nicht gestartet: Frank hat keine Recherche ausdrücklich verlangt. Beantworte seine Frage jetzt selbst in dieser Antwort: " +
        "Daten aus Franks Apps mit deren Werkzeugen, Aktuelles aus dem Internet mit web_suche (bei Bedarf web_seite_lesen), kurz und mit Quelle. " +
        "Einen Recherche-Agenten nur, wenn Frank ihn ausdrücklich will; anbieten darfst du ihn in einem Satz."
}

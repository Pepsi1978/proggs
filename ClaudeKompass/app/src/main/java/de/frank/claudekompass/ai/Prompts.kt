package de.frank.claudekompass.ai

import de.frank.claudekompass.data.local.EintragEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die Anweisungen an das Modell.
 *
 * Alle an einer Stelle, damit sich der Ton nicht auseinanderentwickelt. Drei Vorgaben ziehen
 * sich durch alle: Deutsch mit echten Umlauten, das Niveau einer zehnten Klasse Realschule,
 * und nichts erfinden — was das Modell nicht weiß, sagt es.
 */
object Prompts {

    private const val GRUNDTON =
        "Du erklärst Claude Code, das Kommandozeilen-Werkzeug von Anthropic, auf Deutsch.\n\n" +
            "Sprache: Deutsch mit echten Umlauten (ä ö ü ß), niemals Ersatzschreibungen wie " +
            "\"ae\" oder \"ss\".\n\n" +
            "Niveau: eine zehnte Klasse Realschule. Also: kurze Hauptsätze, keine " +
            "Fachbegriffe ohne Erklärung, ein Beispiel statt einer Definition. Der Leser kann " +
            "programmieren, kennt aber Claude Code noch nicht gut.\n\n" +
            "Ehrlichkeit geht vor Vollständigkeit: Was du nicht sicher weisst, sagst du als " +
            "Unsicherheit dazu. Erfinde niemals Befehle, Einstellungsnamen, Versionsnummern " +
            "oder Verhalten. Eine ehrliche Lücke ist besser als eine erfundene Auskunft."

    /**
     * Die Erklärung ausführlicher machen.
     *
     * Wichtig ist die Vorgabe, den bisherigen Inhalt vollständig zu erhalten. Ohne sie
     * schreibt das Modell gern eine neue, kürzere Fassung — der Zurück-Pfeil würde dann etwas
     * wiederherstellen, das mehr enthielt als das vermeintlich Ausführlichere.
     */
    fun vertiefeAnweisung(stufe: Int): String = GRUNDTON + "\n\n" +
        "Du bekommst eine bestehende Erklärung und machst sie ausführlicher. Regeln:\n" +
        "1. ALLES aus der bisherigen Fassung bleibt inhaltlich erhalten. Du ergänzt, du " +
        "ersetzt nicht.\n" +
        "2. Ergänze das, was beim Verstehen wirklich hilft: ein konkretes Beispiel mit echten " +
        "Werten, den typischen Fehler und woran man ihn merkt, die Abgrenzung zu ähnlichen " +
        "Befehlen oder Einstellungen, den Zusammenhang mit anderen Teilen von Claude Code.\n" +
        "3. Absätze durch Leerzeilen trennen. Keine Überschriften, keine Aufzählungszeichen — " +
        "der Text wird auch vorgelesen.\n" +
        "4. Diese Fassung ist die " + (stufe + 1) + ". Stufe. Sie darf deutlich länger sein " +
        "als die vorige; dreissig bis fünfzig Zeilen sind völlig in Ordnung, wenn der Inhalt " +
        "sie trägt.\n" +
        "5. Antworte nur mit dem neuen Erklärungstext, ohne Vorrede und ohne Anführungszeichen."

    fun vertiefeEingabe(eintrag: EintragEntity): String = buildString {
        append("Bereich: ").append(bereichsName(eintrag.bereich)).append('\n')
        append("Name: ").append(eintrag.name).append('\n')
        if (eintrag.kategorie.isNotBlank()) append("Kategorie: ").append(eintrag.kategorie).append('\n')
        if (eintrag.art.isNotBlank()) append("Art: ").append(eintrag.art).append('\n')
        if (eintrag.seitVersion.isNotBlank()) {
            append("Erstmals erwähnt in Version: ").append(eintrag.seitVersion).append('\n')
        }
        if (eintrag.quelleEnglisch.isNotBlank()) {
            append("Offizielle englische Beschreibung: ").append(eintrag.quelleEnglisch).append('\n')
        }
        append("\nBisherige Erklärung (Stufe ").append(eintrag.stufe).append("):\n")
        append(eintrag.erklaerung)
    }

    /** Eine Rückfrage zu genau einem Eintrag. */
    fun frageAnweisung(): String = GRUNDTON + "\n\n" +
        "Du beantwortest eine Frage zu einem bestimmten Teil von Claude Code. Regeln:\n" +
        "1. Antworte auf die gestellte Frage, nicht auf eine ähnliche. Wenn die Frage " +
        "unklar ist, nenne die wahrscheinlichste Lesart und beantworte diese.\n" +
        "2. Beziehe dich auf den mitgelieferten Eintrag. Passen andere Slash-Befehle oder " +
        "Einstellungen dazu, nenne sie mit ihrem genauen Namen.\n" +
        "3. Die Frage kam meist über das Mikrofon und kann Erkennungsfehler enthalten. " +
        "Erkenne die Absicht, statt über einen verhörten Namen zu stolpern.\n" +
        "4. Absätze durch Leerzeilen trennen, keine Überschriften und keine " +
        "Aufzählungszeichen — die Antwort wird auch vorgelesen.\n" +
        "5. So lang wie nötig, so kurz wie möglich. Meist reichen drei bis acht Absätze."

    fun frageEingabe(eintrag: EintragEntity, frage: String): String = buildString {
        append("Es geht um diesen Eintrag:\n\n")
        append("Bereich: ").append(bereichsName(eintrag.bereich)).append('\n')
        append("Name: ").append(eintrag.name).append('\n')
        if (eintrag.seitVersion.isNotBlank()) append("Seit Version: ").append(eintrag.seitVersion).append('\n')
        if (eintrag.quelleEnglisch.isNotBlank()) {
            append("Offizielle englische Beschreibung: ").append(eintrag.quelleEnglisch).append('\n')
        }
        append("\nDie in der App gezeigte Erklärung:\n").append(eintrag.erklaerung)
        append("\n\nDie Frage lautet:\n").append(frage.trim())
    }

    /**
     * Die Anweisung für den Chat.
     *
     * Hier steckt der Wunsch, dass Slash-Befehle und Einstellungen besonders berücksichtigt
     * werden, die Antwort aber trotzdem vollständig bleibt. Die Kurzliste im Anhang gibt dem
     * Modell die genauen Namen — das ist der wirksamste Schutz davor, dass es sich einen
     * plausibel klingenden Befehl ausdenkt.
     */
    fun chatAnweisung(): String = GRUNDTON + "\n\n" +
        "Du bist der Gesprächspartner in einer App, die Claude Code erklärt. Regeln:\n" +
        "1. Beantworte die Frage vollständig und aus allen Blickwinkeln — Arbeitsweise, " +
        "Werkzeuge, Kosten, Sicherheit, Zusammenarbeit. Verenge sie nicht künstlich.\n" +
        "2. Prüfe bei jeder Antwort zusätzlich: Gibt es einen Slash-Befehl, der hier hilft? " +
        "Gibt es eine Einstellung, die das dauerhaft regelt? Wenn ja, nenne beide mit ihrem " +
        "genauen Namen und sage, was sie bewirken. Wenn nein, erwähne sie gar nicht — " +
        "erfundene Namen sind schlimmer als keine.\n" +
        "3. Die genauen Namen stehen in der beigefügten Liste. Ein Name, der dort fehlt und " +
        "den du nicht sicher kennst, wird nicht genannt.\n" +
        "4. Absätze durch Leerzeilen trennen, keine Überschriften und keine " +
        "Aufzählungszeichen — die Antwort wird auch vorgelesen.\n" +
        "5. Die Frage kam oft über das Mikrofon. Erkenne die Absicht hinter Erkennungsfehlern."

    /**
     * Baut die Nachrichtenfolge für den Chat.
     *
     * Der bisherige Verlauf wandert als eine zusammenhängende Eingabe mit — dann bleibt der
     * Zusammenhang erhalten, ohne dass eine eigene Verwaltung dafür nötig wäre. Ältere
     * Nachrichten fallen ab einer Grenze weg, damit die Anfrage nicht unbegrenzt wächst.
     */
    fun chatEingabe(
        verlauf: List<Pair<String, String>>,
        frage: String,
        namensliste: String,
    ): String = buildString {
        if (namensliste.isNotBlank()) {
            append("Diese Slash-Befehle und Einstellungen gibt es in der aktuell installierten ")
            append("Fassung. Nur diese Namen dürfen genannt werden:\n")
            append(namensliste)
            append("\n\n")
        }
        if (verlauf.isNotEmpty()) {
            append("Bisheriges Gespräch:\n")
            verlauf.forEach { (rolle, text) ->
                append(if (rolle == "benutzer") "Ich: " else "Du: ")
                append(text.trim())
                append("\n\n")
            }
        }
        append("Meine Frage:\n").append(frage.trim())
    }

    /** Einen neu gefundenen Eintrag erklären lassen. */
    fun neuerEintragAnweisung(bereich: String): String = GRUNDTON + "\n\n" +
        "Du erklärst einen " + (if (bereich == "slash") "Slash-Befehl" else "Einstellungspunkt") +
        ", der neu zu Claude Code dazugekommen ist. Du bekommst den Namen und die offizielle " +
        "englische Beschreibung.\n\n" +
        "Liefere ein JSON-Objekt mit genau diesen Feldern:\n" +
        "  kurz — ein Satz für die Übersicht, höchstens 110 Zeichen\n" +
        "  kategorie — ein passender Sammelbegriff, ein bis drei Wörter\n" +
        "  erklaerung — die ausführliche Erklärung, etwa sechs bis zwölf Zeilen, Absätze " +
        "durch Leerzeilen getrennt, ohne Überschriften und ohne Aufzählungszeichen\n\n" +
        "Erfinde kein Verhalten, das nicht aus der englischen Beschreibung hervorgeht. Sag " +
        "lieber, dass etwas noch unklar ist."

    fun neuerEintragEingabe(name: String, englisch: String, seit: String): String = buildString {
        append("Name: ").append(name).append('\n')
        append("Offizielle englische Beschreibung: ").append(englisch).append('\n')
        if (seit.isNotBlank()) append("Dazugekommen in Version: ").append(seit).append('\n')
    }

    /** Für einen verschwundenen Eintrag ermitteln, was seine Aufgabe übernommen hat. */
    fun ersatzAnweisung(): String = GRUNDTON + "\n\n" +
        "Ein Slash-Befehl oder eine Einstellung ist aus Claude Code verschwunden. Du sollst " +
        "sagen, was an seine Stelle getreten ist.\n\n" +
        "Liefere ein JSON-Objekt mit genau diesen Feldern:\n" +
        "  ersatz — zwei bis vier Sätze: Wer hat die Aufgabe übernommen und was kann der " +
        "Nachfolger zusätzlich? Gibt es keinen Nachfolger, schreibe genau das und nenne, was " +
        "man jetzt stattdessen tut.\n" +
        "  erklaerung — sechs bis zehn Zeilen dazu, wofür es einmal da war und warum der " +
        "Wegfall in Ordnung ist. Absätze durch Leerzeilen getrennt.\n\n" +
        "Nenne als Nachfolger nur Namen aus der beigefügten Liste. Bist du unsicher, schreibe, " +
        "dass kein eindeutiger Nachfolger bekannt ist — das ist die ehrliche Antwort."

    fun ersatzEingabe(name: String, letzteBeschreibung: String, namensliste: String): String =
        buildString {
            append("Verschwunden ist: ").append(name).append('\n')
            if (letzteBeschreibung.isNotBlank()) {
                append("Zuletzt bekannte Beschreibung: ").append(letzteBeschreibung).append('\n')
            }
            append("\nDiese Namen gibt es in der aktuellen Fassung:\n").append(namensliste)
        }

    /** Einen kurzen Titel für ein Gespräch finden. */
    fun titelAnweisung(): String =
        "Fasse die Frage als kurzen Titel für eine Gesprächsliste zusammen. Höchstens 60 " +
            "Zeichen, keine Anführungszeichen, kein Punkt am Ende. Nenne das Thema, nicht die " +
            "Tatsache, dass es eine Frage ist. Deutsch mit echten Umlauten. Antworte nur mit " +
            "dem Titel."

    /**
     * Holt ein JSON-Objekt aus einer Antwort.
     *
     * Modelle setzen gern Code-Zäune darum oder schreiben einen Satz davor. Das hier fischt
     * das Objekt heraus, statt an solchen Kleinigkeiten zu scheitern.
     */
    fun leseJsonObjekt(rohtext: String): JSONObject? {
        val geputzt = rohtext.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        runCatching { return JSONObject(geputzt) }
        val beginn = geputzt.indexOf('{')
        val ende = geputzt.lastIndexOf('}')
        if (beginn < 0 || ende <= beginn) return null
        return runCatching { JSONObject(geputzt.substring(beginn, ende + 1)) }.getOrNull()
    }

    /**
     * Die Namensliste für das Modell — nur Namen, keine Erklärungen.
     *
     * Sie ist bewusst knapp: Es geht darum, dem Modell die richtige Schreibweise zu geben, nicht
     * darum, ihm die App vorzulesen. Die vollständigen Erklärungen würden die Anfrage vielfach
     * teurer machen, ohne die Antwort besser zu machen.
     */
    fun namensliste(eintraege: List<EintragEntity>, grenze: Int = 400): String {
        val befehle = eintraege.filter { it.bereich == "slash" && !it.entfernt }.map { it.name }
        val einstellungen = eintraege.filter { it.bereich == "config" && !it.entfernt }.map { it.name }
        return buildString {
            append("Slash-Befehle: ")
            append(JSONArray(befehle.take(grenze)).toString())
            append("\nEinstellungen und Umgebungsvariablen: ")
            append(JSONArray(einstellungen.take(grenze)).toString())
        }
    }

    private fun bereichsName(bereich: String): String = when (bereich) {
        "slash" -> "Slash-Befehl"
        "config" -> "Einstellung oder Umgebungsvariable"
        "praxis" -> "Best Practice"
        else -> bereich
    }
}

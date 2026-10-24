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

    private val nachtragWort = Regex("nachtr[aä]g\\S*|nach trag\\S*|nachtrack\\S*")
    private val neuWunsch = Regex("\\b(neue|eigene|getrennte|separate|eigenständige|andere)\\S*[\\s,]+(\\S+[\\s,]+){0,2}?\\S*(session|sitzung|auftrag)")
    private val bestehendeSitzung = Regex("\\b(gleiche|selbe|dieselbe|derselbe|demselbe|bestehende|laufende|vorherige|alte|bisherige|diese|letzte)\\S*\\s+(\\S+\\s+)?\\S*(session|sitzung)")
    private val bestehenderAuftrag = Regex("\\b(gleiche|selbe|dieselbe|derselbe|demselbe|bestehende|laufende|vorherige|alte|bisherige)\\S*\\s+(\\S+\\s+)?\\S*auftrag")
    private val nochSagen = Regex("programmierer\\s+(noch|zusätzlich|außerdem|auch noch)|\\b(sag|schreib|gib)\\s+(ihm|der sitzung|der session)\\s+(noch|zusätzlich)")
    /** Eine Anweisung, keine bloße Erwähnung: „als Nachtrag“, „einen Nachtrag …“, „Nachtrag in/an/zu …“, „sag dem Programmierer noch“. */
    private val anweisung = Regex("\\bals (nachtr[aä]g|nach trag|nachtrack)|\\bein(en)? (nachtr[aä]g|nach trag|nachtrack)|(nachtrag|nach trag|nachtrack):? (in|an|zu|zum|zur|für)\\b|" +
        nochSagen.pattern)
    /**
     * „In dieser neuen Session“, „in die gerade gestartete Sitzung“: ein Verweis auf eine schon bestehende (eben gestartete)
     * Sitzung, kein Wunsch nach einer neuen. Nur mit „diese“ oder „gerade/eben …“; „in die/einer neuen Session“ bleibt ein Neu-Wunsch.
     */
    private val neueBestehende = Regex("\\b(dies\\S*\\s+((gerade|eben|zuletzt|soeben)\\s+)?|(die|der|den|dem)\\s+(gerade|eben|zuletzt|soeben)\\s+)(neu\\S*|gestartet\\S*)\\s+(\\S+\\s+)?\\S*(session|sitzung)\\b")
    private val verneint = Regex("\\b(nicht|nie|niemals|kein|keine|keinen|keinem|keinesfalls|ohne|nein)\\b")
    private val satzende = Regex("[.,;:!?–—]")

    private fun normiert(text: String) = text.lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ").trim()

    /** Was Franks Worte über einen Nachtrag sagen: ausdrücklich ja, ausdrücklich nein (etwas Neues, „kein Nachtrag“) oder nichts. */
    enum class Urteil { JA, NEIN, OFFEN }

    /**
     * Verneint im selben Satzteil, bis zu vier Wörter davor („kein Nachtrag“, „auf keinen Fall in die laufende …“). Dahinter
     * nicht: „dass Nachträge nicht ankommen“ beschreibt nur.
     */
    private fun verneintBei(t: String, r: IntRange): Boolean =
        verneint.containsMatchIn(t.substring(0, r.first).split(satzende).last().trim().split(" ").takeLast(4).joinToString(" "))

    /**
     * Ja- und Nein-Signale in [worte]: ein nicht verneinter Nachtrag oder Verweis auf eine bestehende Sitzung ist ein Ja, ein
     * verneinter („kein Nachtrag“, „nicht in die laufende Sitzung“) und jeder nicht verneinte Neu-Wunsch („in einer neuen Session“,
     * „als eigenen Auftrag“, „in einer anderen Sitzung“) ein Nein. „In dieser neuen Session“ verweist auf eine bestehende.
     */
    private fun signale(t: String): List<Pair<IntRange, Urteil>> {
        val liste = mutableListOf<Pair<IntRange, Urteil>>()
        for (r in listOf(nachtragWort, bestehendeSitzung, bestehenderAuftrag, nochSagen))
            r.findAll(t).forEach { liste += it.range to if (verneintBei(t, it.range)) Urteil.NEIN else Urteil.JA }
        neuWunsch.findAll(t).forEach { if (!verneintBei(t, it.range)) liste += it.range to Urteil.NEIN }
        return liste
    }

    private fun vorbereitet(worte: String) = neueBestehende.replace(normiert(worte)) { m -> m.value.substringBefore(' ') + " bestehende sitzung" }

    /**
     * Grundsatz: Ein Neu-Wunsch irgendwo sperrt, beides zusammen ist kein ausdrücklicher Nachtrag (dann fragt das Modell). Einzige
     * Ausnahme: Frank beginnt mit der Anweisung („Bitte als Nachtrag noch eine fünfte Aufgabe …“) und widerruft sie im selben ersten
     * Satz nicht. Was er danach beschreibt oder zitiert („… der zweite Auftrag, neuer ausdrücklich getrennter Auftrag …“), ist Inhalt.
     */
    fun urteil(worte: String): Urteil {
        val t = vorbereitet(worte)
        if (t.isBlank()) return Urteil.OFFEN
        val alle = signale(t)
        // Der Anfang: erster Satz, höchstens 15 Wörter (eingesprochen fehlen oft die Satzzeichen).
        val kopf = t.split(Regex("[.!?;]")).first().split(" ").take(15).joinToString(" ")
        val imKopf = alle.filter { it.first.first < kopf.length }
        anweisung.find(kopf)?.takeIf { !verneintBei(t, it.range) }?.let { if (imKopf.none { s -> s.second == Urteil.NEIN }) return Urteil.JA }
        return when {
            alle.any { it.second == Urteil.NEIN } -> Urteil.NEIN
            alle.any { it.second == Urteil.JA } -> Urteil.JA
            else -> Urteil.OFFEN
        }
    }

    /**
     * Verlangt Frank mit [worte] ausdrücklich einen Nachtrag in eine bestehende Sitzung („Schick einen Nachtrag in die gleiche
     * Session“, „sag dem Programmierer noch …“, „in die laufende Sitzung“)? Will er ausdrücklich etwas Neues oder sagt er
     * „kein Nachtrag“, nie. Ein bloß thematisch passender Wunsch („programmiere in Jarvis noch …“) ist es nicht.
     */
    fun ausdruecklich(worte: String): Boolean = urteil(worte) == Urteil.JA

    /** Ein knappes Ja („Ja, bitte!“, „Genau“, „Mach das“), wie bei der Agenten-Prüfung ([SuchRegel.zustimmung]); „Danke“ ist keins. */
    fun istBestaetigung(worte: String): Boolean = SuchRegel.zustimmung(worte)

    /** Die letzte Frage in Jarvis' Antwort [antwort] (der Satz vor dem letzten „?“), sonst null. */
    fun letzteFrage(antwort: String): String? {
        val bis = antwort.lastIndexOf('?').takeIf { it >= 0 } ?: return null
        return antwort.substring(0, bis + 1).split(Regex("(?<=[.!:;])\\s")).last().trim()
    }

    /**
     * Die Worte, an denen der Nachtragswunsch im Gespräch geprüft wird. Franks aktuelle Nachricht [aktuell] entscheidet allein,
     * wenn sie etwas sagt. Ist sie nur ein Ja und endet Jarvis' Antwort davor [rueckfrage] mit einer Frage, zählt nur diese Frage:
     * Fragt sie eindeutig nach einem Nachtrag (kein Entweder-oder, kein neuer Auftrag), bestätigt Frank sie samt der darin
     * genannten Sitzung; sonst nicht. Ohne Frage zählt seine vorige Nachricht [vorher]. Ein früheres „Nachtrag“ erlaubt keinen neuen
     * Wunsch: Ist [aktuell] kein Ja, zählt nur [aktuell].
     */
    fun pruefWorte(aktuell: String, vorher: String?, rueckfrage: String?): String {
        if (urteil(aktuell) != Urteil.OFFEN || !istBestaetigung(aktuell)) return aktuell
        val frage = rueckfrage?.let { letzteFrage(it) }
        if (frage != null) return if (eindeutigJa(frage)) aktuell + "\n" + frage else aktuell
        if (vorher != null && urteil(vorher) == Urteil.JA) return aktuell + "\n" + vorher
        return aktuell
    }

    /** Nur Ja-Signale, kein einziges Nein: Grundlage für ein bloßes „Ja“ darauf. */
    private fun eindeutigJa(worte: String): Boolean = urteil(worte) == Urteil.JA && signale(vorbereitet(worte)).none { it.second == Urteil.NEIN }

    /** Verweist Frank auf eine eben neu gestartete Sitzung („in dieser neuen Session“, „in die gerade gestartete Sitzung“)? */
    fun verweistAufNeue(worte: String): Boolean = neueBestehende.containsMatchIn(normiert(worte))

    /** Die Auftragsnummern aus [bekannte], die in [worte] vorkommen (Groß-/Kleinschreibung egal). */
    fun genannteIds(worte: String, bekannte: Collection<String>): Set<String> {
        val woerter = Regex("[A-Za-z0-9]+").findAll(worte).map { it.value.uppercase(Locale.ROOT) }.toSet()
        return bekannte.filter { it.uppercase(Locale.ROOT) in woerter }.toSet()
    }

    /**
     * Das Ziel eines Nachtrags. [auftragId] = was das Modell angibt (leer = keins), [worte] = Franks eigene Worte, [bekannte] =
     * alle Auftragsnummern, [imGespraech] = alle Aufträge des Jarvis-Gesprächs, aus dem der Aufruf kommt (leer über das Plugin).
     * Ohne aufgerufenes Gespräch bezeichnet nur eine Nummer das Ziel, im Gespräch auch „die gleiche Session“ bei genau einem Auftrag.
     * Eine Nummer nur vom Modell gilt im Gespräch mit Aufträgen nur, wenn sie dorthin gehört und eindeutig ist: der einzige Auftrag
     * des Gesprächs oder, bei mehreren, der einzige noch laufende ([laufendImGespraech], Teil von [imGespraech]). Nachgefragt wird
     * nur, wenn wirklich mehrere Sitzungen passen. [gestartetAusGespraech] = Aufträge, die aus diesem Gespräch gestartet wurden und
     * ihr eigenes haben, älteste zuerst: „in dieser neuen Session“ meint den zuletzt davon gestarteten, und nur ihn.
     */
    fun ziel(auftragId: String, worte: String, bekannte: Collection<String>, imGespraech: List<String>, laufendImGespraech: List<String> = emptyList(),
             gestartetAusGespraech: List<String> = emptyList()): Ziel {
        if (!ausdruecklich(worte)) return Ziel.Nachfrage(KEIN_WUNSCH)
        val genannt = genannteIds(worte, bekannte)
        if (genannt.size > 1) return Ziel.Nachfrage(MEHRDEUTIG)
        if (genannt.size == 1) {
            val id = genannt.first()
            return if (auftragId.isNotEmpty() && !auftragId.equals(id, ignoreCase = true)) Ziel.Nachfrage(WIDERSPRUCH) else Ziel.Auftrag(id)
        }
        // „In dieser neuen Session“: die zuletzt aus diesem Gespräch gestartete; wählt das Modell eine andere, fragen.
        if (verweistAufNeue(worte) && gestartetAusGespraech.isNotEmpty()) {
            val neueste = gestartetAusGespraech.last()
            return if (auftragId.isNotEmpty() && !auftragId.equals(neueste, ignoreCase = true)) Ziel.Nachfrage(MEHRDEUTIG) else Ziel.Auftrag(neueste)
        }
        // Bei mehreren Aufträgen im Gespräch ist der einzige noch laufende eindeutig gemeint.
        val eindeutig = imGespraech.singleOrNull() ?: laufendImGespraech.filter { l -> imGespraech.any { it.equals(l, ignoreCase = true) } }.singleOrNull()
        if (auftragId.isNotEmpty()) {
            // Die Nummer stammt nur vom Modell, nicht aus Franks Worten: Im Gespräch mit Aufträgen zählt sie nur, wenn sie zu diesem
            // Gespräch gehört und eindeutig ist. Sonst wählte das Modell die Sitzung selbst (gleiche App, zuletzt fertig …).
            if (imGespraech.isNotEmpty()) {
                if (imGespraech.none { it.equals(auftragId, ignoreCase = true) }) return Ziel.Nachfrage(WIDERSPRUCH)
                if (eindeutig == null || !eindeutig.equals(auftragId, ignoreCase = true)) return Ziel.Nachfrage(MEHRDEUTIG)
            }
            return Ziel.Auftrag(auftragId)
        }
        return when {
            imGespraech.isEmpty() -> Ziel.Nachfrage(KEIN_ZIEL)
            eindeutig != null -> Ziel.Auftrag(eindeutig)
            else -> Ziel.Nachfrage(MEHRDEUTIG)
        }
    }

    /** Ist [neu] derselbe Nachtrag wie der vorhandene [alt] von [altZeit]? Gleicher Text (Groß-/Kleinschreibung, Leerraum egal) binnen 30 Minuten. */
    fun doppelt(alt: String, altZeit: Long, neu: String, jetzt: Long): Boolean = jetzt - altZeit < 30 * 60_000L && normiert(alt) == normiert(neu)

    /**
     * Kann ein Nachtrag die Sitzung noch erreichen? Eine beendete Cloud-Sitzung nicht (dann offen melden, kein Ersatzauftrag);
     * eine fertige Sitzung am PC schon, ihr Hook wartet nach dem Abschluss weiter auf Nachträge.
     */
    fun zustellbar(cloud: Boolean, stand: String): Boolean = !(cloud && stand in listOf("fertig", "fehler"))

    /**
     * Bekommt ein neuer Auftrag ein eigenes Jarvis-Gespräch? Über das Plugin immer; in der App, wenn Frank ausdrücklich eine
     * neue Session will ([neu]) oder das Gespräch schon einen Auftrag hat: Ein neuer Auftrag hängt sich nie an einen alten.
     * [leer]: Das Gespräch begann mit genau diesem „Programmierer“-Auftrag ([ausLeeremGespraech]); dann ist es selbst die neue
     * Session, sonst entstünden aus einem Auftrag zwei Chats.
     */
    fun eigenesGespraech(ausChatGpt: Boolean, neu: Boolean, auftraegeImGespraech: Int, leer: Boolean = false): Boolean =
        ausChatGpt || (neu && !leer) || auftraegeImGespraech > 0

    /** Beginnt das Gespräch mit Franks „Programmierer“-Nachricht, ohne Frage oder Antwort davor? */
    fun ausLeeremGespraech(nachrichten: List<de.frank.jarvis.data.GespeicherteNachricht>): Boolean {
        val erste = nachrichten.indexOfFirst { it.vonMir }
        return erste >= 0 && ProgrammiererVorlage.aufgabe(nachrichten[erste].text) != null && nachrichten.take(erste).none { it.art == "antwort" }
    }
}

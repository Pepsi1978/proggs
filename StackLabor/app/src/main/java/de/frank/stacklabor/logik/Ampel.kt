package de.frank.stacklabor.logik

/**
 * Die vier Ampelzustände.
 *
 * `GRAU` ist kein Fehlerzustand, sondern eine Aussage: **„nicht bedient"** — zu diesem Ziel
 * trägt kein einziges aktives Mittel etwas bei. Grün wäre hier eine Schmeichelei; genau
 * diese Lücke soll die App aufdecken (Abnahmekriterium A-07).
 */
enum class Ampel {
    GRUEN, GELB, ROT, GRAU;

    /** Für die Sammelampel: je höher, desto schlechter. Grau zählt wie Gelb. */
    val gewicht: Int
        get() = when (this) {
            GRUEN -> 0
            GELB -> 1
            GRAU -> 1
            ROT -> 2
        }
}

/** Wirkung eines Mittels auf ein Ziel. Neutral wird nie gespeichert — es ist der Normalfall. */
enum class Wirkung { STUETZT, STOERT }

/**
 * Eine Zelle der Bewertungstabelle: was ein Mittel für ein Ziel tut.
 *
 * Diese Tabelle kommt einmal von Codex und wird gespeichert. Alle Ampeln rechnet die App
 * danach **selbst** daraus — deshalb springen sie beim Häkchen und beim Umsortieren sofort,
 * kostenlos und ohne Netz (F-14).
 */
data class Zelle(
    val mittelId: String,
    val zielId: String,
    val wirkung: Wirkung,
    /** 1 = schwach, 2 = mittel, 3 = stark */
    val staerke: Int,
    val grund: String,
)

/** Ein Ziel, wie es in einem bestimmten Stack gilt: mit seinem Rang. */
data class ZielImStack(
    val zielId: String,
    val text: String,
    /** 1 = wichtigstes Ziel dieses Stacks */
    val rang: Int,
)

/**
 * Das Rechenwerk hinter allen Ampeln — die vollständige Regel aus `01-FUNKTIONS-SPEC.md` F-14.
 *
 * Es arbeitet ausschließlich auf gespeicherten Daten: kein Netz, keine Kosten, Ergebnis in
 * Millisekunden. Deaktivierte Mittel (Häkchen weg) gehen nicht ein.
 */
object Ampelrechnung {

    /**
     * Das Gewicht eines Ziels ergibt sich aus seinem Rang **in diesem Stack**.
     *
     * Dasselbe Ziel kann im Morgen-Stack Rang 1 und im Abend-Stack Rang 9 haben — deshalb
     * ändern sich die Ampeln, wenn Ziele umsortiert werden, ohne dass eine neue Bewertung
     * nötig wäre (Abnahmekriterium A-04).
     */
    fun gewicht(rang: Int): Int = when {
        rang <= 3 -> 3
        rang <= 7 -> 2
        else -> 1
    }

    /**
     * Die Ampel eines Mittels: der schlimmste Fall, gewichtet nach der Zielpriorität.
     *
     * Beispiel: Ein Mittel stört ein Ziel auf Rang 2 (Gewicht 3) mit Stärke 2 → p = 6 → rot.
     * Rutscht dasselbe Ziel auf Rang 9 (Gewicht 1), wird p = 2 → gelb. Genau das ist die
     * Auskunft, die gebraucht wird: lohnt sich der Kompromiss?
     */
    fun ampelMittel(
        mittelId: String,
        zellen: List<Zelle>,
        zieleImStack: List<ZielImStack>,
    ): Ampel {
        val rangJeZiel = zieleImStack.associate { it.zielId to it.rang }
        var hoechstesP = 0
        for (z in zellen) {
            if (z.mittelId != mittelId || z.wirkung != Wirkung.STOERT) continue
            val rang = rangJeZiel[z.zielId] ?: continue   // Ziel gilt in diesem Stack nicht
            val p = gewicht(rang) * z.staerke
            if (p > hoechstesP) hoechstesP = p
        }
        return when {
            hoechstesP >= 6 -> Ampel.ROT
            hoechstesP >= 2 -> Ampel.GELB
            else -> Ampel.GRUEN
        }
    }

    /**
     * Die Zielnummern, die ein Mittel stört — für den Kurzgrund in Zeile 2 („stört 3, 7").
     */
    fun gestoerteZiele(
        mittelId: String,
        zellen: List<Zelle>,
        zieleImStack: List<ZielImStack>,
    ): List<Int> {
        val rangJeZiel = zieleImStack.associate { it.zielId to it.rang }
        return zellen
            .filter { it.mittelId == mittelId && it.wirkung == Wirkung.STOERT }
            .mapNotNull { rangJeZiel[it.zielId] }
            .sorted()
    }

    /**
     * Die Ampel eines Ziels.
     *
     * `S` = Summe der Stärken aller stützenden aktiven Mittel minus die der störenden.
     * - **rot**, wenn eine Störung der Stärke 3 vorliegt oder S ≤ −1
     * - **grau**, wenn kein einziges stützendes Mittel aktiv ist („nicht bedient")
     * - **grün**, wenn S ≥ 3 und keine Störung ≥ 2
     * - sonst **gelb**
     *
     * Die Reihenfolge der Prüfungen ist Absicht: Eine starke Störung schlägt alles andere,
     * und ein unbedientes Ziel wird nie grün — auch dann nicht, wenn es niemand stört.
     */
    fun ampelZiel(
        zielId: String,
        zellen: List<Zelle>,
        aktiveMittel: Set<String>,
    ): Ampel {
        var summe = 0
        var staerksteStoerung = 0
        var stuetzer = 0
        for (z in zellen) {
            if (z.zielId != zielId || z.mittelId !in aktiveMittel) continue
            when (z.wirkung) {
                Wirkung.STUETZT -> {
                    summe += z.staerke
                    stuetzer++
                }
                Wirkung.STOERT -> {
                    summe -= z.staerke
                    if (z.staerke > staerksteStoerung) staerksteStoerung = z.staerke
                }
            }
        }
        return when {
            staerksteStoerung >= 3 || summe <= -1 -> Ampel.ROT
            stuetzer == 0 -> Ampel.GRAU
            summe >= 3 && staerksteStoerung < 2 -> Ampel.GRUEN
            else -> Ampel.GELB
        }
    }

    /**
     * Die Sammelampel eines Stacks (auf B-01): die schlechteste Ziel-Ampel.
     * Grau, solange keine Bewertung vorliegt.
     */
    fun ampelStack(
        zieleImStack: List<ZielImStack>,
        zellen: List<Zelle>,
        aktiveMittel: Set<String>,
    ): Ampel {
        if (zieleImStack.isEmpty() || zellen.isEmpty()) return Ampel.GRAU
        return zieleImStack
            .map { ampelZiel(it.zielId, zellen, aktiveMittel) }
            .maxByOrNull { it.gewicht } ?: Ampel.GRAU
    }

    /** Die vier Zählpunkte auf der Stack-Karte: wie viele Ziele stehen auf grün, gelb, rot, grau. */
    fun zaehlung(
        zieleImStack: List<ZielImStack>,
        zellen: List<Zelle>,
        aktiveMittel: Set<String>,
    ): Zaehlung {
        var g = 0; var ge = 0; var r = 0; var gr = 0
        for (ziel in zieleImStack) {
            when (ampelZiel(ziel.zielId, zellen, aktiveMittel)) {
                Ampel.GRUEN -> g++
                Ampel.GELB -> ge++
                Ampel.ROT -> r++
                Ampel.GRAU -> gr++
            }
        }
        return Zaehlung(g, ge, r, gr)
    }

    /**
     * Was sich ändert, wenn ein Mittel abgewählt wird — für die Zeile in der Auswertungs-Karte
     * („Ohne Kaffee: Ziel 4 wird grün") und für den Puls nur an den tatsächlich geänderten
     * Ampeln (M-09).
     */
    fun geaenderteZiele(
        vorher: Map<String, Ampel>,
        nachher: Map<String, Ampel>,
    ): Set<String> =
        nachher.filter { (zielId, ampel) -> vorher[zielId] != ampel }.keys

    /** Alle Ziel-Ampeln eines Stacks auf einmal. */
    fun alleZielAmpeln(
        zieleImStack: List<ZielImStack>,
        zellen: List<Zelle>,
        aktiveMittel: Set<String>,
    ): Map<String, Ampel> =
        zieleImStack.associate { it.zielId to ampelZiel(it.zielId, zellen, aktiveMittel) }
}

/** Die Zählpunkte einer Stack-Karte. */
data class Zaehlung(val gruen: Int, val gelb: Int, val rot: Int, val grau: Int) {
    val gesamt: Int get() = gruen + gelb + rot + grau
}

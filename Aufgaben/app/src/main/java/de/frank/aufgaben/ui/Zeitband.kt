package de.frank.aufgaben.ui

import de.frank.aufgaben.data.Aufgabe

/** Freie Stunden zwischen zwei Terminen, die zusammengerückt werden. Minuten ab 0 Uhr, immer volle Stunden. */
data class Luecke(val von: Int, val bis: Int) {
    val minuten: Int get() = bis - von
}

/**
 * Gedehnter Abschnitt der Zeitleiste: Zwei Termine starten so dicht hintereinander, dass sich ihre Karten (mittig auf der
 * Startzeit) verdecken würden. Die Minuten von [von] bis [bis] sind dort [faktor]-mal so hoch wie normal; sonst nichts.
 */
data class Dehnung(val von: Int, val bis: Int, val faktor: Float)

/**
 * Was eine Zeitleiste zeigt: von–bis in Minuten, freie Lücken auf eine feste Höhe zusammengerückt. Außerhalb der Lücken
 * ist eine Stunde [stunde] hoch, jede Lücke [luecke]; die Minuten darin verteilen sich gleichmäßig auf diese Höhe.
 * Abschnitte mit dicht liegenden Terminen sind um [dehnungen] gestreckt, alle anderen Stunden behalten ihre Höhe.
 * Raster, Termine, Jetzt-Linie und Ziehen rechnen alle hierüber, darum passen sie immer zusammen.
 */
data class Zeitband(val vonMin: Int, val bisMin: Int, val luecken: List<Luecke> = emptyList(), val dehnungen: List<Dehnung> = emptyList()) {

    /** Höhe von [vonMin] bis [min]. */
    fun y(min: Float, stunde: Float, luecke: Float): Float = ohneDehnung(min, stunde, luecke) + zusatz(min, stunde) - zusatz(vonMin.toFloat(), stunde)

    /** Zusätzliche Höhe aller Dehnungen vor [min] (Dehnungen liegen nie in einer Lücke: dort ist kein Termin). */
    private fun zusatz(min: Float, stunde: Float): Float =
        dehnungen.sumOf { d -> ((min.coerceIn(d.von.toFloat(), d.bis.toFloat()) - d.von) * (d.faktor - 1f)).toDouble() }.toFloat() / 60f * stunde

    private fun ohneDehnung(min: Float, stunde: Float, luecke: Float): Float {
        var y = 0f
        var t = vonMin.toFloat()
        for (l in luecken) {
            if (min <= l.von) break
            y += (l.von - t) / 60f * stunde
            if (min < l.bis) return y + (min - l.von) / l.minuten * luecke
            y += luecke
            t = l.bis.toFloat()
        }
        return y + (min - t) / 60f * stunde
    }

    fun y(min: Int, stunde: Float, luecke: Float): Float = y(min.toFloat(), stunde, luecke)

    /** Umkehrung von [y]: Minute an der Höhe [px] unter dem Bandanfang. */
    fun minute(px: Float, stunde: Float, luecke: Float): Float {
        if (dehnungen.isEmpty()) return minuteOhneDehnung(px, stunde, luecke)
        // y steigt streng: halbieren. Außerhalb des Bands läuft y linear weiter, der Bereich reicht weit darüber hinaus.
        var lo = vonMin - 48f * 60f
        var hi = bisMin + 48f * 60f
        repeat(40) { val m = (lo + hi) / 2f; if (y(m, stunde, luecke) < px) lo = m else hi = m }
        return (lo + hi) / 2f
    }

    private fun minuteOhneDehnung(px: Float, stunde: Float, luecke: Float): Float {
        var y = 0f
        var t = vonMin.toFloat()
        for (l in luecken) {
            val anfang = y + (l.von - t) / 60f * stunde
            if (px <= anfang) break
            if (px < anfang + luecke) return l.von + (px - anfang) / luecke * l.minuten
            y = anfang + luecke
            t = l.bis.toFloat()
        }
        return t + (px - y) / stunde * 60f
    }

    fun hoehe(stunde: Float, luecke: Float): Float = y(bisMin, stunde, luecke)

    /** Liegt [min] echt innerhalb einer Lücke? Die Stunden an den Rändern bleiben sichtbar. */
    fun inLuecke(min: Int): Boolean = luecken.any { min > it.von && min < it.bis }

    /** Minuten, die gegenüber der eingestellten Spanne [von]–[bis] nicht zu sehen sind. */
    fun ausgeblendet(von: Int, bis: Int): Int =
        luecken.sumOf { it.minuten } + (vonMin - von).coerceAtLeast(0) + (bis - bisMin).coerceAtLeast(0)

    companion object {
        /** Startzeiten näher als so viele Minuten stehen in einer Reihe nebeneinander statt untereinander. */
        const val GLEICHE_REIHE = 5

        /** Abstand zwischen zwei untereinander stehenden Karten, in Minuten bei normaler Stundenhöhe (≈ 3 dp). */
        private const val KARTEN_ABSTAND = 4

        /**
         * Größte Zusatzhöhe einer Dehnung in Minuten normaler Höhe (2 Std. ≈ 84 dp). Bräuchte es mehr (z. B. ein langer
         * Dienst und kurz danach eine Aufgabe), stehen die Karten stattdessen nebeneinander, statt die Leiste aufzublähen.
         */
        private const val MAX_ZUSATZ = 120

        /**
         * Dehnt die Abschnitte zwischen zwei Startzeiten, deren Karten sich sonst verdecken: Die Karten stehen mittig auf
         * ihrer Startzeit, zwei aufeinanderfolgende brauchen also mindestens eine halbe Kartenhöhe der einen plus eine der
         * anderen Abstand. Gestreckt wird nur die Strecke zwischen den beiden Startzeiten, so weit wie nötig. Startzeiten,
         * die fast gleich sind ([GLEICHE_REIHE]), bilden eine Reihe; ihre Karten stehen nebeneinander. Startzeiten zählen
         * so, wie die Leiste sie zeichnet: ohne Uhrzeit am Bandanfang, außerhalb von [von]–[bis] an dessen Rand.
         */
        fun dehnungen(termine: List<Aufgabe>, von: Int, bis: Int): List<Dehnung> {
            class Reihe(val erster: Int, var letzter: Int, var halb: Int)
            val reihen = mutableListOf<Reihe>()
            termine.map { t -> (t.minuten ?: von).coerceIn(von, bis) to halbeKarte(t.dauer) }.sortedBy { it.first }.forEach { (start, halb) ->
                val letzte = reihen.lastOrNull()
                if (letzte != null && start - letzte.erster < GLEICHE_REIHE) { letzte.letzter = start; letzte.halb = maxOf(letzte.halb, halb) }
                else reihen.add(Reihe(start, start, halb))
            }
            // Gemessen ab der spätesten Startzeit der oberen Reihe: Auch deren Karte muss über der nächsten Reihe enden.
            return reihen.zipWithNext().mapNotNull { (a, b) ->
                val noetig = a.halb + b.halb + KARTEN_ABSTAND
                val da = b.erster - a.letzter
                if (da >= noetig || noetig - da > MAX_ZUSATZ) null else Dehnung(a.letzter, b.erster, noetig.toFloat() / da)
            }
        }

        /** Kürzeste Lücke, die zusammenrückt: Bei weniger als zwei freien Stunden verschwände keine Stundenzahl. */
        const val MIN_LUECKE = 120

        /** Ein Terminblock ist mindestens 28 dp hoch, bei 42 dp pro Stunde also 40 Minuten (Zeitleiste KARTE_MIN). */
        private const val BLOCK_MIN = 40

        /**
         * Halbe Kartenhöhe in Minuten (aufgerundet, eher etwas mehr): So weit reicht eine Terminkarte vor und nach ihrer
         * Startzeit, weil sie mittig auf ihr steht. Zeitleiste vergibt danach die Spalten, das Band plant den Platz ein.
         */
        fun halbeKarte(dauer: Int): Int = (maxOf(dauer, BLOCK_MIN) + 1) / 2

        /**
         * Band ohne Ziehen. Mit [auto]: eine Stunde vor dem ersten bis eine Stunde nach dem letzten Termin. Mit [luecken]:
         * zwei oder mehr freie volle Stunden zwischen zwei Terminen rücken zusammen. Weil jede Karte mittig auf ihrer
         * Startzeit steht, gilt auch die halbe Kartenhöhe davor als belegt (Termin 15 Uhr, nächster 20 Uhr: 16:00 und
         * 19:00 bleiben als Ränder stehen, 17 und 18 Uhr verschwinden).
         */
        fun fuer(termine: List<Aufgabe>, von: Int, bis: Int, auto: Boolean, luecken: Boolean, jetzt: Int? = null): Zeitband {
            var (a, b) = spanne(termine, von, bis, auto, jetzt)
            // Die aktuelle Uhrzeit [jetzt] (nur heute) ist immer zu sehen: Die Spanne reicht bis zu ihrer vollen Stunde,
            // und sie zählt wie ein Termin – viel freie Zeit zwischen ihr und den Terminen rückt also zusammen.
            if (jetzt != null) {
                a = minOf(a, jetzt / 60 * 60)
                b = maxOf(b, ((jetzt / 60 + 1) * 60).coerceAtMost(24 * 60))
            }
            // Die Karte steht mittig auf ihrer Startzeit: Belegt ist also auch die halbe Kartenhöhe davor, sonst schöbe sie
            // sich über die „… frei“-Marke einer Lücke direkt darüber.
            val strecken = termine.mapNotNull { t -> t.minuten?.let { val halb = halbeKarte(t.dauer); (it - halb) to it + maxOf(t.dauer, halb) } } + listOfNotNull(jetzt?.let { it to it + 1 })
            val dehnungen = dehnungen(termine, a, b)
            if (!luecken || strecken.size < 2) return Zeitband(a, b, dehnungen = dehnungen)
            // Termine zu belegten Strecken verschmelzen, sortiert und überlappungsfrei.
            val belegt = mutableListOf<IntArray>()
            strecken
                .sortedBy { it.first }
                .forEach { (start, ende) ->
                    val letzte = belegt.lastOrNull()
                    if (letzte != null && start <= letzte[1]) letzte[1] = maxOf(letzte[1], ende)
                    else belegt.add(intArrayOf(start, ende))
                }
            // Ränder auf volle Stunden innerhalb der Spanne: Steht von/bis auf halb (z. B. 5:30) und liegt ein Termin davor,
            // begänne die Lücke sonst um 5:30, verschluckte die 6:00 und die Leiste hätte oben keine Stundenzahl.
            val ersteStunde = (a + 59) / 60 * 60
            val letzteStunde = b / 60 * 60
            val liste = belegt.zipWithNext { vorher, danach ->
                Luecke(((vorher[1] + 59) / 60 * 60).coerceAtLeast(ersteStunde), (danach[0] / 60 * 60).coerceAtMost(letzteStunde))
            }.filter { it.minuten >= MIN_LUECKE }
            return Zeitband(a, b, liste, dehnungen)
        }

        /**
         * „Ganzer Tag“: die eingestellte Spanne, erweitert auf volle Stunden bis zu Terminen davor oder danach (z. B. ein
         * Nachtdienst um 23 Uhr bei einer Spanne bis 22 Uhr), damit kein Termin am Rand klebt.
         */
        fun ganzerTag(termine: List<Aufgabe>, von: Int, bis: Int): Zeitband {
            val starts = termine.mapNotNull { it.minuten }
            if (starts.isEmpty()) return Zeitband(von, bis)
            val erster = starts.min()
            val letzter = termine.maxOf { (it.minuten ?: erster) + maxOf(it.dauer, 30) }
            // Mit der halben Kartenhöhe davor: Die erste Karte steht mittig auf ihrem Start und ragt sonst über die Leiste.
            val obenFrei = termine.minOf { t -> t.minuten?.let { it - halbeKarte(t.dauer) } ?: erster }
            val a = minOf(von, Math.floorDiv(obenFrei, 60) * 60).coerceAtLeast(0)
            val b = maxOf(bis, (letzter + 59) / 60 * 60).coerceAtMost(24 * 60)
            return Zeitband(a, b, dehnungen = dehnungen(termine, a, b))
        }

        private fun spanne(termine: List<Aufgabe>, von: Int, bis: Int, auto: Boolean, jetzt: Int?): Pair<Int, Int> {
            if (!auto) return von to bis
            // Ohne Termine nur ein kurzer Ausschnitt von zwei Stunden: heute ab einer Stunde vor der aktuellen vollen Stunde,
            // sonst die ersten zwei Stunden der Spanne (nie über ihr Ende hinaus).
            if (termine.isEmpty()) {
                if (jetzt != null) return ((jetzt / 60 - 1) * 60).coerceIn(0, 22 * 60).let { it to it + 120 }
                return von to minOf(von + 120, maxOf(bis, von + 60))
            }
            val erster = termine.minOf { it.minuten ?: von }
            val letzter = termine.maxOf { (it.minuten ?: von) + maxOf(it.dauer, 30) }
            // Eine Stunde Vorlauf, bei langen Karten (mittig auf dem Start) so viel mehr, dass die erste ganz zu sehen ist.
            val obenFrei = termine.minOf { t -> (t.minuten ?: von) - halbeKarte(t.dauer) }
            val a = minOf((erster / 60 - 1) * 60, Math.floorDiv(obenFrei, 60) * 60).coerceIn(0, 23 * 60)
            val b = (((letzter + 59) / 60 + 1) * 60).coerceIn(a + 60, 24 * 60)
            return a to b
        }
    }
}

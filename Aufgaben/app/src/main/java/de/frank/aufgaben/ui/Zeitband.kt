package de.frank.aufgaben.ui

import de.frank.aufgaben.data.Aufgabe

/** Freie Stunden zwischen zwei Terminen, die zusammengerückt werden. Minuten ab 0 Uhr, immer volle Stunden. */
data class Luecke(val von: Int, val bis: Int) {
    val minuten: Int get() = bis - von
}

/**
 * Was eine Zeitleiste zeigt: von–bis in Minuten, freie Lücken auf eine feste Höhe zusammengerückt. Außerhalb der Lücken
 * ist eine Stunde [stunde] hoch, jede Lücke [luecke]; die Minuten darin verteilen sich gleichmäßig auf diese Höhe.
 * Raster, Termine, Jetzt-Linie und Ziehen rechnen alle hierüber, darum passen sie immer zusammen.
 */
data class Zeitband(val vonMin: Int, val bisMin: Int, val luecken: List<Luecke> = emptyList()) {

    /** Höhe von [vonMin] bis [min]. */
    fun y(min: Float, stunde: Float, luecke: Float): Float {
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
        /** Kürzeste Lücke, die zusammenrückt: Bei weniger als zwei freien Stunden verschwände keine Stundenzahl. */
        const val MIN_LUECKE = 120

        /** Ein Terminblock ist mindestens 34 dp hoch, bei 42 dp pro Stunde also gut 48 Minuten. */
        private const val BLOCK_MIN = 49

        /**
         * Band ohne Ziehen. Mit [auto]: eine Stunde vor dem ersten bis eine Stunde nach dem letzten Termin. Mit [luecken]:
         * zwei oder mehr freie volle Stunden zwischen zwei Terminen rücken zusammen (Termin 15 Uhr, nächster 19 Uhr:
         * 16:00 und 19:00 bleiben als Ränder stehen, 17 und 18 Uhr verschwinden).
         */
        fun fuer(termine: List<Aufgabe>, von: Int, bis: Int, auto: Boolean, luecken: Boolean, jetzt: Int? = null): Zeitband {
            var (a, b) = spanne(termine, von, bis, auto)
            // Die aktuelle Uhrzeit [jetzt] (nur heute) ist immer zu sehen: Die Spanne reicht bis zu ihrer vollen Stunde,
            // und sie zählt wie ein Termin – viel freie Zeit zwischen ihr und den Terminen rückt also zusammen.
            if (jetzt != null) {
                a = minOf(a, jetzt / 60 * 60)
                b = maxOf(b, ((jetzt / 60 + 1) * 60).coerceAtMost(24 * 60))
            }
            val strecken = termine.mapNotNull { t -> t.minuten?.let { it to it + maxOf(t.dauer, BLOCK_MIN) } } + listOfNotNull(jetzt?.let { it to it + 1 })
            if (!luecken || strecken.size < 2) return Zeitband(a, b)
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
            return Zeitband(a, b, liste)
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
            val a = minOf(von, erster / 60 * 60).coerceAtLeast(0)
            val b = maxOf(bis, (letzter + 59) / 60 * 60).coerceAtMost(24 * 60)
            return Zeitband(a, b)
        }

        private fun spanne(termine: List<Aufgabe>, von: Int, bis: Int, auto: Boolean): Pair<Int, Int> {
            if (!auto || termine.isEmpty()) return von to bis
            val erster = termine.minOf { it.minuten ?: von }
            val letzter = termine.maxOf { (it.minuten ?: von) + maxOf(it.dauer, 30) }
            val a = ((erster / 60 - 1) * 60).coerceIn(0, 23 * 60)
            val b = (((letzter + 59) / 60 + 1) * 60).coerceIn(a + 60, 24 * 60)
            return a to b
        }
    }
}

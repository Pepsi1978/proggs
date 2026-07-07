package de.frank.entropyreducer.data.remote.whoop

/**
 * Whoop liefert pro Workout nur eine numerische `sport_id`. Diese Tabelle uebersetzt
 * sie in lesbare deutsche Sportnamen — Quelle: Whoop Developer Docs (api-mapping)
 * + Community-Wiki, Stand 2026-05-09. Unbekannte IDs landen auf "Sport".
 *
 * Ergaenzungen einfach an die Map anhaengen — Default-Fallback fangt sie sauber ab.
 */
object WhoopSportNames {

    private val SPORT_NAMES: Map<Int, String> = mapOf(
        -1 to "Aktivität",
        0 to "Aktivität",
        1 to "Radfahren",
        16 to "Baseball",
        17 to "Basketball",
        18 to "Rudern",
        19 to "Fechten",
        20 to "Hockey",
        21 to "Football",
        22 to "Golf",
        24 to "Eishockey",
        25 to "Lacrosse",
        27 to "Rugby",
        28 to "Segeln",
        29 to "Skifahren",
        30 to "Fußball",
        31 to "Softball",
        32 to "Squash",
        33 to "Schwimmen",
        34 to "Tennis",
        35 to "Leichtathletik",
        36 to "Volleyball",
        37 to "Wasserball",
        38 to "Ringen",
        39 to "Boxen",
        42 to "Tanz",
        43 to "Pilates",
        44 to "Yoga",
        45 to "Gewichtheben",
        47 to "Skilanglauf",
        48 to "Functional Fitness",
        49 to "Duathlon",
        51 to "Turnen",
        52 to "Wandern",
        53 to "Reiten",
        55 to "Kajak",
        56 to "Kampfsport",
        57 to "Mountainbike",
        59 to "Powerlifting",
        60 to "Klettern",
        61 to "Stand-up Paddling",
        62 to "Triathlon",
        63 to "Spazieren",
        64 to "Surfen",
        65 to "Crosstrainer",
        66 to "Stairmaster",
        70 to "Meditation",
        71 to "Sonstiges",
        73 to "Tauchen",
        82 to "Ultimate Frisbee",
        83 to "Bouldern",
        84 to "Seilspringen",
        85 to "Australian Football",
        86 to "Skaten",
        87 to "Trainer",
        88 to "Eisbad",
        89 to "Pendeln",
        90 to "Gaming",
        91 to "Snowboarden",
        92 to "Motocross",
        93 to "Caddy",
        94 to "Hindernislauf",
        95 to "Motorsport",
        96 to "HIIT",
        97 to "Spinning",
        98 to "Jiu-Jitsu",
        99 to "Körperliche Arbeit",
        100 to "Cricket",
        101 to "Pickleball",
        102 to "Inline-Skating",
        103 to "Box-Fitness",
        104 to "Spikeball",
        105 to "Rollstuhlsport",
        106 to "Paddle Tennis",
        107 to "Barre",
        108 to "Bühnenauftritt",
        109 to "Stress-Job",
        110 to "Parkour",
        111 to "Gaelic Football",
        112 to "Hurling",
        113 to "Zirkussport",
        121 to "Massage",
        123 to "Krafttraining",
        125 to "Sport schauen",
        126 to "Assault Bike",
        127 to "Kickboxen",
        128 to "Dehnen",
        230 to "Tischtennis",
        231 to "Badminton",
        232 to "Netball",
        233 to "Sauna",
        234 to "Disc Golf",
        235 to "Gartenarbeit",
        236 to "Druckluftmassage",
        237 to "Massagepistole",
        238 to "Paintball",
        239 to "Eislaufen",
        240 to "Handball",
    )

    /**
     * Whoop-Sport-ID -> deutscher Name. Bei unbekannten IDs wird "Sport #N"
     * zurueckgegeben damit die UI nicht leer bleibt.
     */
    fun nameOf(sportId: Int?): String = when (sportId) {
        null -> "Aktivität"
        else -> SPORT_NAMES[sportId] ?: "Sport #$sportId"
    }

    /**
     * Whoop-Sport-ID -> Material-Icon-Name. Wir mappen grob auf bestehende Material
     * Icons damit die WorkoutCard immer ein passendes Symbol zeigt.
     * Fallback: FitnessCenter (Hantel) — passt zu Krafttraining und ist generisch genug.
     */
    fun iconKeyOf(sportId: Int?): SportIcon = when (sportId) {
        1, 57 -> SportIcon.BIKE
        18 -> SportIcon.ROW
        29, 47, 91 -> SportIcon.SKI
        33 -> SportIcon.POOL
        44, 70, 107, 128 -> SportIcon.SELF_IMPROVEMENT
        35, 49, 52, 62, 63, 84, 110, 235 -> SportIcon.RUN
        45, 48, 59, 96, 123 -> SportIcon.FITNESS
        17, 21, 22, 24, 27, 30, 32, 34, 36, 37, 100, 101, 230, 231, 232, 240 -> SportIcon.SPORTS
        38, 39, 56, 98, 127 -> SportIcon.MARTIAL
        60, 83 -> SportIcon.HIKING
        86, 102, 239 -> SportIcon.SKATE
        else -> SportIcon.FITNESS
    }
}

enum class SportIcon {
    BIKE, ROW, SKI, POOL, SELF_IMPROVEMENT, RUN, FITNESS, SPORTS, MARTIAL, HIKING, SKATE
}

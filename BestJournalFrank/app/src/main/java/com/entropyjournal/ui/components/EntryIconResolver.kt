package com.entropyjournal.ui.components

/**
 * Stichwort-basierte Symbol-Erkennung als Offline-Fallback und Sofort-Anzeige.
 * Liefert einen LucideIconPool-Schluessel zurueck.
 *
 * Reihenfolge der Regeln = Prioritaet (spezifische Begriffe vor generischen).
 * Der erste Treffer gewinnt.
 *
 * Spaetere Erweiterung (V0.17.0): Gemini-Klassifikator ueberschreibt das Ergebnis
 * mit einer praeziseren Zuordnung wenn Internet + API-Key verfuegbar sind.
 */
object EntryIconResolver {

    private data class IconRule(
        val keywords: List<String>,
        val iconKey: String,
    )

    private val rules: List<IconRule> = listOf(
        // === Wasser / Hydrierung — ZUERST weil "morgen", "heute" etc. sonst die Tageszeit-Regeln triggern ===
        IconRule(listOf("hydrierung", "hydratation", "wasser trinken", "1.5 liter", "2 liter wasser", "viel trinken", "trinkmenge"), "glass_water"),
        IconRule(listOf("wasser", "getrunken", "schlucke", "durst"), "droplet"),
        IconRule(listOf("schwitzen", "geschwitzt", "schweiss", "nasse haare"), "droplets"),

        // === Tageszeit / Wetter ===
        IconRule(listOf("sonnenaufgang", "frueher morgen"), "sunrise"),
        IconRule(listOf("sonnenuntergang", "abenddaemmerung", "abenddämmerung"), "sunset"),
        IconRule(listOf("morgen", "aufgewacht", "fruehstueck", "frühstück"), "sun"),
        IconRule(listOf("abend", "feierabend", "dämmerung", "daemmerung"), "sunset"),
        IconRule(listOf("traum", "albtraum", "geträumt", "getraeumt", "sternenhimmel"), "moon_star"),
        IconRule(listOf("nacht", "schlafen", "geschlafen", "geschlummert"), "moon"),
        IconRule(listOf("regen", "geregnet", "schauer", "niesel"), "cloud_rain"),
        IconRule(listOf("schnee", "geschneit", "winter"), "snowflake"),
        IconRule(listOf("wind", "stuermisch", "stürmisch", "boe", "böe"), "wind"),
        IconRule(listOf("wolke", "wolken", "bewoelkt", "bewölkt", "neblig", "nebel", "grau"), "cloud"),
        IconRule(listOf("sonnig", "sonne", "warm", "heiss", "heiß", "sommer"), "sun"),

        // === Bewegung / Sport / Natur ===
        IconRule(listOf("ski", "skifahren", "snowboard", "alpen"), "mountain_snow"),
        IconRule(listOf("wandern", "wanderung", "berg", "berge", "gipfel", "huette", "hütte"), "mountain"),
        IconRule(listOf("wald", "natur", "park", "draussen", "draußen", "baeume", "bäume"), "trees"),
        IconRule(listOf("blume", "blumen", "garten", "blueten", "blühen", "fruehling", "frühling"), "flower"),
        IconRule(listOf("blatt", "pflanze", "botanik", "bio"), "leaf"),
        IconRule(listOf("spaziergang", "spazieren", "geschlendert"), "footprints"),
        IconRule(listOf("rad", "fahrrad", "radfahren", "biking", "radtour"), "bike"),
        IconRule(listOf("fitness", "gym", "training", "workout", "studio", "krafttraining"), "dumbbell"),
        IconRule(listOf("laufen", "joggen", "marathon", "lauf", "fussball", "fußball", "tennis", "schwimmen", "sport"), "person_standing"),
        IconRule(listOf("erfolg", "geschafft", "gewonnen", "stolz", "leistung", "ziel erreicht", "siegen"), "trophy"),

        // === Essen / Trinken ===
        IconRule(listOf("kaffee", "espresso", "cappuccino", "latte"), "coffee"),
        IconRule(listOf("pizza"), "pizza"),
        IconRule(listOf("wein", "rotwein", "weisswein", "weißwein", "anstossen", "anstoßen"), "wine"),
        IconRule(listOf("bier", "kneipenabend"), "beer"),
        IconRule(listOf("apfel", "obst", "gemuese", "gemüse", "gesund essen"), "apple"),
        IconRule(listOf("kuchen", "torte"), "cake"),
        IconRule(listOf("restaurant", "essen", "gegessen", "abendessen", "mittagessen", "lecker", "kochen", "gekocht", "rezept"), "utensils"),

        // === Reise / Verkehr ===
        IconRule(listOf("flug", "geflogen", "flugzeug", "airport", "flughafen"), "plane"),
        IconRule(listOf("zug", "bahn", "ice", "pendeln", "deutsche bahn"), "train"),
        IconRule(listOf("schiff", "boot", "faehre", "fähre", "kreuzfahrt"), "ship"),
        IconRule(listOf("strand", "meer", "see", "wasser", "kueste", "küste"), "anchor"),
        IconRule(listOf("urlaub", "reise", "gereist", "ferien", "trip", "abenteuer"), "compass"),
        IconRule(listOf("auto", "gefahren", "fahrt", "stau"), "car"),
        IconRule(listOf("ort", "platz", "standort", "besucht"), "map_pin"),

        // === Haus / Soziales / Familie ===
        IconRule(listOf("zuhause", "wohnung", "haus", "heim", "putzen", "geputzt", "aufgeraeumt", "aufgeräumt"), "home"),
        IconRule(listOf("couch", "sofa", "fernsehabend", "gemuetlich", "gemütlich", "ausgeruht"), "sofa"),
        IconRule(listOf("bett", "ins bett", "geschlummert"), "bed"),
        IconRule(listOf("baby", "kind", "kinder", "schwanger"), "baby"),
        IconRule(listOf("hund", "hundespaziergang"), "dog"),
        IconRule(listOf("katze", "miez"), "cat"),
        IconRule(listOf("liebe", "verliebt", "kuss", "umarmung", "umarmt", "partner", "partnerin"), "heart"),
        IconRule(listOf("familie", "eltern", "mama", "papa", "geschwister", "oma", "opa"), "heart_handshake"),
        IconRule(listOf("freund", "freunde", "freundin", "kollege", "kollegen", "treffen"), "users"),
        IconRule(listOf("allein", "fuer mich", "für mich", "selbst"), "user"),

        // === Arbeit / Geist / Lernen ===
        IconRule(listOf("programmiert", "code", "coding", "software", "developer", "entwickelt"), "code"),
        IconRule(listOf("computer", "laptop", "rechner", "am pc"), "laptop"),
        IconRule(listOf("arbeit", "job", "buero", "büro", "meeting", "termin", "projekt", "beruf"), "briefcase"),
        IconRule(listOf("idee", "einfall", "inspiration", "kreativ", "geistesblitz"), "lightbulb"),
        IconRule(listOf("nachgedacht", "gedanken", "ueberlegt", "überlegt", "philosophie", "reflexion", "gegruebelt", "gegrübelt"), "brain"),
        IconRule(listOf("gelernt", "studium", "uni", "schule", "kurs", "weiterbildung", "pruefung", "prüfung"), "graduation_cap"),
        IconRule(listOf("gelesen", "buch", "buecher", "bücher", "roman"), "book_open"),
        IconRule(listOf("geschrieben", "notiert", "festgehalten", "tagebuch"), "pencil"),

        // === Stimmung / Gefuehle ===
        IconRule(listOf("ueberglueklich", "überglücklich", "begeistert", "euphorisch", "fantastisch"), "smile_plus"),
        IconRule(listOf("glueklich", "glücklich", "freude", "froh", "lachen", "gelacht", "gut gelaunt", "zufrieden"), "smile"),
        IconRule(listOf("traurig", "weinen", "geweint", "trauer", "deprimiert", "depressiv", "betruebt", "betrübt"), "frown"),
        IconRule(listOf("wuetend", "wütend", "zornig", "aerger", "ärger", "sauer", "frustriert"), "angry"),
        IconRule(listOf("neutral", "mittel", "geht so", "okay"), "meh"),
        IconRule(listOf("magisch", "wunder", "wunderschoen", "wunderschön", "besonders", "wow"), "sparkles"),
        IconRule(listOf("highlight", "moment", "top"), "star"),
        IconRule(listOf("leidenschaftlich", "intensiv", "brennend", "feuer", "lagerfeuer"), "flame"),
        IconRule(listOf("energie", "power", "produktiv", "schwung", "dynamisch"), "zap"),

        // === Gesundheit ===
        IconRule(listOf("arzt", "doktor", "untersuchung", "krankenhaus", "klinik"), "stethoscope"),
        IconRule(listOf("krank", "erkaeltet", "erkältet", "fieber", "kopfschmerzen", "schmerzen", "tabletten", "medikament"), "pill"),

        // === Kultur / Hobby ===
        IconRule(listOf("musik", "konzert", "song", "lied", "instrument", "gitarre", "klavier", "gehoert", "gehört"), "music"),
        IconRule(listOf("mikrofon", "voice", "eingesprochen", "sprachaufnahme"), "mic"),
        IconRule(listOf("foto", "fotografiert", "kamera", "bild gemacht", "shooting"), "camera"),
        IconRule(listOf("malen", "gemalt", "kunst"), "palette"),
        IconRule(listOf("zeichnen", "gezeichnet", "skizze", "skizziert"), "brush"),
        IconRule(listOf("film", "kino", "serie", "netflix", "geguckt", "geschaut"), "film"),
        IconRule(listOf("gaming", "spiel", "gezockt", "konsole", "videospiel"), "gamepad"),

        // === Diverses ===
        IconRule(listOf("geschenk", "geschenkt", "ueberraschung", "überraschung"), "gift"),
        IconRule(listOf("shopping", "einkaufen", "gekauft", "laden"), "shopping_bag"),
        IconRule(listOf("telefon", "anruf", "telefoniert"), "phone"),
        IconRule(listOf("kalender", "geplant", "termin"), "calendar"),
        IconRule(listOf("wecker", "fruehaufstehen", "frühaufstehen", "wachgeworden", "alarm"), "alarm_clock"),
        IconRule(listOf("zeit", "uhrzeit", "warten", "gewartet"), "clock"),
    )

    /**
     * Findet den passenden Symbol-Schluessel fuer einen Eintrag.
     * @param text Der vollstaendige Eintragstext (Titel + Body)
     * @param isVoiceEntry True wenn der Eintrag per Sprache aufgenommen wurde
     */
    fun resolveKey(text: String, isVoiceEntry: Boolean = false): String {
        if (text.isBlank()) {
            return if (isVoiceEntry) LucideIconPool.voiceKey else LucideIconPool.fallbackKey
        }

        val lower = text.lowercase()
        for (rule in rules) {
            for (kw in rule.keywords) {
                if (lower.contains(kw)) return rule.iconKey
            }
        }
        return if (isVoiceEntry) LucideIconPool.voiceKey else LucideIconPool.fallbackKey
    }
}

package com.entropyjournal.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.AlarmClock
import com.composables.icons.lucide.Anchor
import com.composables.icons.lucide.Angry
import com.composables.icons.lucide.Apple
import com.composables.icons.lucide.Baby
import com.composables.icons.lucide.Bed
import com.composables.icons.lucide.Beer
import com.composables.icons.lucide.Bike
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Brain
import com.composables.icons.lucide.Briefcase
import com.composables.icons.lucide.Brush
import com.composables.icons.lucide.Cake
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Car
import com.composables.icons.lucide.Cat
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.CloudRain
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.Compass
import com.composables.icons.lucide.Dog
import com.composables.icons.lucide.Droplet
import com.composables.icons.lucide.Droplets
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Flower2
import com.composables.icons.lucide.Footprints
import com.composables.icons.lucide.Frown
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Gift
import com.composables.icons.lucide.GlassWater
import com.composables.icons.lucide.GraduationCap
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.HeartHandshake
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Laptop
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Meh
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.MoonStar
import com.composables.icons.lucide.Mountain
import com.composables.icons.lucide.MountainSnow
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.PersonStanding
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.Pill
import com.composables.icons.lucide.Pizza
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.Ship
import com.composables.icons.lucide.ShoppingBag
import com.composables.icons.lucide.Smile
import com.composables.icons.lucide.SmilePlus
import com.composables.icons.lucide.Snowflake
import com.composables.icons.lucide.Sofa
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Stethoscope
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.Sunrise
import com.composables.icons.lucide.Sunset
import com.composables.icons.lucide.TramFront
import com.composables.icons.lucide.Trees
import com.composables.icons.lucide.Trophy
import com.composables.icons.lucide.User
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.Utensils
import com.composables.icons.lucide.Wind
import com.composables.icons.lucide.Wine
import com.composables.icons.lucide.Zap

/**
 * Pool von Lucide-Symbolen die fuer Tagebucheintraege zur Verfuegung stehen.
 * Jedes Symbol hat einen stabilen Schluessel, eine deutsche Beschreibung (fuer den
 * KI-Klassifikator), und kategorie-spezifische Farben fuer Tag und Nacht.
 *
 * Lizenz: Lucide Icons (ISC) — alle Icons frei verwendbar.
 */
object LucideIconPool {

    data class IconEntry(
        val key: String,
        val icon: ImageVector,
        val description: String,
        val tintDark: Color,
        val tintLight: Color,
    )

    // === Farb-Paletten pro Kategorie ===
    // Format: (Dark, Light) — Dark fuer dunklen Hintergrund, Light fuer hellen
    private val sunColors = Color(0xFFFFB300) to Color(0xFFE65100)            // Bernstein/Tieforange
    private val moonColors = Color(0xFF7986CB) to Color(0xFF303F9F)           // Indigo
    private val cloudColors = Color(0xFFB0BEC5) to Color(0xFF455A64)          // Grau
    private val snowColors = Color(0xFF80DEEA) to Color(0xFF006064)           // Eiscyan
    private val waterColors = Color(0xFF4FC3F7) to Color(0xFF0277BD)          // Wasserblau
    private val natureColors = Color(0xFF81C784) to Color(0xFF2E7D32)         // Waldgruen
    private val sportColors = Color(0xFFC5E1A5) to Color(0xFF689F38)          // Limette/Olive
    private val trophyColors = Color(0xFFFFD54F) to Color(0xFFF57F17)         // Gold/Bronze
    private val foodColors = Color(0xFFFF8A65) to Color(0xFFD84315)           // Tomatenrot
    private val drinkWarmColors = Color(0xFFBCAAA4) to Color(0xFF6D4C41)      // Karamell/Braun
    private val travelColors = Color(0xFF4DD0E1) to Color(0xFF00838F)         // Tuerkis
    private val homeColors = Color(0xFFFFCC80) to Color(0xFFFB8C00)           // Sand/Karamell
    private val loveColors = Color(0xFFF48FB1) to Color(0xFFAD1457)           // Pink/Magenta
    private val petColors = Color(0xFFD7CCC8) to Color(0xFF5D4037)            // Beige/Braun
    private val workColors = Color(0xFFB39DDB) to Color(0xFF4527A0)           // Lila
    private val ideaColors = Color(0xFFFFF176) to Color(0xFFF9A825)           // Gelb/Bernstein
    private val learnColors = Color(0xFF90CAF9) to Color(0xFF1565C0)          // Blassblau/Mitteblau
    private val moodPositiveColors = Color(0xFFFFF59D) to Color(0xFFF9A825)   // Sonnengelb
    private val moodNegativeColors = Color(0xFF90A4AE) to Color(0xFF455A64)   // Blau-Grau
    private val moodNeutralColors = Color(0xFFBDBDBD) to Color(0xFF616161)    // Grau
    private val energyColors = Color(0xFFFFAB40) to Color(0xFFE65100)         // Orange
    private val healthColors = Color(0xFF80CBC4) to Color(0xFF00695C)         // Mintgruen
    private val artColors = Color(0xFFCE93D8) to Color(0xFF6A1B9A)            // Magenta-Lila
    private val miscColors = Color(0xFF80DEEA) to Color(0xFF006064)           // Standard-Cyan

    private fun e(
        key: String,
        icon: ImageVector,
        description: String,
        colors: Pair<Color, Color>,
    ) = IconEntry(key, icon, description, colors.first, colors.second)

    val all: List<IconEntry> = listOf(
        // === Tageszeit / Wetter ===
        e("sun", Lucide.Sun, "Sonne, Morgen, Tageslicht, sonniger Tag", sunColors),
        e("sunrise", Lucide.Sunrise, "Sonnenaufgang, frueher Morgen, Tagesanfang", sunColors),
        e("sunset", Lucide.Sunset, "Sonnenuntergang, Abenddaemmerung, Tagesende", sunColors),
        e("moon", Lucide.Moon, "Mond, Nacht, spaeter Abend", moonColors),
        e("moon_star", Lucide.MoonStar, "Sternennacht, Traum, magische Nachtstimmung", moonColors),
        e("cloud", Lucide.Cloud, "Wolken, bewoelkt, grauer Tag", cloudColors),
        e("cloud_rain", Lucide.CloudRain, "Regen, Nieselregen, Schauer", cloudColors),
        e("snowflake", Lucide.Snowflake, "Schnee, Winter, Kaelte", snowColors),
        e("wind", Lucide.Wind, "Wind, stuermisch, frische Luft", snowColors),

        // === Wasser / Hydrierung ===
        e("glass_water", Lucide.GlassWater, "Wasserglas, Wasser trinken, Hydrierung, Durst loeschen", waterColors),
        e("droplet", Lucide.Droplet, "Wassertropfen, Fluessigkeit, einzelner Tropfen", waterColors),
        e("droplets", Lucide.Droplets, "Mehrere Wassertropfen, Schwitzen, Feuchtigkeit, viel trinken", waterColors),

        // === Bewegung / Sport / Natur ===
        e("footprints", Lucide.Footprints, "Spaziergang, Wandern, Schritte gehen", sportColors),
        e("mountain", Lucide.Mountain, "Berge, Gipfel, Bergwanderung", natureColors),
        e("mountain_snow", Lucide.MountainSnow, "Schneebedeckte Berge, Skifahren, Hochgebirge", natureColors),
        e("trees", Lucide.Trees, "Wald, Baeume, Natur, gruener Bereich", natureColors),
        e("leaf", Lucide.Leaf, "Blatt, Pflanze, Botanik, Bio", natureColors),
        e("flower", Lucide.Flower2, "Blume, Garten, Bluete, Fruehling", natureColors),
        e("bike", Lucide.Bike, "Fahrrad, Radfahren, Radtour", sportColors),
        e("dumbbell", Lucide.Dumbbell, "Fitness, Krafttraining, Gym, Workout", sportColors),
        e("person_standing", Lucide.PersonStanding, "Sport, Bewegung, koerperliche Aktivitaet, Joggen, Laufen", sportColors),
        e("trophy", Lucide.Trophy, "Erfolg, gewonnen, Sieg, geschafft, Auszeichnung", trophyColors),

        // === Essen / Trinken ===
        e("coffee", Lucide.Coffee, "Kaffee, Cappuccino, Espresso, Morgenkaffee", drinkWarmColors),
        e("utensils", Lucide.Utensils, "Essen, Mahlzeit, Restaurant, Mittagessen, Abendessen", foodColors),
        e("pizza", Lucide.Pizza, "Pizza, italienisch, Snack", foodColors),
        e("wine", Lucide.Wine, "Wein, Weinabend, Anstossen", drinkWarmColors),
        e("beer", Lucide.Beer, "Bier, Pub, Kneipenabend", drinkWarmColors),
        e("apple", Lucide.Apple, "Apfel, Obst, gesundes Essen, Snack", foodColors),
        e("cake", Lucide.Cake, "Geburtstag, Torte, Feier mit Kuchen", foodColors),

        // === Reise / Verkehr ===
        e("plane", Lucide.Plane, "Flugzeug, Flug, Reise mit Flieger, Flughafen", travelColors),
        e("car", Lucide.Car, "Auto, Autofahrt, Fahrt mit dem PKW", travelColors),
        e("train", Lucide.TramFront, "Zug, Bahnfahrt, ICE, Pendeln", travelColors),
        e("ship", Lucide.Ship, "Schiff, Boot, Faehre, Kreuzfahrt", travelColors),
        e("anchor", Lucide.Anchor, "Hafen, Meer, See, Wasserausflug", travelColors),
        e("compass", Lucide.Compass, "Reise, Abenteuer, Orientierung, Erkundung", travelColors),
        e("map_pin", Lucide.MapPin, "Ort, Standort, neuer Platz besucht", travelColors),

        // === Haus / Soziales / Familie ===
        e("home", Lucide.House, "Zuhause, Wohnung, Haus, Heim", homeColors),
        e("sofa", Lucide.Sofa, "Couch, entspannen, Fernsehabend, gemuetlich", homeColors),
        e("bed", Lucide.Bed, "Bett, schlafen, Nachtruhe", homeColors),
        e("user", Lucide.User, "Allein, Selbstreflexion, Person, ich", loveColors),
        e("users", Lucide.Users, "Freunde, Gruppe, mehrere Personen, Treffen", loveColors),
        e("baby", Lucide.Baby, "Kind, Baby, Eltern werden, Familie mit Kindern", loveColors),
        e("heart", Lucide.Heart, "Liebe, Herz, romantisch, verliebt, Partner", loveColors),
        e("heart_handshake", Lucide.HeartHandshake, "Familie, Verbundenheit, Naehe, geliebte Menschen", loveColors),
        e("dog", Lucide.Dog, "Hund, Hundespaziergang, Haustier", petColors),
        e("cat", Lucide.Cat, "Katze, Haustier mit Katze", petColors),

        // === Arbeit / Geist / Lernen ===
        e("briefcase", Lucide.Briefcase, "Arbeit, Job, Buero, Beruf, Karriere, Termin", workColors),
        e("laptop", Lucide.Laptop, "Computer, Laptop, am Rechner gearbeitet", workColors),
        e("code", Lucide.Code, "Programmieren, Software, Coding, IT", workColors),
        e("lightbulb", Lucide.Lightbulb, "Idee, Geistesblitz, Inspiration, Einfall", ideaColors),
        e("brain", Lucide.Brain, "Nachdenken, reflektiert, philosophisch, gegruebelt, Konzentration, Fokus, Sprachkompetenz, Lernen, Geist", ideaColors),
        e("book_open", Lucide.BookOpen, "Lesen, Buch, Roman, Studium", learnColors),
        e("graduation_cap", Lucide.GraduationCap, "Lernen, Schule, Uni, Pruefung, Weiterbildung, Sprachkurs", learnColors),
        e("pencil", Lucide.Pencil, "Schreiben, Notiz, Tagebuch, festgehalten", learnColors),

        // === Stimmung / Gefuehle ===
        e("smile_plus", Lucide.SmilePlus, "Sehr glueklich, ueberglueklich, freudig, beglueckt", moodPositiveColors),
        e("smile", Lucide.Smile, "Glueklich, gut gelaunt, zufrieden, froh", moodPositiveColors),
        e("meh", Lucide.Meh, "Neutral, gleichgueltig, durchschnittlich, mittel", moodNeutralColors),
        e("frown", Lucide.Frown, "Traurig, niedergeschlagen, betruebt", moodNegativeColors),
        e("angry", Lucide.Angry, "Wuetend, zornig, Aerger, sauer, frustriert", moodNegativeColors),
        e("flame", Lucide.Flame, "Hitze, intensiv, brennend, leidenschaftlich, motiviert", energyColors),
        e("zap", Lucide.Zap, "Energie, Power, voller Schwung, dynamisch", energyColors),
        e("sparkles", Lucide.Sparkles, "Besonderer Moment, magisch, wunderschoen, beeindruckend", moodPositiveColors),
        e("star", Lucide.Star, "Highlight, herausragend, etwas Besonderes, top", moodPositiveColors),

        // === Gesundheit ===
        e("stethoscope", Lucide.Stethoscope, "Arzt, Untersuchung, medizinischer Termin, Gesundheit", healthColors),
        e("pill", Lucide.Pill, "Medikament, Tabletten, krank, Behandlung", healthColors),

        // === Kultur / Hobby ===
        e("music", Lucide.Music, "Musik, Lied, gehoert, Konzert, Instrument", artColors),
        e("mic", Lucide.Mic, "Mikrofon, eingesprochen, Voice, Aufnahme", artColors),
        e("camera", Lucide.Camera, "Foto, fotografiert, Kamera, Bild gemacht", artColors),
        e("palette", Lucide.Palette, "Malerei, Kunst, kreativ, gemalt", artColors),
        e("brush", Lucide.Brush, "Pinsel, Zeichnen, Skizze, Kunsthandwerk", artColors),
        e("film", Lucide.Film, "Film, Kino, Serie, geguckt, Netflix", artColors),
        e("gamepad", Lucide.Gamepad2, "Gaming, Videospiel, gezockt, Konsole", artColors),

        // === Diverses ===
        e("gift", Lucide.Gift, "Geschenk, ueberrascht worden, etwas geschenkt", loveColors),
        e("shopping_bag", Lucide.ShoppingBag, "Einkaufen, Shopping, gekauft, Laden besucht", miscColors),
        e("phone", Lucide.Phone, "Telefonat, Anruf, telefoniert", miscColors),
        e("calendar", Lucide.Calendar, "Termin, Datum, geplant, im Kalender", miscColors),
        e("clock", Lucide.Clock, "Zeit, Uhrzeit, Dauer, gewartet", miscColors),
        e("alarm_clock", Lucide.AlarmClock, "Wecker, frueh aufgestanden, Wachgeworden", miscColors),
    )

    val byKey: Map<String, IconEntry> = all.associateBy { it.key }

    fun get(key: String?): IconEntry? = key?.let { byKey[it] }
    fun getIcon(key: String?): ImageVector = get(key)?.icon ?: Lucide.Pencil

    val fallbackKey: String = "pencil"
    val voiceKey: String = "mic"
}

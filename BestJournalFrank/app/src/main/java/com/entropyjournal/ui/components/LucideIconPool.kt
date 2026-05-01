package com.entropyjournal.ui.components

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
import com.composables.icons.lucide.Dumbbell
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Flower2
import com.composables.icons.lucide.Footprints
import com.composables.icons.lucide.Frown
import com.composables.icons.lucide.Gamepad2
import com.composables.icons.lucide.Gift
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
 * Jedes Symbol hat einen stabilen Schluessel (fuer Cache + KI-Auswahl) und eine
 * deutsche Beschreibung die der KI hilft das richtige Symbol zu waehlen.
 *
 * Lizenz: Lucide Icons (ISC) — alle Icons frei verwendbar, MIT-kompatibel.
 */
object LucideIconPool {

    data class IconEntry(
        val key: String,
        val icon: ImageVector,
        val description: String,
    )

    val all: List<IconEntry> = listOf(
        // === Tageszeit / Wetter ===
        IconEntry("sun", Lucide.Sun, "Sonne, Morgen, Tageslicht, sonniger Tag"),
        IconEntry("sunrise", Lucide.Sunrise, "Sonnenaufgang, frueher Morgen, Tagesanfang"),
        IconEntry("sunset", Lucide.Sunset, "Sonnenuntergang, Abenddaemmerung, Tagesende"),
        IconEntry("moon", Lucide.Moon, "Mond, Nacht, spaeter Abend"),
        IconEntry("moon_star", Lucide.MoonStar, "Sternennacht, Traum, magische Nachtstimmung"),
        IconEntry("cloud", Lucide.Cloud, "Wolken, bewoelkt, grauer Tag"),
        IconEntry("cloud_rain", Lucide.CloudRain, "Regen, Nieselregen, Schauer"),
        IconEntry("snowflake", Lucide.Snowflake, "Schnee, Winter, Kaelte"),
        IconEntry("wind", Lucide.Wind, "Wind, stuermisch, frische Luft"),

        // === Bewegung / Sport / Natur ===
        IconEntry("footprints", Lucide.Footprints, "Spaziergang, Wandern, Schritte gehen"),
        IconEntry("mountain", Lucide.Mountain, "Berge, Gipfel, Bergwanderung"),
        IconEntry("mountain_snow", Lucide.MountainSnow, "Schneebedeckte Berge, Skifahren, Hochgebirge"),
        IconEntry("trees", Lucide.Trees, "Wald, Baeume, Natur, gruener Bereich"),
        IconEntry("leaf", Lucide.Leaf, "Blatt, Pflanze, Botanik, Bio"),
        IconEntry("flower", Lucide.Flower2, "Blume, Garten, Bluete, Fruehling"),
        IconEntry("bike", Lucide.Bike, "Fahrrad, Radfahren, Radtour"),
        IconEntry("dumbbell", Lucide.Dumbbell, "Fitness, Krafttraining, Gym, Workout"),
        IconEntry("person_standing", Lucide.PersonStanding, "Sport, Bewegung, koerperliche Aktivitaet"),
        IconEntry("trophy", Lucide.Trophy, "Erfolg, gewonnen, Sieg, geschafft, Auszeichnung"),

        // === Essen / Trinken ===
        IconEntry("coffee", Lucide.Coffee, "Kaffee, Cappuccino, Espresso, Morgenkaffee"),
        IconEntry("utensils", Lucide.Utensils, "Essen, Mahlzeit, Restaurant, Mittagessen, Abendessen"),
        IconEntry("pizza", Lucide.Pizza, "Pizza, italienisch, Snack"),
        IconEntry("wine", Lucide.Wine, "Wein, Weinabend, Anstossen"),
        IconEntry("beer", Lucide.Beer, "Bier, Pub, Kneipenabend"),
        IconEntry("apple", Lucide.Apple, "Apfel, Obst, gesundes Essen, Snack"),
        IconEntry("cake", Lucide.Cake, "Geburtstag, Torte, Feier mit Kuchen"),

        // === Reise / Verkehr ===
        IconEntry("plane", Lucide.Plane, "Flugzeug, Flug, Reise mit Flieger, Flughafen"),
        IconEntry("car", Lucide.Car, "Auto, Autofahrt, Fahrt mit dem PKW"),
        IconEntry("train", Lucide.TramFront, "Zug, Bahnfahrt, ICE, Pendeln"),
        IconEntry("ship", Lucide.Ship, "Schiff, Boot, Faehre, Kreuzfahrt"),
        IconEntry("anchor", Lucide.Anchor, "Hafen, Meer, See, Wasserausflug"),
        IconEntry("compass", Lucide.Compass, "Reise, Abenteuer, Orientierung, Erkundung"),
        IconEntry("map_pin", Lucide.MapPin, "Ort, Standort, neuer Platz besucht"),

        // === Haus / Soziales / Familie ===
        IconEntry("home", Lucide.House, "Zuhause, Wohnung, Haus, Heim"),
        IconEntry("sofa", Lucide.Sofa, "Couch, entspannen, Fernsehabend, gemuetlich"),
        IconEntry("bed", Lucide.Bed, "Bett, schlafen, Nachtruhe"),
        IconEntry("user", Lucide.User, "Allein, Selbstreflexion, Person, ich"),
        IconEntry("users", Lucide.Users, "Freunde, Gruppe, mehrere Personen, Treffen"),
        IconEntry("baby", Lucide.Baby, "Kind, Baby, Eltern werden, Familie mit Kindern"),
        IconEntry("heart", Lucide.Heart, "Liebe, Herz, romantisch, verliebt, Partner"),
        IconEntry("heart_handshake", Lucide.HeartHandshake, "Familie, Verbundenheit, Naehe, geliebte Menschen"),
        IconEntry("dog", Lucide.Dog, "Hund, Hundespaziergang, Haustier"),
        IconEntry("cat", Lucide.Cat, "Katze, Haustier mit Katze"),

        // === Arbeit / Geist / Lernen ===
        IconEntry("briefcase", Lucide.Briefcase, "Arbeit, Job, Buero, Beruf, Karriere, Termin"),
        IconEntry("laptop", Lucide.Laptop, "Computer, Laptop, am Rechner gearbeitet"),
        IconEntry("code", Lucide.Code, "Programmieren, Software, Coding, IT"),
        IconEntry("lightbulb", Lucide.Lightbulb, "Idee, Geistesblitz, Inspiration, Einfall"),
        IconEntry("brain", Lucide.Brain, "Nachdenken, reflektiert, philosophisch, gegruebelt"),
        IconEntry("book_open", Lucide.BookOpen, "Lesen, Buch, Roman, Studium"),
        IconEntry("graduation_cap", Lucide.GraduationCap, "Lernen, Schule, Uni, Pruefung, Weiterbildung"),
        IconEntry("pencil", Lucide.Pencil, "Schreiben, Notiz, Tagebuch, festgehalten"),

        // === Stimmung / Gefuehle ===
        IconEntry("smile_plus", Lucide.SmilePlus, "Sehr glueklich, ueberglueklich, freudig, beglueckt"),
        IconEntry("smile", Lucide.Smile, "Glueklich, gut gelaunt, zufrieden, froh"),
        IconEntry("meh", Lucide.Meh, "Neutral, gleichgueltig, durchschnittlich, mittel"),
        IconEntry("frown", Lucide.Frown, "Traurig, niedergeschlagen, betruebt"),
        IconEntry("angry", Lucide.Angry, "Wuetend, zornig, Aerger, sauer, frustriert"),
        IconEntry("flame", Lucide.Flame, "Hitze, intensiv, brennend, leidenschaftlich, motiviert"),
        IconEntry("zap", Lucide.Zap, "Energie, Power, voller Schwung, dynamisch"),
        IconEntry("sparkles", Lucide.Sparkles, "Besonderer Moment, magisch, wunderschoen, beeindruckend"),
        IconEntry("star", Lucide.Star, "Highlight, herausragend, etwas Besonderes, top"),

        // === Gesundheit ===
        IconEntry("stethoscope", Lucide.Stethoscope, "Arzt, Untersuchung, medizinischer Termin, Gesundheit"),
        IconEntry("pill", Lucide.Pill, "Medikament, Tabletten, krank, Behandlung"),

        // === Kultur / Hobby ===
        IconEntry("music", Lucide.Music, "Musik, Lied, gehoert, Konzert, Instrument"),
        IconEntry("mic", Lucide.Mic, "Mikrofon, eingesprochen, Voice, Aufnahme"),
        IconEntry("camera", Lucide.Camera, "Foto, fotografiert, Kamera, Bild gemacht"),
        IconEntry("palette", Lucide.Palette, "Malerei, Kunst, kreativ, gemalt"),
        IconEntry("brush", Lucide.Brush, "Pinsel, Zeichnen, Skizze, Kunsthandwerk"),
        IconEntry("film", Lucide.Film, "Film, Kino, Serie, geguckt, Netflix"),
        IconEntry("gamepad", Lucide.Gamepad2, "Gaming, Videospiel, gezockt, Konsole"),

        // === Diverses ===
        IconEntry("gift", Lucide.Gift, "Geschenk, ueberrascht worden, etwas geschenkt"),
        IconEntry("shopping_bag", Lucide.ShoppingBag, "Einkaufen, Shopping, gekauft, Laden besucht"),
        IconEntry("phone", Lucide.Phone, "Telefonat, Anruf, telefoniert"),
        IconEntry("calendar", Lucide.Calendar, "Termin, Datum, geplant, im Kalender"),
        IconEntry("clock", Lucide.Clock, "Zeit, Uhrzeit, Dauer, gewartet"),
        IconEntry("alarm_clock", Lucide.AlarmClock, "Wecker, frueh aufgestanden, Wachgeworden"),
    )

    /** Schluessel-zu-Eintrag-Index fuer schnellen Lookup. */
    val byKey: Map<String, IconEntry> = all.associateBy { it.key }

    /** Liefert das passende Symbol oder Pencil als Fallback. */
    fun get(key: String?): ImageVector =
        key?.let { byKey[it]?.icon } ?: Lucide.Pencil

    /** Standard-Fallback wenn weder Stichwort noch KI ein Icon vorschlagen. */
    val fallbackKey: String = "pencil"

    /** Voice-Eintrag-Default (wenn nichts gefunden wird). */
    val voiceKey: String = "mic"
}

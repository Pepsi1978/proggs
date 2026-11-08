package de.frank.jarvis.faehigkeit

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import de.frank.jarvis.Hintergrundstart
import de.frank.jarvis.MainActivity
import de.frank.jarvis.ablage.AblageSuche
import de.frank.jarvis.fahrt.Navigation
import de.frank.jarvis.fahrt.Orte
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Alltagsbefehle direkt auf dem Handy: „Starte App XYZ“ öffnet jede installierte, startbare App, „Starte Maps und navigiere
 * mich zu …“ startet in Google Maps sofort die Autonavigation vom aktuellen Standort. Läuft in Jarvis selbst, ohne Rechner
 * und ohne Programmierauftrag; nur im Gespräch mit Frank, nicht über ChatGPT und nicht in Hintergrund-Agenten.
 * Der Schnellstart auf dem Startbildschirm bleibt davon unberührt.
 */
class AppStartFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "appstart"
    override val name = "Apps und Navigation"
    override val beschreibung = "Öffnet installierte Apps auf dem Handy und startet die Autonavigation in Google Maps."
    override val hinweise =
        "„Starte/Öffne App X“ → app_starten mit dem genannten Namen. „Starte Maps und navigiere mich zu …“, „Navigiere mich nach Hause“ → nur navigation_starten " +
            "(startet die Navigation sofort, kein zusätzliches app_starten, keine Fahrzeit-Abfrage, außer Frank will ausdrücklich die Fahrzeit). " +
            "Beides führst du selbst aus: nie programmieren, Handy-Use, einen Agenten oder einen Rechner dafür. Jarvis Mac OS und Jarvis Windows HP bleiben bei ihren eigenen Start-Werkzeugen. " +
            "Nur wenn das Werkzeug MEHRDEUTIG oder UNSICHER meldet, fragst du mit den genannten Kandidaten nach; Ziele ohne gespeicherte Adresse („zu Mama“, „zum Arzt“) und unvollständige Adressen nicht raten, sondern nachfragen. " +
            "Melde Frank genau den Stand, den das Werkzeug nennt: „geöffnet“ nur, wenn es das bestätigt, und einen gegebenen Startbefehl nie wiederholen."

    override val imPlugin: Boolean get() = false
    override val nurImGespraech: Boolean get() = true

    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "app_starten",
            titel = "App starten",
            beschreibung = "Jarvis: öffnet eine installierte App auf Franks Handy anhand ihres Namens (ungefähr reicht). " +
                "Beispiele: „Starte Spotify“ → name Spotify. „Öffne die Kamera“ → name Kamera. „Starte Geniale Aufgaben“ → name Geniale Aufgaben.",
            schema = schema("name" to text("Name der App, wie Frank ihn sagt."), pflicht = listOf("name")),
            nurLesen = false,
            taetigkeit = "öffnet eine App",
        ) { a -> appStarten(a.optString("name")) },
        Werkzeug(
            name = "navigation_starten",
            titel = "Navigation starten",
            beschreibung = "Jarvis: startet in Google Maps sofort die Autonavigation vom aktuellen Standort zum Ziel (keine Routenvorschau). " +
                "ziel: „zuhause“, „arbeit“, „papa“ (gespeicherte Adressen) oder genau die angesagte Adresse bzw. der Ort, nichts dazuerfinden. " +
                "Beispiele: „Starte Maps und navigiere mich zum Alexanderplatz“ → ziel Alexanderplatz. „Navigiere mich nach Hause“ → ziel zuhause. „Fahr mich zu Papa“ → ziel papa.",
            schema = schema("ziel" to text("„zuhause“, „arbeit“, „papa“ oder die angesagte Adresse bzw. der Ort."), pflicht = listOf("ziel")),
            nurLesen = false,
            taetigkeit = "startet die Navigation",
        ) { a -> navigieren(a.optString("ziel")) },
    )

    private class Kandidat(val paket: String, val activity: String, val name: String, val schluessel: String)

    /** Ergebnis der Suche; [sicher] = false heißt: nur ein ungefährer Treffer, Frank fragen statt öffnen. */
    private class Treffer(val liste: List<Kandidat>, val sicher: Boolean = true)

    private suspend fun appStarten(gesagt: String): Ergebnis {
        val anfragen = anfragen(gesagt)
        if (anfragen.isEmpty()) return Ergebnis("Kein App-Name angegeben. Frag Frank, welche App er öffnen will.", fehler = true)
        val alle = startbare()
        if (alle.isEmpty()) return Ergebnis("Jarvis kann die installierten Apps gerade nicht lesen.", fehler = true)
        val t = auswahl(alle, anfragen)
        // Mehrere Einträge derselben App mit demselben Namen sind keine echte Auswahl.
        val liste = t.liste.distinctBy { it.paket to it.schluessel }
        return when {
            liste.isEmpty() -> Ergebnis("Keine installierte App passt zu „$gesagt“. Sag Frank ehrlich, dass Jarvis sie nicht gefunden hat.", fehler = true)
            liste.size > 1 -> Ergebnis("MEHRDEUTIG: Zu „$gesagt“ passen ${nennen(liste)}. Frag Frank kurz, welche er meint.", fehler = true)
            !t.sicher -> Ergebnis("UNSICHER: Keine App heißt „$gesagt“, am ähnlichsten ist „${liste[0].name}“. Frag Frank, ob er die meint; nichts geöffnet.", fehler = true)
            else -> liste[0].let { k ->
                val start = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(k.paket, k.activity)
                    .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                oeffnen(start, "„${k.name}“")
            }
        }
    }

    /** Gleich lautende Namen bekommen das Paket dazu, damit Frank sie unterscheiden kann. */
    private fun nennen(liste: List<Kandidat>): String = liste.joinToString { k ->
        if (liste.count { it.schluessel == k.schluessel } > 1) "„${k.name}“ (${k.paket})" else "„${k.name}“"
    }

    private suspend fun navigieren(gesagt: String): Ergebnis {
        val benannt = Orte.benannt(context, gesagt)
        val ziel = benannt ?: gesagt.trim()
        if (ziel.isBlank()) return Ergebnis("Kein Ziel angegeben. Frag Frank, wohin er navigiert werden will.", fehler = true)
        if (benannt == null && Orte.unklar(gesagt))
            return Ergebnis("Für „${gesagt.trim()}“ ist keine Adresse gespeichert. Nichts gestartet; frag Frank nach der Adresse.", fehler = true)
        if (context.packageManager.getLaunchIntentForPackage(Navigation.MAPS) == null) return Ergebnis("Google Maps ist auf dem Handy nicht installiert oder deaktiviert.", fehler = true)
        return oeffnen(Navigation.intent(ziel), "Die Navigation nach $ziel", "Google Maps ist vorn und startet die Autonavigation vom aktuellen Standort nach $ziel.")
    }

    /**
     * Öffnet und meldet nur, was feststeht. Bestätigt ist das Öffnen nur, wenn Jarvis vorn war und danach verschwindet. Gesperrtes
     * Handy, geteilter Bildschirm, Start aus dem Hintergrund und Zeitablauf melden „Startbefehl gegeben“, ohne Fehler, damit das
     * Modell nicht ein zweites Mal startet.
     */
    private suspend fun oeffnen(intent: Intent, was: String, erfolg: String = "$was ist jetzt vorn geöffnet."): Ergebnis {
        val vorn = MainActivity.sichtbar
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: ActivityNotFoundException) { return Ergebnis("$was ließ sich nicht öffnen: Android kennt kein passendes Ziel.", fehler = true) }
        catch (_: SecurityException) { return Ergebnis("$was ließ sich nicht öffnen: Android hat es verweigert.", fehler = true) }
        val nichtWiederholen = " Sag Frank genau das und wiederhole den Aufruf nicht."
        val gesperrt = context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
        return when {
            gesperrt -> Ergebnis("Startbefehl für $was ist gegeben, das Handy ist aber gesperrt: Nach dem Entsperren ist es vorn.$nichtWiederholen")
            !vorn -> Ergebnis("Startbefehl für $was ist gegeben. Jarvis lief im Hintergrund und kann nicht sehen, ob es vorn ist" +
                (if (Hintergrundstart.ueberlagernErlaubt(context)) "" else "; ohne „Über anderen Apps einblenden“ (in Jarvis unter Einrichtung) verwirft Android solche Starts oft still") +
                ".$nichtWiederholen")
            MainActivity.geteilt -> Ergebnis("Startbefehl für $was ist gegeben. Jarvis ist im geteilten Bildschirm und kann nicht sehen, ob es aufging.$nichtWiederholen")
            withTimeoutOrNull(6_000) { MainActivity.sichtbarFluss.first { !it } } != null -> Ergebnis(erfolg)
            else -> Ergebnis("Startbefehl für $was ist gegeben, aber Jarvis ist nach 6 Sekunden noch vorn: nicht bestätigt.$nichtWiederholen")
        }
    }

    /** Das Gesagte in Suchschlüsseln: wörtlich, ohne Artikel und ohne Markenvorsatz („Google Maps“ → „maps“). */
    private fun anfragen(gesagt: String): List<String> {
        val ohneApp = gesagt.replace(Regex("(?i)(?<!\\p{L})apps?(?!\\p{L})"), " ")
        val ohneArtikel = schluessel(ohneApp.replace(Regex("(?i)(?<!\\p{L})(die|der|das|den)(?!\\p{L})"), " "))
        val ohneMarke = MARKEN.firstOrNull { ohneArtikel.startsWith(it) && ohneArtikel.length > it.length + 1 }?.let { ohneArtikel.removePrefix(it) }
        return listOfNotNull(schluessel(ohneApp), ohneArtikel, ohneMarke).filter { it.isNotBlank() }.distinct()
    }

    /** Namen je Launcher-Eintrag; neu gelesen wird nur, was neu oder aktualisiert ist (loadLabel ist teuer). */
    private val namen = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun startbare(): List<Kandidat> {
        val pm = context.packageManager
        val start = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching { pm.queryIntentActivities(start, 0) }.getOrDefault(emptyList())
            .filter { it.activityInfo.packageName != context.packageName }
            .map { r ->
                val a = r.activityInfo
                val n = namen.getOrPut("${a.packageName}/${a.name}@${a.applicationInfo?.sourceDir}") { r.loadLabel(pm).toString() }
                Kandidat(a.packageName, a.name, n, schluessel(n))
            }
            .filter { it.schluessel.isNotBlank() }
    }

    /**
     * Stufen, jede über alle Anfragen: genau gleich, Name beginnt so, Name enthält es, Name steckt im Gesagten (zuerst am Ende des
     * Gesagten, dann der längste), zuletzt ein kleiner Hörfehler, der nur bei genau einem nahen Namen ab fünf Zeichen sicher gilt.
     */
    private fun auswahl(alle: List<Kandidat>, qs: List<String>): Treffer {
        fun stufe(passt: (Kandidat, String) -> Boolean): List<Kandidat>? =
            qs.firstNotNullOfOrNull { q -> alle.filter { passt(it, q) }.takeIf { it.isNotEmpty() } }
        stufe { k, q -> k.schluessel == q }?.let { return Treffer(it) }
        stufe { k, q -> k.schluessel.startsWith(q) }?.let { return Treffer(it) }
        stufe { k, q -> q.length >= 3 && q in k.schluessel }?.let { return Treffer(it) }
        for (q in qs) {
            val drin = alle.filter { it.schluessel.length >= 3 && it.schluessel in q }
            if (drin.isEmpty()) continue
            val amEnde = drin.filter { q.endsWith(it.schluessel) }.ifEmpty { drin }
            val max = amEnde.maxOf { it.schluessel.length }
            return Treffer(amEnde.filter { it.schluessel.length == max })
        }
        val q = qs.last()
        val grenze = (q.length / 4).coerceIn(1, 3)
        val nah = alle.map { it to AblageSuche.abstand(it.schluessel, q, grenze) }.filter { it.second <= grenze }
        val best = nah.minOfOrNull { it.second } ?: return Treffer(emptyList())
        val liste = nah.filter { it.second == best }.map { it.first }
        return Treffer(liste, sicher = best <= 1 && q.length >= 5)
    }

    private fun schluessel(t: String): String = t.lowercase(Locale.GERMAN).filter { it.isLetterOrDigit() }

    private companion object { val MARKEN = listOf("google", "samsung", "microsoft") }
}

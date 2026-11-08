package de.frank.jarvis.faehigkeit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import de.frank.jarvis.data.Einstellungen
import java.util.Locale

/**
 * Alltagsbefehle direkt auf dem Handy: „Starte App XYZ“ öffnet jede installierte, startbare App, „Starte Maps und navigiere
 * mich zu …“ startet in Google Maps sofort die Autonavigation vom aktuellen Standort. Läuft in Jarvis selbst, ohne Rechner
 * und ohne Programmierauftrag. Der Schnellstart auf dem Startbildschirm bleibt davon unberührt.
 */
class AppStartFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "appstart"
    override val name = "Apps und Navigation"
    override val beschreibung = "Öffnet installierte Apps auf dem Handy und startet die Autonavigation in Google Maps."
    override val hinweise =
        "„Starte/Öffne App X“ → app_starten mit dem genannten Namen. „Starte Maps und navigiere mich zu …“, „Navigiere mich nach Hause“ → nur navigation_starten " +
            "(startet die Navigation sofort, kein zusätzliches app_starten). Beides führst du selbst aus: nie programmieren, Handy-Use, einen Agenten oder einen Rechner dafür. " +
            "Jarvis Mac OS und Jarvis Windows HP bleiben bei ihren eigenen Start-Werkzeugen. Nur wenn das Werkzeug MEHRDEUTIG meldet, fragst du mit den genannten Kandidaten nach. " +
            "Navigation nie mit fahrzeit_lesen verbinden, außer Frank fragt ausdrücklich nach der Fahrzeit. Unklare Ziele (Personen oder Orte ohne gespeicherte Adresse wie „zu Mama“, " +
            "„zum Arzt“, unvollständige Adressen) nicht raten, sondern kurz nachfragen. Melde Frank genau den Stand, den das Werkzeug nennt: „geöffnet“ nur, wenn es das bestätigt."

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
                "ziel: „zuhause“, „arbeit“, „papa“ (gespeicherte Adressen) oder die angesagte Adresse bzw. der Ort. " +
                "Beispiele: „Starte Maps und navigiere mich zum Alexanderplatz“ → ziel Alexanderplatz, Berlin. „Navigiere mich nach Hause“ → ziel zuhause. „Fahr mich zu Papa“ → ziel papa.",
            schema = schema("ziel" to text("„zuhause“, „arbeit“, „papa“ oder eine Adresse bzw. ein Ort."), pflicht = listOf("ziel")),
            nurLesen = false,
            taetigkeit = "startet die Navigation",
        ) { a -> navigieren(a.optString("ziel")) },
    )

    private class Kandidat(val paket: String, val name: String, val schluessel: String)

    private suspend fun appStarten(gesagt: String): Ergebnis {
        val gesucht = schluessel(gesagt.replace(Regex("(?i)(?<!\\p{L})(die|der|das|app)(?!\\p{L})"), " "))
        if (gesucht.isBlank()) return Ergebnis("Kein App-Name angegeben. Frag Frank, welche App er öffnen will.", fehler = true)
        val alle = startbare()
        if (alle.isEmpty()) return Ergebnis("Jarvis kann die installierten Apps gerade nicht lesen.", fehler = true)
        val treffer = auswahl(alle, gesucht)
        return when {
            treffer.isEmpty() -> Ergebnis("Keine installierte App passt zu „$gesagt“. Sag Frank ehrlich, dass Jarvis sie nicht gefunden hat.", fehler = true)
            treffer.size > 1 -> Ergebnis("MEHRDEUTIG: Zu „$gesagt“ passen ${treffer.joinToString { "„${it.name}“" }}. Frag Frank kurz, welche er meint.", fehler = true)
            else -> treffer[0].let { k ->
                val intent = context.packageManager.getLaunchIntentForPackage(k.paket)
                    ?: return Ergebnis("„${k.name}“ lässt sich nicht starten.", fehler = true)
                oeffnen(intent, "„${k.name}“", "„${k.name}“ ist jetzt vorn geöffnet.")
            }
        }
    }

    private suspend fun navigieren(gesagt: String): Ergebnis {
        val ziel = Orte.benannt(context, gesagt) ?: gesagt.trim()
        if (ziel.isBlank()) return Ergebnis("Kein Ziel angegeben. Frag Frank, wohin er navigiert werden will.", fehler = true)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${Uri.encode(ziel)}&mode=d")).setPackage(MAPS)
        if (context.packageManager.getLaunchIntentForPackage(MAPS) == null) return Ergebnis("Google Maps ist auf dem Handy nicht installiert oder deaktiviert.", fehler = true)
        return oeffnen(intent, "Google Maps für die Navigation nach $ziel", "Google Maps ist vorn und startet die Autonavigation vom aktuellen Standort nach $ziel.")
    }

    /**
     * Öffnet und meldet nur, was feststeht. Aus dem Hintergrund verwirft Android den Start ohne Fehler, wenn Jarvis nicht
     * „Über anderen Apps einblenden“ darf: dann gar nicht erst versuchen. War Jarvis vorn, bestätigt sein Verschwinden das Öffnen;
     * aus dem Hintergrund sieht Jarvis die andere App nicht und sagt das auch so.
     */
    private suspend fun oeffnen(intent: Intent, was: String, bestaetigt: String): Ergebnis {
        val vorn = de.frank.jarvis.MainActivity.sichtbar
        if (!vorn && android.os.Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context))
            return Ergebnis("$was ist nicht geöffnet: Jarvis läuft im Hintergrund und darf nicht „Über anderen Apps einblenden“, darum blockiert Android das Öffnen. " +
                "Frank kann es in Jarvis unter Einrichtung erlauben oder Jarvis vorher öffnen.", fehler = true)
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: ActivityNotFoundException) { return Ergebnis("$was ließ sich nicht öffnen: Android kennt kein passendes Ziel.", fehler = true) }
        catch (e: SecurityException) { return Ergebnis("$was ließ sich nicht öffnen: ${e.message ?: "Android hat es verweigert"}.", fehler = true) }
        if (!vorn) return Ergebnis("Startbefehl für $was ist gegeben. Jarvis lief im Hintergrund und kann nicht sehen, ob es vorn ist; sag Frank genau das.")
        repeat(25) { kotlinx.coroutines.delay(100); if (!de.frank.jarvis.MainActivity.sichtbar) return Ergebnis(bestaetigt) }
        return Ergebnis("$was ist nicht bestätigt geöffnet: Jarvis ist nach 2,5 Sekunden noch vorn.", fehler = true)
    }

    private fun startbare(): List<Kandidat> {
        val pm = context.packageManager
        val start = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching { pm.queryIntentActivities(start, 0) }.getOrDefault(emptyList())
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { val n = it.loadLabel(pm).toString(); Kandidat(it.activityInfo.packageName, n, schluessel(n)) }
            .filter { it.schluessel.isNotBlank() }
    }

    /** Genau gleich vor „Name beginnt so“ vor „enthält“ vor „Name steckt im Gesagten“ (längster) vor kleinem Hörfehler. */
    private fun auswahl(alle: List<Kandidat>, q: String): List<Kandidat> {
        alle.filter { it.schluessel == q }.takeIf { it.isNotEmpty() }?.let { return it }
        alle.filter { it.schluessel.startsWith(q) }.takeIf { it.isNotEmpty() }?.let { return it }
        if (q.length >= 3) alle.filter { q in it.schluessel }.takeIf { it.isNotEmpty() }?.let { return it }
        alle.filter { it.schluessel.length >= 3 && it.schluessel in q }.takeIf { it.isNotEmpty() }
            ?.let { l -> val max = l.maxOf { it.schluessel.length }; return l.filter { it.schluessel.length == max } }
        val grenze = (q.length / 4).coerceIn(1, 3)
        val nah = alle.map { it to abstand(it.schluessel, q) }.filter { it.second <= grenze }
        val best = nah.minOfOrNull { it.second } ?: return emptyList()
        return nah.filter { it.second == best }.map { it.first }
    }

    private fun schluessel(t: String): String = t.lowercase(Locale.GERMAN).filter { it.isLetterOrDigit() }

    private fun abstand(a: String, b: String): Int {
        var vorher = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val jetzt = IntArray(b.length + 1); jetzt[0] = i
            for (j in 1..b.length) jetzt[j] = minOf(jetzt[j - 1] + 1, vorher[j] + 1, vorher[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            vorher = jetzt
        }
        return vorher[b.length]
    }

    private companion object { const val MAPS = "com.google.android.apps.maps" }
}

/** Franks benannte Orte: Zuhause, Arbeit und Papa aus den Einstellungen; alles andere ist eine Adresse oder ein Ort. */
object Orte {
    fun benannt(context: Context, text: String): String? {
        val e = Einstellungen.get(context)
        val t = text.trim().lowercase(Locale.GERMAN).removePrefix("zum ").removePrefix("zur ").removePrefix("zu ").removePrefix("nach ").trim()
        return when (t) {
            "arbeit", "auf arbeit", "dienst", "museumsinsel" -> e.adresseArbeit
            "zuhause", "hause", "zu hause", "heim", "wohnung", "daheim" -> e.adresseZuhause
            "papa", "papas", "papa nach hause", "meinem papa", "meinem vater", "vater", "papi" -> e.adressePapa
            else -> null
        }
    }
}

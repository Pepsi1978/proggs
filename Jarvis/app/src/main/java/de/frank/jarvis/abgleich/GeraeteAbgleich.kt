package de.frank.jarvis.abgleich

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.gedaechtnis.Bereich
import de.frank.jarvis.gedaechtnis.Lagebild
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Hält die Daten der angebundenen Apps auf Franks Geräten gleich (Handy ↔ Tablet). Jarvis ist auf jedem Gerät der Abgleicher:
 * Die Apps selbst sprechen mit keinem Server, sie geben über ihre Brücke nur ihren ganzen Stand als Paket heraus und spielen
 * ein Paket ein (Gegenstück: `bruecke/GeraeteAbgleich.kt` in jeder App).
 *
 * Transport ist der Geräteabgleich-Speicher am Jarvis-Server (`/j/<Geheimnis>/abgleich/…`, dieselbe Technik wie die Ablage):
 * je App ein Dokument `appdaten/<kennung>` mit der Prüfsumme des Inhalts, das Paket selbst als Datei. Der Server meldet
 * jede Änderung sofort als Signal „geraeteabgleich“ ans andere Gerät.
 *
 * Regeln, damit nichts verloren geht:
 * - **Es gilt der ganze Stand, der jüngere gewinnt.** Kein zeilenweises Mischen (hochgezählte Kennungen kollidierten).
 * - **Erstquelle ist das Handy.** Ein Gerät, das eine App noch nie abgeglichen hat, lädt nichts hoch, solange es nicht das
 *   Handy ist: Sonst überschriebe ein frisch installiertes, leeres Tablet den echten Stand.
 * - **Hochgeladen wird nur neuer Inhalt.** Verglichen wird die Prüfsumme über die Tabelleninhalte, nicht die Datei. Ein
 *   eingespielter Stand gilt als abgeglichen und geht nie zurück (kein Hin und Her).
 * - **Haben beide Geräte seit dem letzten Abgleich geändert**, gewinnt die jüngere Änderung (maßgeblich ist, wann Jarvis die
 *   Änderung bemerkt hat, nicht die Dateizeit); der unterlegene Stand bleibt auf seinem Gerät als Rückfall liegen (das macht die
 *   App vor jedem Einspielen) und steht im Logbuch.
 * - **Ein leerer Stand geht nie hoch.** Ist die App hier leer (neu installiert, Daten gelöscht), holt Jarvis den Stand des Servers zurück.
 * - **Passt die App-Fassung nicht** (das andere Gerät hat ein neueres Datenbank-Schema), wird weder eingespielt noch
 *   hochgeladen, bis die App hier aktualisiert ist.
 * - Nie während eines Sprachgesprächs (Sprachmodus vor Hintergrundarbeit).
 */
object GeraeteAbgleich {
    private class App(val kennung: String, val name: String, val adresse: Uri, val bereiche: List<Bereich>)

    private val APPS = listOf(
        App("aufgaben", "Geniale Aufgaben", Uri.parse("content://de.frank.aufgaben.jarvis"), listOf(Bereich.AUFGABEN)),
        App("ziele", "Ziele", Uri.parse("content://de.frank.ziele.jarvis"), listOf(Bereich.ZIELE)),
        App("ideen", "Geniale Ideen", Uri.parse("content://de.frank.genialeideen.jarvis"), listOf(Bereich.IDEEN)),
        App("epigenetik", "Epigenetik", Uri.parse("content://de.frank.epigenetik.jarvis"), emptyList()),
        App("stacklabor", "Stack-Labor", Uri.parse("content://de.frank.stacklabor.werftstudio.jarvis"), listOf(Bereich.STACKS)),
    )
    private const val ART = "appdaten"
    private const val ABSTAND_MS = 10 * 60_000L

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val anstoss = Channel<Unit>(Channel.CONFLATED)
    private var schleife: Job? = null
    private val beobachter = HashMap<Uri, ContentObserver>()
    private val handler by lazy { Handler(HandlerThread("jarvis-geraeteabgleich").apply { start() }.looper) }
    private val netz = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    private val JSON = "application/json".toMediaType()
    /** Was zuletzt im Logbuch stand, je App: Wiederkehrende Hinweise (wartet, App zu alt) erscheinen nur beim Wechsel. */
    private val gemeldet = HashMap<String, String>()

    fun anstossen() { anstoss.trySend(Unit) }

    @Synchronized
    fun starte(context: Context) {
        val app = context.applicationContext
        if (schleife?.isActive == true) return
        APPS.filter { it.adresse !in beobachter }.forEach { a ->
            val b = object : ContentObserver(handler) {
                override fun onChange(selbst: Boolean) = anstossen()
                override fun onChange(selbst: Boolean, uri: Uri?) = anstossen()
            }
            runCatching { app.contentResolver.registerContentObserver(a.adresse, true, b); beobachter[a.adresse] = b }
        }
        schleife = bereich.launch {
            delay(8_000)   // Beim Start des Dienstes haben Verbindung und Gedächtnis Vorrang.
            while (isActive) {
                runCatching { runde(app) }.onFailure { Logbuch.fehler("Geräteabgleich", it) }
                withTimeoutOrNull(ABSTAND_MS) { anstoss.receive() }
                delay(3_000)   // Änderungen kommen in Schüben (mehrere Schreibvorgänge je Bedienschritt): einmal für alle.
            }
        }
    }

    // ---------------------------------------------------------------- Zustand

    private fun zustandDatei(app: Context) = File(app.filesDir, "geraeteabgleich.json")
    private fun lade(app: Context): JSONObject = runCatching { JSONObject(zustandDatei(app).readText()) }.getOrElse { JSONObject() }
    private fun sichere(app: Context, z: JSONObject) {
        val teil = File(app.filesDir, "geraeteabgleich.json.teil")
        teil.writeText(z.toString())
        if (!teil.renameTo(zustandDatei(app))) { zustandDatei(app).delete(); teil.renameTo(zustandDatei(app)) }
    }
    private fun JSONObject.obj(name: String): JSONObject = optJSONObject(name) ?: JSONObject().also { put(name, it) }

    private fun melde(a: App, text: String, fehler: Boolean = false) {
        if (gemeldet.put(a.kennung, text) == text) return
        if (fehler) Logbuch.fehler("Geräteabgleich ${a.name}", null, text) else Logbuch.system("Geräteabgleich ${a.name}", text)
    }

    // ---------------------------------------------------------------- Brücke

    private fun rufe(app: Context, a: App, methode: String, anfrage: JSONObject? = null): JSONObject? = runCatching {
        val extras = Bundle().apply { anfrage?.let { putString("json", it.toString()) } }
        app.contentResolver.call(a.adresse, methode, null, extras)?.getString("json")?.let(::JSONObject)
    }.getOrNull()

    // ---------------------------------------------------------------- Runde

    private fun runde(app: Context) {
        val e = Einstellungen.get(app)
        if (!e.geraeteAbgleichAn || e.serverHost.isBlank() || e.serverToken.isBlank()) return
        if (pause()) return
        val basisAdresse = "https://${e.serverHost}/j/${e.geheimnis}/abgleich"
        val z = lade(app)
        val apps = z.obj("apps")
        // 0. Änderungen hier bemerken, auch ohne Netz. Wann Jarvis sie bemerkt hat (`lokal_seit`), entscheidet später, welcher Stand der
        //    jüngere ist. Die Dateizeit der Datenbank taugt dafür nicht: Export und Einspielen des Abgleichs verschieben sie selbst.
        val staende = HashMap<String, JSONObject>()
        for (a in APPS) {
            val stand = rufe(app, a, "abgleich_stand") ?: continue     // App nicht installiert
            staende[a.kennung] = stand
            val s = apps.obj(a.kennung)
            val stempel = stand.optString("stempel")
            if (stempel.isNotEmpty() && stempel != s.optString("gesehen")) s.put("gesehen", stempel).put("lokal_seit", System.currentTimeMillis())
        }
        // 1. Was liegt am Server Neues? Ohne Netz bleibt alles, wie es ist; lokale Änderungen gehen beim nächsten Mal hoch.
        try { hole(basisAdresse, z, apps) } catch (x: IOException) { return } finally { sichere(app, z) }
        // 2. Je App: einspielen, hochladen oder nichts tun.
        for (a in APPS) {
            if (pause()) return
            val stand = staende[a.kennung] ?: continue
            try { gleiche(app, basisAdresse, a, apps.obj(a.kennung), stand) }
            catch (x: IOException) { /* Netz weg: nächste Runde */ }
            catch (x: Abgelehnt) { melde(a, x.message ?: "vom Server abgelehnt", fehler = true) }
            catch (x: Exception) { Logbuch.fehler("Geräteabgleich ${a.name}", x) }
            sichere(app, z)
        }
    }

    /** Der Server oder die Prüfsumme hat etwas abgelehnt (kein Netzfehler): steht im Logbuch, statt still als „Netz weg“ zu enden. */
    private class Abgelehnt(text: String) : Exception(text)

    /** Im Sprachgespräch ruht der Abgleich, auch zwischen zwei Apps und vor dem Einspielen (Sprachmodus vor Hintergrundarbeit). */
    private fun pause() = de.frank.jarvis.speech.GespraechsFokus.sprachmodus

    private fun hole(basis: String, z: JSONObject, apps: JSONObject) {
        while (true) {
            val url = "$basis/aenderungen?seit=${z.optLong("stand")}&geraet=${Uri.encode(Geraet.ablageName)}"
            val antwort = netz.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) throw IOException("Server ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            // Neu aufgesetzter Speicher: Die gemerkten Server-Stände stimmen nicht mehr, der eigene abgeglichene Stand schon.
            if (antwort.optBoolean("neuaufbau") || (z.optString("instanz").isNotEmpty() && z.optString("instanz") != antwort.optString("instanz"))) {
                for (k in apps.keys()) apps.optJSONObject(k)?.remove("fern")
                if (z.optLong("stand") != 0L && !antwort.optBoolean("neuaufbau")) { z.put("stand", 0).put("instanz", antwort.optString("instanz")); continue }
            }
            z.put("instanz", antwort.optString("instanz"))
            val docs = antwort.optJSONArray("docs")
            for (i in 0 until (docs?.length() ?: 0)) {
                val d = docs!!.getJSONObject(i)
                if (d.optString("art") != ART) continue
                val eintrag = apps.obj(d.optString("id"))
                if (d.optBoolean("geloescht")) { eintrag.remove("fern"); continue }
                val daten = d.optJSONObject("daten") ?: continue
                eintrag.put("fern", JSONObject().put("inhalt", daten.optString("inhalt")).put("sha", daten.optString("text_sha"))
                    .put("geaendert", d.optLong("geaendert")).put("von", d.optString("von")).put("groesse", daten.optLong("groesse")))
            }
            z.put("stand", antwort.optLong("stand"))
            if (!antwort.optBoolean("mehr")) return
        }
    }

    /** Was Jarvis zuletzt für diese App gesehen hat: Stand hier, Stand am Server, App-Fassung. Unverändert heißt nichts zu tun, ohne Export. */
    private fun ruheMarke(stempel: String, fern: JSONObject?, version: Long) = "$stempel|${fern?.optString("inhalt").orEmpty()}|$version"

    private fun gleiche(app: Context, basis: String, a: App, s: JSONObject, stand: JSONObject) {
        val stempel = stand.optString("stempel")
        if (stempel.isEmpty()) { melde(a, "Die App kennt den Abgleich noch nicht (ältere Fassung)."); return }
        val fern = s.optJSONObject("fern")
        val basisInhalt = s.optString("basis")
        val version = stand.optLong("version")
        // Nichts Neues, weder hier noch am Server noch an der App-Fassung: der häufigste Fall, ohne Export. Gilt auch für Wartezustände
        // (Tablet wartet auf das Handy, App gesperrt), sonst liefe dort jede Runde ein voller Export ins Leere.
        if (s.optString("ruhe") == ruheMarke(stempel, fern, version)) return
        val export = rufe(app, a, "abgleich_export") ?: return
        val lokal = export.optString("inhalt")
        if (lokal.isEmpty()) { melde(a, "Export gescheitert: ${export.optString("fehler")}", fehler = true); return }
        // Der Export selbst verschiebt die Dateizeiten (Checkpoint): Das ist keine Änderung von Frank.
        val nachher = export.optString("stempel")
        s.put("gesehen", nachher)
        fun ruhen() { s.put("ruhe", ruheMarke(nachher, fern, version)) }
        if (fern != null && fern.optString("inhalt") == lokal) {
            s.put("basis", lokal).put("stempel", nachher)
            s.remove("gesperrt"); s.remove("gesperrt_bei")
            ruhen()
            melde(a, "abgeglichen")
            return
        }
        val leer = export.optLong("zeilen", -1L) == 0L
        val lokalGeaendert = basisInhalt.isEmpty() || lokal != basisInhalt
        val lokalZeit = s.optLong("lokal_seit", System.currentTimeMillis())
        when {
            // Hier ist die App leer (neu installiert, Daten gelöscht), es gab aber schon einen Stand: Ein leerer Stand geht nie hoch,
            // sonst wären die Daten auf beiden Geräten weg. Stattdessen kommt der Stand des Servers zurück.
            leer && fern != null -> { melde(a, "Die App ist hier leer; der Stand von ${fern.optString("von")} wird zurückgeholt."); einspielen(app, basis, a, s, fern, version) }
            leer -> { if (basisInhalt.isNotEmpty()) melde(a, "Die App ist hier leer und der Server hat keinen Stand: nichts hochgeladen.", fehler = true); ruhen() }
            fern == null -> {
                if (basisInhalt.isEmpty() && Geraet.istTablet) { melde(a, "wartet auf den ersten Stand vom Handy"); ruhen(); return }
                hochladen(app, basis, a, s, export, lokalZeit, null)
            }
            // Gesperrt und seither nichts Neues vom anderen Gerät und keine neue App-Fassung hier: nicht jede Runde neu versuchen.
            s.optBoolean("gesperrt") && s.optString("gesperrt_bei") == "${fern.optString("inhalt")}|$version" -> ruhen()
            !lokalGeaendert -> einspielen(app, basis, a, s, fern, version)
            basisInhalt.isEmpty() -> if (Geraet.istTablet) einspielen(app, basis, a, s, fern, version) else hochladen(app, basis, a, s, export, lokalZeit, fern)
            fern.optString("inhalt") == basisInhalt -> hochladen(app, basis, a, s, export, lokalZeit, fern)   // nur hier geändert
            // Beide Seiten haben seit dem letzten Abgleich geändert: Die jüngere Änderung gewinnt.
            lokalZeit > fern.optLong("geaendert") -> {
                melde(a, "Beide Geräte haben geändert; der Stand dieses Geräts ist jünger und gilt. Der andere bleibt dort als Rückfall liegen.")
                hochladen(app, basis, a, s, export, lokalZeit, fern)
            }
            else -> {
                melde(a, "Beide Geräte haben geändert; der Stand von ${fern.optString("von")} ist jünger und gilt. Der Stand dieses Geräts bleibt als Rückfall in der App liegen.")
                einspielen(app, basis, a, s, fern, version)
            }
        }
    }

    private fun hochladen(app: Context, basis: String, a: App, s: JSONObject, export: JSONObject, lokalZeit: Long, fern: JSONObject?) {
        // Solange das andere Gerät ein neueres Schema hat, überschriebe ein Upload dessen Stand mit einem, den es nicht mehr lesen dürfte.
        if (s.optBoolean("gesperrt")) return
        val sha = export.optString("sha256")
        val paket = File(app.cacheDir, "abgleich-${a.kennung}.zip")
        try {
            (app.contentResolver.openFileDescriptor(a.adresse.buildUpon().path("/abgleich/export").build(), "r") ?: throw IOException("Paket nicht lesbar")).use { pfd ->
                java.io.FileInputStream(pfd.fileDescriptor).use { ein -> paket.outputStream().use { ein.copyTo(it) } }
            }
            if (sha256(paket) != sha) { melde(a, "Das Paket der App passt nicht zu seiner Prüfsumme.", fehler = true); return }
            netz.newCall(Request.Builder().url("$basis/blob/$sha").put(paket.asRequestBody("application/octet-stream".toMediaType())).build()).execute().use { r ->
                if (!r.isSuccessful) throw Abgelehnt("Paket hochladen: Server antwortet ${r.code}")
            }
            // Auch bei falsch gehender Uhr muss ein Stand, der auf dem des Servers aufbaut, diesen ablösen.
            val geaendert = maxOf(lokalZeit, (fern?.optLong("geaendert") ?: 0L) + 1)
            val daten = JSONObject().put("text_sha", sha).put("inhalt", export.optString("inhalt")).put("groesse", paket.length())
                .put("schema", export.optJSONObject("schema") ?: JSONObject()).put("version", export.optLong("version"))
            val rumpf = JSONObject().put("daten", daten).put("geaendert", geaendert).put("geloescht", false).put("von", Geraet.ablageName).toString()
            val antwort = netz.newCall(Request.Builder().url("$basis/doc/$ART/${a.kennung}").put(rumpf.toRequestBody(JSON)).build()).execute().use { r ->
                if (!r.isSuccessful) throw Abgelehnt("Stand melden: Server antwortet ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            if (antwort.optBoolean("uebernommen")) {
                val neuFern = JSONObject().put("inhalt", export.optString("inhalt")).put("sha", sha).put("geaendert", geaendert).put("von", Geraet.ablageName).put("groesse", paket.length())
                s.put("basis", export.optString("inhalt")).put("stempel", export.optString("stempel")).put("gesehen", export.optString("stempel"))
                s.put("fern", neuFern).put("ruhe", ruheMarke(export.optString("stempel"), neuFern, export.optLong("version")))
                gemeldet.remove(a.kennung)
                Logbuch.system("Geräteabgleich ${a.name}", "Stand hochgeladen (${paket.length() / 1024} KB)")
            } else {
                // Der Server hat inzwischen einen jüngeren Stand und gibt ihn gleich mit: Die nächste Runde entscheidet mit ihm.
                antwort.optJSONObject("doc")?.let { d ->
                    val daten = d.optJSONObject("daten") ?: JSONObject()
                    if (!d.optBoolean("geloescht") && daten.optString("inhalt").isNotEmpty()) s.put("fern", JSONObject().put("inhalt", daten.optString("inhalt"))
                        .put("sha", daten.optString("text_sha")).put("geaendert", d.optLong("geaendert")).put("von", "dem anderen Gerät").put("groesse", daten.optLong("groesse")))
                }
                anstossen()
            }
        } finally {
            paket.delete()
        }
    }

    private fun einspielen(app: Context, basis: String, a: App, s: JSONObject, fern: JSONObject, version: Long) {
        // Das Einspielen beendet die App und startet sie neu: nie in ein beginnendes Sprachgespräch hinein.
        if (pause()) return
        val sha = fern.optString("sha")
        val sperre = "${fern.optString("inhalt")}|$version"
        val paket = File(app.cacheDir, "abgleich-${a.kennung}-fern.zip")
        try {
            netz.newCall(Request.Builder().url("$basis/blob/$sha").build()).execute().use { r ->
                if (!r.isSuccessful) throw Abgelehnt("Paket holen: Server antwortet ${r.code}")
                paket.outputStream().use { aus -> r.body!!.byteStream().copyTo(aus) }
            }
            if (sha256(paket) != sha) throw Abgelehnt("Das Paket von ${fern.optString("von")} kam beschädigt an.")
            (app.contentResolver.openFileDescriptor(a.adresse.buildUpon().path("/abgleich/import").build(), "w") ?: throw IOException("Paket nicht ablegbar")).use { pfd ->
                java.io.FileOutputStream(pfd.fileDescriptor).use { aus -> paket.inputStream().use { it.copyTo(aus) } }
            }
            val antwort = rufe(app, a, "abgleich_import", JSONObject().put("sha256", sha))
            if (antwort?.optBoolean("ok") != true) {
                // Bis das andere Gerät etwas Neues hochlädt oder die App hier aktualisiert ist, lohnt kein neuer Versuch.
                if (antwort?.optBoolean("app_zu_alt") == true) s.put("gesperrt", true).put("gesperrt_bei", sperre)
                melde(a, "Einspielen abgelehnt: ${antwort?.optString("fehler") ?: "keine Antwort der App"}", fehler = true)
                return
            }
            // Die App startet ihren Prozess neu und spielt das Paket beim Start ein: Der nächste Aufruf weckt sie und liefert den neuen Stand.
            Thread.sleep(1_500)
            // Solange der alte Prozess noch lebt, antwortet er mit dem alten Stand: ein paar Mal nachfragen.
            var neu: JSONObject? = null
            for (versuch in 1..8) {
                neu = rufe(app, a, "abgleich_export")
                if (neu?.optString("inhalt") == fern.optString("inhalt")) break
                Thread.sleep(800)
            }
            if (neu == null || neu.optString("inhalt") != fern.optString("inhalt")) {
                // Nicht hochladen: Was jetzt in der App steht, ist weder der alte noch sicher der neue Stand.
                s.put("gesperrt", true).put("gesperrt_bei", sperre)
                melde(a, "Nach dem Einspielen stimmt der Inhalt nicht mit dem Stand von ${fern.optString("von")} überein. Abgleich für diese App angehalten.", fehler = true)
                return
            }
            s.put("basis", fern.optString("inhalt")).put("stempel", neu.optString("stempel")).put("gesehen", neu.optString("stempel"))
                .put("ruhe", ruheMarke(neu.optString("stempel"), fern, neu.optLong("version", version)))
            s.remove("gesperrt"); s.remove("gesperrt_bei")
            gemeldet.remove(a.kennung)
            Logbuch.system("Geräteabgleich ${a.name}", "Stand von ${fern.optString("von")} eingespielt (${paket.length() / 1024} KB)")
            if (a.bereiche.isNotEmpty()) bereich.launch { runCatching { Lagebild.auffrischen(app, a.bereiche, "Abgleich ${a.name}") } }
        } finally {
            paket.delete()
        }
    }

    private fun sha256(datei: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        datei.inputStream().use { ein -> val puffer = ByteArray(65536); while (true) { val n = ein.read(puffer); if (n < 0) break; md.update(puffer, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

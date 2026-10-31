package de.frank.jarvis.helfer

import android.content.Context
import android.os.Process
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.javascript.Context as Js
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

/** Ergebnis eines Helferlaufs. [dateien]: name und absoluter Pfad. */
data class HelferErgebnis(val ok: Boolean, val text: String, val daten: JSONObject = JSONObject(), val dateien: List<Pair<String, String>> = emptyList(), val fehler: String = "", val dauerMs: Long = 0L)

/**
 * Führt Handy-Helfer aus: Rhino als reiner Interpreter ohne Java-Zugriff (ClassShutter sperrt alles), mit Rechenzeit-Grenze. Die einzige Tür nach außen ist
 * [Bruecke], und die prüft jede Tür gegen den Steckbrief. Schlüssel setzt nur die Brücke ein; das Skript sieht sie nie.
 */
object HelferLauf {
    private const val MAX_LAUF_MS = 60_000L
    private const val MAX_DATEI = 20 * 1024 * 1024
    private const val MAX_ENTPACKT = 64 * 1024 * 1024
    private const val MAX_HTTP_BYTES = 4_000_000L
    /** Rhino prüft die Zeitgrenze nur zwischen Befehlen; hängt ein Skript in einem Regex, gibt der Aufrufer es nach dieser Nachfrist auf. */
    private const val NACHFRIST_MS = 5_000L
    private const val MAX_PRUEFUNG_MS = 120_000L
    private const val PROBE_AUSGABE = "probe-ausgabe"
    /** Höchstens zwei Läufe gleichzeitig, jeder auf eigenem Faden mit niedriger Priorität: Das Live-Gespräch geht vor. */
    private val LAEUFE = Semaphore(2)
    private val HTTP = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS).followRedirects(false).build()

    private class Zeitfrist : ContextFactory() {
        override fun makeContext(): Js = super.makeContext().apply {
            optimizationLevel = -1
            // Ohne Grenze legt Endlos-Rekursion im Interpreter Frames auf den Heap, bis der Speicher der ganzen App voll ist.
            maximumInterpreterStackDepth = 2_000
            languageVersion = Js.VERSION_ES6
            instructionObserverThreshold = 10_000
            setClassShutter { false }
            putThreadLocal("beginn", System.currentTimeMillis())
        }
        override fun observeInstructionCount(cx: Js, instructionCount: Int) {
            val beginn = cx.getThreadLocal("beginn") as Long
            if (System.currentTimeMillis() - beginn > MAX_LAUF_MS) throw Error("Zeitgrenze von ${MAX_LAUF_MS / 1000} Sekunden erreicht")
            if (Thread.currentThread().isInterrupted) throw Error("Abgebrochen")
        }
    }
    private val fabrik = Zeitfrist()

    /** Was das Skript als `jarvis` sieht; alles per JSON-Text, damit keine Java-Objekte ins Skript gelangen. */
    class Bruecke(private val c: Context, private val h: Helfer, private val ausgabe: File, private val probe: Boolean, private val kontext: CoroutineContext, private val frist: Long) {
        val dateien = mutableListOf<Pair<String, String>>()

        private fun erlaubterHost(url: String, konto: HelferKonto?): Boolean {
            val u = url.toHttpUrlOrNull() ?: return false
            if (u.scheme != "https") return false
            return u.host in (konto?.hosts ?: h.rechte.netz)
        }

        fun http(optionen: String): String {
            val o = JSONObject(optionen)
            val kontoId = o.optString("konto")
            val konto = if (kontoId.isEmpty()) null else h.rechte.konten.firstOrNull { it.id == kontoId } ?: error("Das Konto $kontoId ist für diesen Helfer nicht freigegeben.")
            val url = o.optString("url")
            check(erlaubterHost(url, konto)) { "Diese Adresse ist für den Helfer nicht erlaubt: $url" }
            if (konto != null) {
                check(!probe) { "In der Prüfung werden keine Konten benutzt." }
                check(h.freigegebeneRechte.isNotEmpty()) { "Die Rechte dieses Helfers sind noch nicht freigegeben." }
                check(!KontenRegister.gesperrt(c, konto.id)) { "Das Konto ${konto.id} ist gesperrt." }
            }
            val wert = konto?.let { KontenRegister.finde(c, it.id)?.lesen()?.takeIf { w -> w.isNotBlank() } ?: error("Für ${it.id} ist kein Schlüssel eingetragen (Einstellungen → Konten und APIs).") }
            var ziel = url.toHttpUrlOrNull()!!
            val anfrage = Request.Builder()
            o.optJSONObject("kopf")?.let { k -> k.keys().forEach { anfrage.header(it, k.optString(it)) } }
            if (konto != null) when {
                konto.art == "bearer" -> anfrage.header("Authorization", "Bearer $wert")
                konto.art.startsWith("kopf:") -> anfrage.header(konto.art.removePrefix("kopf:"), wert!!)
                konto.art.startsWith("query:") -> ziel = ziel.newBuilder().addQueryParameter(konto.art.removePrefix("query:"), wert).build()
                else -> error("Unbekannte Art des Kontos: ${konto.art}")
            }
            val methode = o.optString("methode", "GET").uppercase()
            val rumpf = if (o.has("rumpf")) o.optString("rumpf").toRequestBody((o.optJSONObject("kopf")?.optString("Content-Type")?.ifEmpty { null } ?: "application/json").toMediaTypeOrNull()) else null
            anfrage.url(ziel).method(methode, rumpf ?: if (methode in setOf("POST", "PUT", "PATCH")) ByteArray(0).toRequestBody(null) else null)
            HTTP.newCall(anfrage.build()).execute().use { r ->
                if (konto != null) KontenRegister.nutzungMerken(c, konto.id, h.name)
                // Nur begrenzt lesen: body.string() lüde die ganze Antwort in den Speicher, bevor gekürzt wird.
                val text = r.body?.let { b ->
                    val quelle = b.source()
                    quelle.request(MAX_HTTP_BYTES)
                    quelle.buffer.readString(minOf(quelle.buffer.size, MAX_HTTP_BYTES), b.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8)
                }.orEmpty().take(2_000_000)
                return JSONObject().put("code", r.code).put("text", if (wert != null) text.replace(wert, "***") else text).toString()
            }
        }

        fun werkzeug(name: String, argumente: String): String {
            check(name in h.rechte.werkzeuge && !HelferKatalog.werkzeugGesperrt(name)) { "Das Werkzeug $name ist für diesen Helfer nicht erlaubt." }
            val w = Register.werkzeuge(c).firstOrNull { it.name == name } ?: error("Dieses Werkzeug gibt es nicht: $name")
            check(w.nurLesen || !probe) { "In der Prüfung laufen nur lesende Werkzeuge." }
            check(w.nurLesen || h.freigegebeneRechte.isNotEmpty()) { "Die Rechte dieses Helfers sind noch nicht freigegeben." }
            val rest = frist - System.currentTimeMillis()
            check(rest > 0) { "Die Zeitgrenze des Helfers ist erreicht." }
            val e = runBlocking(kontext) { withTimeoutOrNull(rest) { w.ausfuehren(JSONObject(argumente)) } } ?: error("Das Werkzeug $name hat nicht rechtzeitig geantwortet.")
            return JSONObject().put("text", e.text).put("fehler", e.fehler).toString()
        }

        private fun speicher() = HelferKatalog.speicherDatei(c, h.kennung)
        private fun speicherJson() = runCatching { JSONObject(speicher().readText()) }.getOrDefault(JSONObject())
        fun speicherLesen(schluessel: String): String = speicherJson().opt(schluessel)?.toString().orEmpty()
        fun speicherSchreiben(schluessel: String, wert: String) {
            check(!probe) { "In der Prüfung wird nichts dauerhaft gespeichert." }
            val j = speicherJson().put(schluessel, wert)
            check(j.toString().length < 2_000_000) { "Der Speicher des Helfers ist voll." }
            speicher().parentFile?.mkdirs()
            speicher().writeText(j.toString())
        }

        private fun zielDatei(name: String): File {
            check(name.isNotBlank() && !name.contains('/') && !name.contains("..")) { "Ungültiger Dateiname." }
            return File(ausgabe, name)
        }

        /** Erst vollständig schreiben, dann umbenennen: Ein Abbruch ersetzt nie eine gute Datei durch eine halbe. */
        private inline fun atomar(ziel: File, schreiben: (File) -> Unit) {
            val zwischen = File.createTempFile(".entstehend-", "", ausgabe)
            try {
                schreiben(zwischen)
                check(zwischen.renameTo(ziel)) { "Die Datei konnte nicht abgelegt werden." }
            } finally {
                zwischen.delete()
            }
        }

        fun textDatei(name: String, text: String): String {
            check(text.length < MAX_DATEI) { "Die Datei ist zu groß." }
            val bytes = text.toByteArray()
            check(bytes.size < MAX_DATEI) { "Die Datei ist zu groß." }
            val d = zielDatei(name)
            atomar(d) { it.writeBytes(bytes) }
            dateien += name to d.absolutePath
            return d.absolutePath
        }

        fun zipDatei(name: String, inhalte: String): String {
            // Die 20 MB gelten für das fertige Archiv; Text komprimiert stark, deshalb begrenzt das hier vorab den Speicher.
            check(inhalte.length < MAX_ENTPACKT) { "Der Inhalt ist zu groß." }
            val j = JSONObject(inhalte)
            val d = zielDatei(name)
            atomar(d) { zwischen ->
                var summe = 0L
                ZipOutputStream(zwischen.outputStream().buffered()).use { z ->
                    // DOCX und XLSX erwarten [Content_Types].xml als ersten Eintrag.
                    j.keys().asSequence().sortedBy { if (it.startsWith("[Content_Types]")) "" else it }.forEach { pfad ->
                        check(!pfad.contains("..") && !pfad.startsWith("/")) { "Ungültiger Pfad im Archiv." }
                        val bytes = j.optString(pfad).toByteArray()
                        summe += bytes.size
                        check(summe < MAX_ENTPACKT) { "Der Inhalt ist zu groß." }
                        z.putNextEntry(ZipEntry(pfad))
                        z.write(bytes)
                        z.closeEntry()
                    }
                }
                check(zwischen.length() < MAX_DATEI) { "Die Datei ist zu groß." }
            }
            dateien += name to d.absolutePath
            return d.absolutePath
        }

        fun log(text: String) = Logbuch.system("Helfer ${h.kennung}", KontenRegister.schwaerzen(c, text).take(300))
    }

    /** Macht aus der Brücke das bequeme `jarvis`-Objekt; JSON hin und zurück, sodass im Skript nur reine JS-Werte ankommen. */
    private const val VORSPANN = """
var jarvis = (function (b) {
  var p = function (t) { return JSON.parse(String(t)); };
  return Object.freeze({
    http: function (o) { return p(b.http(JSON.stringify(o))); },
    werkzeug: function (n, a) { return p(b.werkzeug(String(n), JSON.stringify(a || {}))); },
    speicherLesen: function (s) { return String(b.speicherLesen(String(s))); },
    speicherSchreiben: function (s, w) { b.speicherSchreiben(String(s), String(w)); },
    textDatei: function (n, t) { return String(b.textDatei(String(n), String(t))); },
    zipDatei: function (n, i) { return String(b.zipDatei(String(n), JSON.stringify(i))); },
    log: function (t) { b.log(String(t)); }
  });
})(__bruecke);
"""

    /** Ein Lauf. [ordner] enthält Skript; in der Prüfung der noch nicht installierte Probeordner. */
    suspend fun ausfuehren(c: Context, h: Helfer, eingabe: JSONObject, ordner: File = HelferKatalog.aktuell(c, h.kennung), probe: Boolean = false): HelferErgebnis {
        // Werkzeuge des Skripts behalten den Kontext des Aufrufers (Agent, Hintergrundlauf), aber nicht seinen Job: Den Abbruch trägt der Interrupt.
        val aufrufer = currentCoroutineContext().minusKey(Job).minusKey(ContinuationInterceptor)
        return LAEUFE.withPermit {
            val beginn = System.currentTimeMillis()
            val fertig = CompletableDeferred<HelferErgebnis>()
            val faden = Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                fertig.complete(
                    try { laufen(c, h, eingabe, ordner, probe, aufrufer) }
                    catch (e: Throwable) { HelferErgebnis(false, "", fehler = "Der Helfer ist abgestürzt: ${e.javaClass.simpleName}", dauerMs = System.currentTimeMillis() - beginn) }
                )
            }, "helfer-${h.kennung}").apply { isDaemon = true }
            faden.start()
            try {
                withTimeoutOrNull(MAX_LAUF_MS + NACHFRIST_MS) { fertig.await() } ?: run {
                    faden.interrupt()
                    HelferErgebnis(false, "", fehler = "Zeitgrenze von ${MAX_LAUF_MS / 1000} Sekunden erreicht", dauerMs = System.currentTimeMillis() - beginn)
                }
            } catch (e: CancellationException) {
                faden.interrupt()
                throw e
            }
        }
    }

    private fun laufen(c: Context, h: Helfer, eingabe: JSONObject, ordner: File, probe: Boolean, aufrufer: CoroutineContext): HelferErgebnis {
        val beginn = System.currentTimeMillis()
        // Prüfläufe bekommen einen eigenen Ordner: Sie löschen ihre Dateien danach und dürfen kein echtes Ergebnis treffen.
        val ausgabe = if (probe) File(ordner, PROBE_AUSGABE).apply { mkdirs() } else HelferKatalog.ausgabe(c, h.kennung)
        val bruecke = Bruecke(c, h, ausgabe, probe, aufrufer, beginn + MAX_LAUF_MS)
        val cx = fabrik.enterContext()
        return try {
            val scope = cx.initSafeStandardObjects()
            // Nur diese Methoden: Der ClassShutter sperrt Java-Klassen, ein Wrapper-Objekt mit reinen Text-Methoden bleibt erreichbar.
            ScriptableObject.putProperty(scope, "__bruecke", BrueckeJs(bruecke, scope))
            cx.evaluateString(scope, VORSPANN, "vorspann", 1, null)
            ScriptableObject.deleteProperty(scope, "__bruecke")
            cx.evaluateString(scope, File(ordner, h.datei).readText(), h.datei, 1, null)
            val f = scope.get("ausfuehren", scope) as? Function ?: error("Das Skript hat keine Funktion ausfuehren.")
            val eingabeJs = org.mozilla.javascript.NativeJSON.parse(cx, scope, eingabe.toString(), org.mozilla.javascript.Callable { _, _, _, args -> args[1] })
            val roh = f.call(cx, scope, scope, arrayOf(eingabeJs))
            val json = org.mozilla.javascript.NativeJSON.stringify(cx, scope, roh, null, null)?.toString().orEmpty()
            val r = runCatching { JSONObject(json) }.getOrDefault(JSONObject().put("text", json))
            val genannt = r.optJSONArray("dateien")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { it.optString("name") to it.optString("pfad") } } ?: emptyList()
            HelferErgebnis(true, KontenRegister.schwaerzen(c, r.optString("text")), r.optJSONObject("daten") ?: JSONObject(), (genannt + bruecke.dateien).distinctBy { it.second }, dauerMs = System.currentTimeMillis() - beginn)
        } catch (e: Throwable) {
            // Auch Stack-Überlauf und Speichermangel eines Skripts dürfen Jarvis nicht mitreißen; dort nur ein fester Text, das Schwärzen bräuchte selbst Speicher.
            val grund = when (e) {
                is OutOfMemoryError -> "Der Helfer braucht zu viel Speicher."
                is StackOverflowError -> "Der Helfer ruft sich zu tief selbst auf."
                else -> KontenRegister.schwaerzen(c, (e as? org.mozilla.javascript.RhinoException)?.details() ?: e.message ?: e.javaClass.simpleName).take(500)
            }
            HelferErgebnis(false, "", fehler = grund, dauerMs = System.currentTimeMillis() - beginn)
        } finally {
            Js.exit()
        }
    }

    /** Die Tür ins Skript als eigenes Script-Objekt, ganz ohne Java-Reflexion (die ist gesperrt). */
    private class BrueckeJs(private val b: Bruecke, scope: ScriptableObject) : ScriptableObject(scope, null) {
        override fun getClassName() = "Bruecke"
        init {
            fun m(name: String, n: Int, f: (List<String>) -> String) = put(name, this, object : org.mozilla.javascript.BaseFunction() {
                override fun call(cx: Js, s: org.mozilla.javascript.Scriptable, t: org.mozilla.javascript.Scriptable?, a: Array<Any?>): Any =
                    try { f((0 until n).map { Js.toString(a.getOrNull(it)) }) } catch (e: Exception) { throw org.mozilla.javascript.ScriptRuntime.constructError("Error", e.message ?: e.javaClass.simpleName) }
            })
            m("http", 1) { b.http(it[0]) }
            m("werkzeug", 2) { b.werkzeug(it[0], it[1]) }
            m("speicherLesen", 1) { b.speicherLesen(it[0]) }
            m("speicherSchreiben", 2) { b.speicherSchreiben(it[0], it[1]); "" }
            m("textDatei", 2) { b.textDatei(it[0], it[1]) }
            m("zipDatei", 2) { b.zipDatei(it[0], it[1]) }
            m("log", 1) { b.log(it[0]); "" }
        }
    }

    /**
     * Prüft ein neues Paket in einem Probeordner: je Test ein Lauf, gemessen, Ergebnis gegen die Erwartung. Bei Fehlschlag bleibt der Ordner uninstalliert.
     * Liefert Prüfungsprotokoll und ob alles bestanden ist.
     */
    suspend fun pruefen(c: Context, manifest: JSONObject, ordner: File): Pair<Boolean, JSONObject> {
        val h = Helfer(manifest)
        val tests = manifest.optJSONArray("tests") ?: JSONArray()
        val zeilen = JSONArray()
        var alle = true
        val ende = System.currentTimeMillis() + MAX_PRUEFUNG_MS
        var abgelaufen = false
        for (i in 0 until tests.length()) {
            val t = tests.optJSONObject(i) ?: continue
            // Ein hängendes Skript soll die Registrierung nicht je Test eine Minute aufhalten.
            if (abgelaufen || System.currentTimeMillis() > ende) {
                alle = false
                zeilen.put(JSONObject().put("name", t.optString("name")).put("ok", false).put("dauer_ms", 0).put("grund", "Übersprungen: Zeitgrenze der Prüfung erreicht"))
                continue
            }
            val erwartet = t.optJSONObject("erwartet") ?: JSONObject().put("ok", true)
            val r = ausfuehren(c, h, t.optJSONObject("eingabe") ?: JSONObject(), ordner, probe = true)
            if (r.dauerMs >= MAX_LAUF_MS) abgelaufen = true
            val enthaelt = erwartet.optJSONArray("enthaelt")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
            val gesamt = r.text + " " + r.daten + " " + r.dateien.joinToString { it.first }
            val ok = r.ok == erwartet.optBoolean("ok", true) && enthaelt.all { gesamt.contains(it) } &&
                r.dateien.all { File(it.second).let { d -> d.exists() && d.length() > 0 } }
            if (!ok) alle = false
            zeilen.put(JSONObject().put("name", t.optString("name")).put("ok", ok).put("dauer_ms", r.dauerMs).put("grund", if (ok) "" else (r.fehler.ifEmpty { "Ergebnis passt nicht: " + r.text.take(200) })))
            r.dateien.forEach { runCatching { File(it.second).delete() } }
        }
        File(ordner, PROBE_AUSGABE).deleteRecursively()
        return alle to JSONObject().put("zeit", System.currentTimeMillis() / 1000).put("ergebnis", if (alle) "${tests.length()} von ${tests.length()} Tests bestanden" else "Tests nicht bestanden").put("tests", zeilen)
    }
}

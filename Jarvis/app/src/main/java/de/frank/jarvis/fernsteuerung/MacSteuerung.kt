package de.frank.jarvis.fernsteuerung

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Computer Use: ein einzelner Befehl an den Fernsteuerungs-Dienst des Macs (Jarvis/werkzeuge/mac-fernsteuerung/Befehle.swift).
 * Jeder Befehl hat eine eigene Verbindung (Anmeldung mit art=werkzeug); eine offene Live-Sitzung bleibt dabei bestehen.
 * Rahmen wie bei [FernVerbindung]: 0x10 Anmeldung, 0x12 Befehl; zurück 0x05 Antwort, bei Dateien danach 0x06 mit den Rohdaten.
 */
object MacSteuerung {
    private const val VERBINDEN_MS = 8000
    private const val MAX_ANTWORT = 40_000_000

    fun token(context: Context): String =
        runCatching { File(context.applicationContext.filesDir, "fernsteuerung-token.txt").readText().trim() }.getOrDefault("")

    /** Antwort des Dienstes; eine mitgelieferte Datei landet in [datei]. Wirft [IOException], wenn der Mac nicht erreichbar ist. */
    suspend fun befehl(context: Context, befehl: JSONObject, fristMs: Int = 30_000, datei: File? = null): JSONObject {
        val t = token(context)
        if (t.isEmpty()) throw IOException("Der Zugang zum Mac fehlt (fernsteuerung-token.txt).")
        // Im Tunnel geht gelegentlich ein einzelner Verbindungsversuch verloren (siehe Fernziel): einmal nachfassen.
        return try { einmal(t, befehl, fristMs, datei, VERBINDEN_MS) } catch (e: java.net.SocketTimeoutException) {
            if (e.message.orEmpty().contains("connect", ignoreCase = true)) einmal(t, befehl, fristMs, datei, 4000) else throw e
        }
    }

    private suspend fun einmal(token: String, befehl: JSONObject, fristMs: Int, datei: File?, verbindenMs: Int): JSONObject = suspendCancellableCoroutine { fort ->
        val s = Socket()
        fort.invokeOnCancellation { runCatching { s.close() } }
        thread(isDaemon = true, name = "Mac-Befehl") {
            try {
                s.use {
                    s.tcpNoDelay = true
                    s.connect(InetSocketAddress(Fernziel.MAC.adresse, FernsteuerungActivity.PORT), verbindenMs)
                    s.soTimeout = fristMs
                    val aus = DataOutputStream(s.getOutputStream().buffered())
                    fun senden(typ: Int, j: JSONObject) {
                        val d = j.toString().toByteArray()
                        aus.writeByte(typ); aus.writeInt(d.size); aus.write(d)
                    }
                    senden(0x10, JSONObject().put("token", token).put("art", "werkzeug"))
                    senden(0x12, befehl)
                    aus.flush()
                    val ein = DataInputStream(s.getInputStream().buffered(1 shl 16))
                    val antwort = antwort(ein)
                    if (antwort.optLong("bytes") > 0) dateiLesen(ein, datei)
                    fort.resume(antwort)
                }
            } catch (e: Throwable) { fort.resumeWithException(e) }
        }
    }

    private fun antwort(ein: DataInputStream): JSONObject {
        while (true) {
            val typ = ein.readUnsignedByte()
            val n = ein.readInt()
            if (n < 0 || n > MAX_ANTWORT) throw IOException("Antwort zu groß: $n")
            val daten = ByteArray(n)
            ein.readFully(daten)
            if (typ == 0x05) return JSONObject(String(daten))
        }
    }

    private fun dateiLesen(ein: DataInputStream, datei: File?) {
        val typ = ein.readUnsignedByte()
        val n = ein.readInt()
        if (typ != 0x06 || n < 0) throw IOException("Unerwartete Datei vom Mac")
        val puffer = ByteArray(1 shl 16)
        var rest = n
        (datei ?: throw IOException("Unerwartete Datei vom Mac")).outputStream().use { o ->
            while (rest > 0) {
                val g = ein.read(puffer, 0, minOf(rest, puffer.size))
                if (g < 0) throw IOException("Datei vom Mac abgebrochen")
                o.write(puffer, 0, g)
                rest -= g
            }
        }
    }
}

package de.frank.jarvis.fernsteuerung

import android.util.Log
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

/**
 * TCP-Verbindung zum Fernsteuerungs-Dienst auf dem Mac (Jarvis/werkzeuge/mac-fernsteuerung).
 * Rahmen in beide Richtungen: [1 Byte Typ][4 Byte Länge][Nutzlast].
 * Mac -> Handy: 0x01 Info {w,h}, 0x02 H.264 (Annex B), 0x03 Zustand {tastatur,geheim,gesperrt}, 0x04 Meldung {meldung}.
 * Handy -> Mac: 0x10 Anmeldung {token}, 0x11 Eingabe {t:…}.
 */
class FernVerbindung(
    private val host: String,
    private val port: Int,
    private val token: String,
    private val beiInfo: (Int, Int, Boolean) -> Unit,
    private val beiBild: (ByteArray) -> Unit,
    private val beiZustand: (JSONObject) -> Unit,
    private val beiEnde: (String) -> Unit,
    private val beiMeldung: (String) -> Unit = {},
) {
    private val sender = Executors.newSingleThreadExecutor()
    @Volatile private var socket: Socket? = null
    @Volatile private var aus: DataOutputStream? = null
    @Volatile private var beendet = false

    fun start() {
        Thread({
            try {
                val s = Socket()
                s.tcpNoDelay = true
                s.receiveBufferSize = 1 shl 20
                s.connect(InetSocketAddress(host, port), 6000)
                socket = s
                aus = DataOutputStream(s.getOutputStream().buffered())
                senden(0x10, JSONObject().put("token", token))
                // Lebenszeichen: ohne es beendet der Mac die Sitzung nach 12 s (Tunnel weg, Handy aus).
                Thread({
                    while (!beendet) {
                        senden(0x11, JSONObject().put("t", "puls"))
                        try { Thread.sleep(3000) } catch (_: InterruptedException) { break }
                    }
                }, "Fernsteuerung-Puls").apply { isDaemon = true }.start()
                val ein = DataInputStream(s.getInputStream().buffered(1 shl 16))
                while (!beendet) {
                    val typ = ein.readUnsignedByte()
                    val n = ein.readInt()
                    if (n < 0 || n > 20_000_000) throw IllegalStateException("Rahmen zu groß: $n")
                    val daten = ByteArray(n)
                    ein.readFully(daten)
                    when (typ) {
                        0x01 -> JSONObject(String(daten)).let { beiInfo(it.optInt("w"), it.optInt("h"), it.optBoolean("ok", true)) }
                        0x02 -> beiBild(daten)
                        0x03 -> beiZustand(JSONObject(String(daten)))
                        0x04 -> beiMeldung(JSONObject(String(daten)).optString("meldung"))
                    }
                }
            } catch (e: Exception) {
                if (!beendet) {
                    Log.w(TAG, "Verbindung beendet", e)
                    beiEnde(e.message ?: e.javaClass.simpleName)
                }
            } finally {
                runCatching { socket?.close() }
            }
        }, "Fernsteuerung-Lesen").start()
    }

    fun eingabe(j: JSONObject) = senden(0x11, j)

    private fun senden(typ: Int, j: JSONObject) {
        if (beendet) return
        // Zwischen Prüfung und execute kann stop() den Sender schon geschlossen haben.
        try { sender.execute {
            val o = aus ?: return@execute
            try {
                val b = j.toString().toByteArray()
                o.writeByte(typ); o.writeInt(b.size); o.write(b); o.flush()
            } catch (e: Exception) {
                Log.w(TAG, "Senden fehlgeschlagen", e)
            }
        } } catch (_: java.util.concurrent.RejectedExecutionException) {}
    }

    fun stop() {
        beendet = true
        runCatching { socket?.close() }
        sender.shutdown()
    }

    companion object { const val TAG = "JarvisFernsteuerung" }
}

package de.frank.jarvis.fernsteuerung

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/** Rechner, deren Bildschirm Jarvis fernbedienen kann, mit ihrer Adresse im eigenen Tunnel (WireGuard). */
enum class Fernziel(val adresse: String, val titel: String, val nominativ: String, val dativ: String, val port: Int = FernsteuerungActivity.PORT, val konsole: Boolean = false) {
    MAC("10.8.0.6", "Mac-Bildschirm", "Der Mac", "dem Mac"),
    WINDOWS("10.8.0.2", "Windows-Bildschirm", "Der Windows-PC", "dem Windows-PC"),
    /** Kein Bildschirm: Die Server-Konsole (OpenLauncherServer/web) öffnet [ServerKonsoleActivity] statt der Fernsteuerung. */
    SERVER("10.8.0.1", "Server-Konsole", "Der Hostinger-Server", "dem Hostinger-Server", 8090, konsole = true);

    companion object {
        fun aus(name: String?): Fernziel = entries.firstOrNull { it.name == name } ?: MAC

        /**
         * Welche Rechner gerade wirklich fernbedienbar sind (gleichzeitig geprüft, höchstens [wartezeitMs] plus kurze
         * Dienstprüfung, bei Fehlschlag ein zweiter Versuch mit 4 s). 4 s reichten am 29.10.2026 nicht: Hängt der Mac am
         * Handy-Hotspot, laufen Prüfungen zweimal übers Mobilnetz (Handy, VPS, Handy, Mac), gemessen 1,7 s Antwortzeit und
         * 10 % Verlust; erfolgreiche Verbindungen brauchten bis 4 s. 1,5 s reichten im Tunnel nicht: Nach einer Pause braucht WireGuard erst einen Handshake über den
         * VPS, der erste Verbindungsversuch kam am 28.10.2026 zu spät an und Jarvis verband blind mit dem ausgeschalteten Mac.
         */
        suspend fun erreichbar(wartezeitMs: Int = 8000): List<Fernziel> = pruefen(wartezeitMs).filter { it.ok }.map { it.ziel }

        /** Wie [erreichbar], aber mit Grund und Dauer je Rechner, damit das Logbuch erklären kann, warum einer rot wurde. */
        suspend fun pruefen(wartezeitMs: Int = 8000): List<Pruefung> = coroutineScope {
            entries.map { z ->
                async(Dispatchers.IO) {
                    // Im Tunnel geht gelegentlich ein einzelner Verbindungsversuch verloren: einmal nachfassen.
                    val erst = bereit(z, z.port, wartezeitMs)
                    if (erst.ok) erst
                    else bereit(z, z.port, 4000).let { it.copy(ms = erst.ms + it.ms, grund = if (it.ok) it.grund else "2 Versuche: ${erst.grund} / ${it.grund}") }
                }
            }.map { it.await() }
        }

        /**
         * Port offen allein reicht nicht: Die Weiterleitung (Node bzw. Ruby) nimmt auch an, wenn der Dienst dahinter nicht
         * läuft, und schließt dann sofort. Der Dienst selbst wartet still auf die Anmeldung; kommt in der kurzen Lesefrist
         * also nichts und kein Verbindungsende, ist er da. Ohne Anmeldung schließt er die Prüfverbindung selbst, eine
         * laufende Sitzung bleibt unberührt.
         */
        private suspend fun bereit(z: Fernziel, port: Int, wartezeitMs: Int): Pruefung = withContext(Dispatchers.IO) {
            val beginn = System.currentTimeMillis()
            fun ergebnis(ok: Boolean, grund: String) = Pruefung(z, ok, grund, System.currentTimeMillis() - beginn)
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(z.adresse, port), wartezeitMs)
                    s.soTimeout = 800
                    try {
                        if (s.getInputStream().read() >= 0) ergebnis(true, "Dienst antwortet")
                        else ergebnis(false, "Weiterleitung nimmt an, Dienst dahinter schließt sofort (Dienst aus?)")
                    } catch (_: SocketTimeoutException) { ergebnis(true, "Dienst wartet auf Anmeldung") }
                }
            } catch (_: SocketTimeoutException) {
                ergebnis(false, "Verbindungsaufbau nach $wartezeitMs ms ohne Antwort (Tunnel-Paket verloren, Rechner schläft oder aus)")
            } catch (e: Exception) {
                ergebnis(false, "${e.javaClass.simpleName}: ${e.message.orEmpty()}")
            }
        }
    }
}

/** Ergebnis einer Erreichbarkeitsprüfung: [grund] und [ms] landen bei Fehlschlägen im Logbuch. */
data class Pruefung(val ziel: Fernziel, val ok: Boolean, val grund: String, val ms: Long)

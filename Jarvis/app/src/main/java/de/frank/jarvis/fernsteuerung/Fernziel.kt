package de.frank.jarvis.fernsteuerung

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/** Rechner, deren Bildschirm Jarvis fernbedienen kann, mit ihrer Adresse im eigenen Tunnel (WireGuard). */
enum class Fernziel(val adresse: String, val titel: String, val nominativ: String, val dativ: String) {
    MAC("10.8.0.6", "Mac-Bildschirm", "Der Mac", "dem Mac"),
    WINDOWS("10.8.0.2", "Windows-Bildschirm", "Der Windows-PC", "dem Windows-PC");

    companion object {
        fun aus(name: String?): Fernziel = entries.firstOrNull { it.name == name } ?: MAC

        /**
         * Welche Rechner gerade wirklich fernbedienbar sind (gleichzeitig geprüft, höchstens [wartezeitMs] plus kurze
         * Dienstprüfung). 1,5 s reichten im Tunnel nicht: Nach einer Pause braucht WireGuard erst einen Handshake über den
         * VPS, der erste Verbindungsversuch kam am 28.10.2026 zu spät an und Jarvis verband blind mit dem ausgeschalteten Mac.
         */
        suspend fun erreichbar(wartezeitMs: Int = 4000): List<Fernziel> = coroutineScope {
            entries.map { z -> async(Dispatchers.IO) { z.takeIf { bereit(it.adresse, FernsteuerungActivity.PORT, wartezeitMs) } } }
                .mapNotNull { it.await() }
        }

        /**
         * Port offen allein reicht nicht: Die Weiterleitung (Node bzw. Ruby) nimmt auch an, wenn der Dienst dahinter nicht
         * läuft, und schließt dann sofort. Der Dienst selbst wartet still auf die Anmeldung; kommt in der kurzen Lesefrist
         * also nichts und kein Verbindungsende, ist er da. Ohne Anmeldung schließt er die Prüfverbindung selbst, eine
         * laufende Sitzung bleibt unberührt.
         */
        private suspend fun bereit(host: String, port: Int, wartezeitMs: Int): Boolean = withContext(Dispatchers.IO) {
            runCatching {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), wartezeitMs)
                    s.soTimeout = 800
                    try { s.getInputStream().read() >= 0 } catch (_: SocketTimeoutException) { true }
                }
            }.getOrDefault(false)
        }
    }
}

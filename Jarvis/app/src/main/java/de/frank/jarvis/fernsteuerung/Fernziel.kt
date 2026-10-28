package de.frank.jarvis.fernsteuerung

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/** Rechner, deren Bildschirm Jarvis fernbedienen kann, mit ihrer Adresse im eigenen Tunnel (WireGuard). */
enum class Fernziel(val adresse: String, val titel: String, val nominativ: String, val dativ: String) {
    MAC("10.8.0.6", "Mac-Bildschirm", "Der Mac", "dem Mac"),
    WINDOWS("10.8.0.2", "Windows-Bildschirm", "Der Windows-PC", "dem Windows-PC");

    companion object {
        fun aus(name: String?): Fernziel = entries.firstOrNull { it.name == name } ?: MAC

        /** Welche Rechner gerade auf dem Fernsteuerungs-Port antworten (gleichzeitig geprüft, höchstens [wartezeitMs]). */
        suspend fun erreichbar(wartezeitMs: Int = 1500): List<Fernziel> = coroutineScope {
            entries.map { z -> async(Dispatchers.IO) { z.takeIf { offen(it.adresse, FernsteuerungActivity.PORT, wartezeitMs) } } }
                .mapNotNull { it.await() }
        }

        private suspend fun offen(host: String, port: Int, wartezeitMs: Int): Boolean = withContext(Dispatchers.IO) {
            runCatching { Socket().use { it.connect(InetSocketAddress(host, port), wartezeitMs); true } }.getOrDefault(false)
        }
    }
}

package de.frank.newskompass.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/**
 * Schreibt Dateien so, dass nie eine halbe Datei das Original ersetzt: erst in eine eigene
 * Zwischendatei, auf den Datenträger synchronisiert, zurückgelesen und geprüft — erst dann
 * ersetzt ein Umbenennen das Ziel. Danach wird das Ziel noch einmal gelesen und geprüft.
 */
object SicheresSchreiben {

    /** [pruefe] bekommt den zurückgelesenen Text und wirft, wenn er unbrauchbar ist. */
    fun schreibe(ziel: File, inhalt: String, pruefe: (String) -> Unit) {
        val zwischen = File(ziel.parentFile, "${ziel.name}.${UUID.randomUUID()}.tmp")
        try {
            FileOutputStream(zwischen).use { aus ->
                aus.write(inhalt.toByteArray(Charsets.UTF_8))
                aus.flush()
                aus.fd.sync()
            }
            pruefe(zwischen.readText())
            if (!zwischen.renameTo(ziel)) throw IOException("Umbenennen nach ${ziel.name} gescheitert")
        } catch (fehler: Exception) {
            // Nur die eigene Zwischendatei verschwindet; das bisherige Ziel bleibt, wie es war.
            zwischen.delete()
            throw fehler
        }
        pruefe(ziel.readText())
    }

    fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

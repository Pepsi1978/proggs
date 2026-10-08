package com.ngrok

/**
 * Lädt die native ngrok-Bibliothek auf Android. Das ngrok-Paket will sie aus der JAR in einen Temp-Ordner
 * entpacken; in einer APK gibt es sie dort nicht. Hier wird sie stattdessen wie jede Android-Bibliothek
 * geladen und danach so eingerichtet, wie das Paket es selbst täte. Liegt bewusst im Paket com.ngrok:
 * Nur von hier aus ist dessen interne Klasse Runtime erreichbar.
 */
object NgrokStart {
    private var geladen = false

    @Synchronized
    fun lade() {
        if (geladen) return
        System.loadLibrary("ngrok_java")
        Runtime.init(Runtime.getLogger())
        geladen = true
    }
}

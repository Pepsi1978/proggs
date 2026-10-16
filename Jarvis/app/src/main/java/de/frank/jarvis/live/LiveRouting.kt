package de.frank.jarvis.live

import android.media.AudioDeviceInfo

/**
 * Wählt das Kommunikationsgerät für das Live-Gespräch: ein verbundenes Headset oder die Freisprechanlage im Auto vor dem
 * Handy-Lautsprecher. Reine Logik über die Gerätetypen, damit sie ohne Gerät testbar ist.
 */
object LiveRouting {
    const val TAG = "JarvisLiveRouting"

    /** Reihenfolge der Wahl; was hier nicht steht (z. B. Hörmuschel), wird nie gewählt. */
    private val VORRANG = listOf(
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_HEARING_AID,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
    )

    /** Index des gewählten Geräts in [typen] oder -1, wenn keins passt. */
    fun wahl(typen: List<Int>): Int {
        for (typ in VORRANG) { val i = typen.indexOf(typ); if (i >= 0) return i }
        return -1
    }

    /** Ausgänge, über die Medienton in voller Qualität geht; nur dann lohnt der Medienweg statt des Telefonmodus. */
    private val MEDIEN_AUSGAENGE = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_USB_HEADSET,
    )

    fun medienAusgang(typen: List<Int>) = typen.any { it in MEDIEN_AUSGAENGE }

    fun istBluetooth(typ: Int) = typ == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || typ == AudioDeviceInfo.TYPE_BLE_HEADSET ||
        typ == AudioDeviceInfo.TYPE_BLE_SPEAKER || typ == AudioDeviceInfo.TYPE_HEARING_AID

    fun name(typ: Int) = when (typ) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Lautsprecher"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Hörmuschel"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Handy-Mikrofon"
        AudioDeviceInfo.TYPE_TELEPHONY -> "Telefonleitung"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth-Freisprechen (SCO)"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth-Medien (A2DP)"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth-LE-Headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth-LE-Lautsprecher"
        AudioDeviceInfo.TYPE_HEARING_AID -> "Hörgerät"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB-Headset"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Kabel-Headset"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Kabel-Kopfhörer"
        else -> "Typ $typ"
    }
}

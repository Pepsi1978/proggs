package de.frank.jarvis.live

import android.media.AudioDeviceInfo.TYPE_BLE_HEADSET
import android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO
import android.media.AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
import android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
import android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveRoutingTest {
    @Test fun autoVorLautsprecher() = assertEquals(2, LiveRouting.wahl(listOf(TYPE_BUILTIN_EARPIECE, TYPE_BUILTIN_SPEAKER, TYPE_BLUETOOTH_SCO)))
    @Test fun ohneBluetoothLautsprecher() = assertEquals(1, LiveRouting.wahl(listOf(TYPE_BUILTIN_EARPIECE, TYPE_BUILTIN_SPEAKER)))
    @Test fun leAudioVorSco() = assertEquals(1, LiveRouting.wahl(listOf(TYPE_BLUETOOTH_SCO, TYPE_BLE_HEADSET)))
    @Test fun kabelVorLautsprecher() = assertEquals(1, LiveRouting.wahl(listOf(TYPE_BUILTIN_SPEAKER, TYPE_WIRED_HEADSET)))
    @Test fun nieHoermuschel() = assertEquals(-1, LiveRouting.wahl(listOf(TYPE_BUILTIN_EARPIECE)))
}

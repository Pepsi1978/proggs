package de.frank.jarvis.audio

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Hält den Anker PowerConf (Konferenz-Lautsprecher, steht dauerhaft in der Wohnung) zu Hause verbunden.
 *
 * Auslöser: App-Start, Bluetooth geht an (etwa nach dem Schlafmodus), der Anker trennt sich, Heimkommen (eigener Umkreis
 * wie beim Licht) und die Minutenrunde. Dann versucht Jarvis zuerst im Abstand weniger Sekunden, danach immer seltener zu
 * verbinden, bis eines der Profile (Medien oder Telefon) steht. Läuft im Hintergrund über den Prozess der Dienste, also auch
 * bei gesperrtem Handy.
 *
 * Android hat für normale Apps keine öffentliche „Verbinden“-Funktion. Jeder Versuch geht deshalb eine Leiter durch:
 * verborgenes `connect` der Profile (Medien, Telefon), verborgenes `BluetoothDevice.connect`, zuletzt eine SDP-Abfrage,
 * die die Funkstrecke aufbaut, worauf sich der Anker meist selbst wieder verbindet. Was greift, steht im Logbuch.
 */
object AnkerVerbindung {
    private const val UMKREIS_ID = "anker_zuhause"
    private const val RADIUS_M = 150f

    /** Für die Anzeige in den Einstellungen und für Jarvis. */
    enum class Zustand(val text: String) {
        AUS("Aus"), VERBUNDEN("Verbunden"), SUCHT("Sucht"), WARTET("Wartet"),
        BLUETOOTH_AUS("Bluetooth aus"), UNTERWEGS("Nicht zu Hause"), NICHT_GEKOPPELT("Nicht gekoppelt"), KEINE_ERLAUBNIS("Bluetooth-Erlaubnis fehlt"),
    }

    private val _zustand = MutableStateFlow(Zustand.AUS)
    val zustand: StateFlow<Zustand> = _zustand.asStateFlow()

    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var lauf: Job? = null
    @Volatile private var gestartet = false
    @Volatile private var a2dp: BluetoothProfile? = null
    @Volatile private var headset: BluetoothProfile? = null
    /** Welche Sprossen der Leiter in diesem Prozess schon einmal ins Logbuch geschrieben wurden (sonst flutet es). */
    private val gemeldet = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private fun erlaubt(c: Context) = Build.VERSION.SDK_INT < 31 ||
        c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun adapter(c: Context): BluetoothAdapter? = c.getSystemService(BluetoothManager::class.java)?.adapter

    @SuppressLint("MissingPermission")
    private fun geraet(c: Context): BluetoothDevice? = runCatching {
        adapter(c)?.bondedDevices?.firstOrNull { it.name?.contains(KonferenzMikrofon.GERAET, ignoreCase = true) == true }
    }.getOrNull()

    /** Steht Medien- oder Telefon-Profil zum Anker? */
    @SuppressLint("MissingPermission")
    fun verbunden(c: Context): Boolean {
        if (!erlaubt(c)) return false
        val d = geraet(c) ?: return false
        val p = listOfNotNull(a2dp, headset)
        if (p.isNotEmpty()) return p.any { runCatching { it.getConnectionState(d) }.getOrDefault(0) == BluetoothProfile.STATE_CONNECTED }
        // Proxys noch nicht da: Kommunikationsgerät als Ersatz.
        return KonferenzMikrofon.verfuegbar(c)
    }

    /** Einmal beim App-Start: Empfänger für Bluetooth anmelden, Profile holen, ersten Versuch anstoßen. */
    fun starten(context: Context) {
        val c = context.applicationContext
        if (gestartet) return
        gestartet = true
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        runCatching { ContextCompat.registerReceiver(c, Empfaenger, filter, ContextCompat.RECEIVER_NOT_EXPORTED) }
            .onFailure { Logbuch.fehler("Anker: Empfänger", it) }
        proxysHolen(c)
        anstossen(c, "App-Start")
    }

    @SuppressLint("MissingPermission")
    private fun proxysHolen(c: Context) {
        if (!erlaubt(c)) return
        val a = adapter(c) ?: return
        val hoerer = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) a2dp = proxy else if (profile == BluetoothProfile.HEADSET) headset = proxy
                zustandAktualisieren(c)
            }
            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) a2dp = null else if (profile == BluetoothProfile.HEADSET) headset = null
            }
        }
        if (a2dp == null) runCatching { a.getProfileProxy(c, hoerer, BluetoothProfile.A2DP) }
        if (headset == null) runCatching { a.getProfileProxy(c, hoerer, BluetoothProfile.HEADSET) }
    }

    /** Warum gerade nicht gesucht wird; null = suchen. */
    private fun hindernis(c: Context): Zustand? = when {
        !Einstellungen.get(c).ankerAutoVerbinden -> Zustand.AUS
        !erlaubt(c) -> Zustand.KEINE_ERLAUBNIS
        adapter(c)?.isEnabled != true -> Zustand.BLUETOOTH_AUS
        geraet(c) == null -> Zustand.NICHT_GEKOPPELT
        !zuHause(c) -> Zustand.UNTERWEGS
        else -> null
    }

    fun zustandAktualisieren(c: Context) {
        val h = hindernis(c)
        _zustand.value = when {
            h == Zustand.AUS || h == Zustand.KEINE_ERLAUBNIS || h == Zustand.BLUETOOTH_AUS || h == Zustand.NICHT_GEKOPPELT -> h
            verbunden(c) -> Zustand.VERBUNDEN
            h != null -> h
            lauf?.isActive == true -> Zustand.SUCHT
            else -> Zustand.WARTET
        }
    }

    /** Startet die Suche neu mit kurzen Abständen (alter Lauf endet). */
    fun anstossen(context: Context, grund: String) {
        val c = context.applicationContext
        lauf?.cancel()
        lauf = bereich.launch { suchen(c, grund) }
    }

    /** Aus der Minutenrunde: Umkreis pflegen und, falls nichts läuft und keine Verbindung steht, langsam weitersuchen. */
    suspend fun pruefen(context: Context) {
        val c = context.applicationContext
        if (!gestartet) starten(c)
        runCatching { umkreisAbgleichen(c) }
        proxysHolen(c)
        if (lauf?.isActive != true && hindernis(c) == null && !verbunden(c)) lauf = bereich.launch { suchen(c, "Minutenrunde", schnell = false) }
        zustandAktualisieren(c)
    }

    /** Abstände: zuerst alle 4 s (40 s lang), dann alle 20 s (gut 3 Minuten), dann jede Minute (30 Minuten), dann alle 5 Minuten. */
    private fun abstand(versuch: Int): Long = when {
        versuch < 10 -> 4_000L
        versuch < 20 -> 20_000L
        versuch < 50 -> 60_000L
        else -> 300_000L
    }

    private suspend fun suchen(c: Context, grund: String, schnell: Boolean = true) {
        var versuch = if (schnell) 0 else 20
        var protokolliert = false
        while (true) {
            proxysHolen(c)
            val h = hindernis(c)
            if (h != null || verbunden(c)) { zustandAktualisieren(c); return }
            _zustand.value = Zustand.SUCHT
            if (!protokolliert) { Logbuch.system("Anker: suche Verbindung", grund); protokolliert = true }
            geraet(c)?.let { verbindenVersuchen(c, it) }
            // Kurz warten und prüfen, ob es gegriffen hat (die Profile brauchen ein, zwei Sekunden).
            delay(abstand(versuch++))
            if (verbunden(c)) {
                Logbuch.system("Anker verbunden", "nach $versuch Versuch(en), Anlass: $grund")
                zustandAktualisieren(c)
                return
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun verbindenVersuchen(c: Context, d: BluetoothDevice) {
        // 1. Verborgenes connect der Profile (Medien und Telefon).
        for ((name, proxy) in listOf("Medien" to a2dp, "Telefon" to headset)) {
            if (proxy == null) continue
            runCatching { proxy.javaClass.getMethod("connect", BluetoothDevice::class.java).invoke(proxy, d) }
                .onSuccess { melden("$name-connect", "Ergebnis $it") }
                .onFailure { melden("$name-connect", ursache(it)) }
        }
        // 2. Verborgenes BluetoothDevice.connect (alle erlaubten Profile).
        runCatching { d.javaClass.getMethod("connect").invoke(d) }
            .onSuccess { melden("Gerät-connect", "Ergebnis $it") }
            .onFailure { melden("Gerät-connect", ursache(it)) }
        // 3. SDP-Abfrage: öffentlich, baut die Funkstrecke auf; der Anker verbindet sich dann meist selbst.
        runCatching { d.fetchUuidsWithSdp() }
            .onSuccess { melden("SDP", "Ergebnis $it") }
            .onFailure { melden("SDP", ursache(it)) }
    }

    private fun ursache(t: Throwable): String = (if (t is java.lang.reflect.InvocationTargetException) t.targetException ?: t else t)
        .let { "${it.javaClass.simpleName}: ${it.message?.take(120)}" }

    private fun melden(sprosse: String, ergebnis: String) {
        if (gemeldet.add("$sprosse|${ergebnis.take(40)}")) Logbuch.system("Anker-Versuch $sprosse", ergebnis)
    }

    private object Empfaenger : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val c = context.applicationContext
            val d: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val istAnker = runCatching { d?.name?.contains(KonferenzMikrofon.GERAET, ignoreCase = true) == true }.getOrDefault(false)
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                    BluetoothAdapter.STATE_ON -> { proxysHolen(c); anstossen(c, "Bluetooth an") }
                    BluetoothAdapter.STATE_OFF -> { lauf?.cancel(); a2dp = null; headset = null; zustandAktualisieren(c) }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> if (istAnker) {
                    Logbuch.system("Anker getrennt")
                    anstossen(c, "Anker getrennt")
                }
                else -> if (istAnker) zustandAktualisieren(c)
            }
        }
    }

    // ------------------------------------------------------------------ Zuhause (Umkreis wie beim Licht)

    private fun p(c: Context) = c.getSharedPreferences("anker_verbindung", Context.MODE_PRIVATE)

    /** Ohne eingerichteten Umkreis (keine Standort-Erlaubnis, keine Adresse) gilt Zuhause, sonst der letzte Stand von Android. */
    fun zuHause(c: Context): Boolean = eingerichtet.isEmpty() || !p(c).getBoolean("weg", false)

    private fun absicht(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 4713, Intent(c, AnkerUmkreisEmpfaenger::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    @Volatile private var eingerichtet = ""
    @Volatile private var entferntInDiesemLauf = false
    @Volatile private var zuhauseFehlerBis = 0L

    @SuppressLint("MissingPermission")
    private suspend fun umkreisAbgleichen(c: Context) {
        val gewuenscht = Einstellungen.get(c).ankerAutoVerbinden && de.frank.jarvis.fahrt.Standort.immerErlaubt(c)
        if (!gewuenscht) {
            if (eingerichtet.isNotEmpty() || !entferntInDiesemLauf) {
                runCatching { LocationServices.getGeofencingClient(c).removeGeofences(absicht(c)) }
                eingerichtet = ""; entferntInDiesemLauf = true
            }
            return
        }
        entferntInDiesemLauf = false
        if (System.currentTimeMillis() < zuhauseFehlerBis) return
        val heim = de.frank.jarvis.kaffee.Kaffeemaschine.zuhause(c) ?: run { zuhauseFehlerBis = System.currentTimeMillis() + 3_600_000L; return }
        val kennung = "${heim.first},${heim.second}"
        if (eingerichtet == kennung) return
        eingerichtet = kennung
        val anfrage = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(
                Geofence.Builder().setRequestId(UMKREIS_ID).setCircularRegion(heim.first, heim.second, RADIUS_M)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(60_000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).build(),
            ).build()
        runCatching {
            LocationServices.getGeofencingClient(c).addGeofences(anfrage, absicht(c))
                .addOnSuccessListener { Logbuch.system("Anker Umkreis", "eingerichtet (${RADIUS_M.toInt()} m)") }
                .addOnFailureListener { f -> eingerichtet = ""; Logbuch.system("Anker Umkreis", "nicht eingerichtet: ${f.message}") }
        }.onFailure { eingerichtet = "" }
    }

    internal fun umkreis(c: Context, betreten: Boolean) {
        val warWeg = p(c).getBoolean("weg", false)
        p(c).edit().putBoolean("weg", !betreten).apply()
        if (betreten && warWeg) anstossen(c, "Heimkommen")
        if (!betreten) { lauf?.cancel(); zustandAktualisieren(c) }
    }
}

/** Empfängt Betreten und Verlassen des Anker-Umkreises von Android. */
class AnkerUmkreisEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ereignis = GeofencingEvent.fromIntent(intent) ?: return
        if (ereignis.hasError()) return
        val app = context.applicationContext
        when (ereignis.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> AnkerVerbindung.umkreis(app, betreten = true)
            Geofence.GEOFENCE_TRANSITION_EXIT -> AnkerVerbindung.umkreis(app, betreten = false)
        }
    }
}

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
    /** Nach so langer erfolgloser Suche zu Hause kommt eine Meldung mit „Erneut versuchen“. */
    private const val MELDEN_NACH_MS = 15 * 60_000L
    private const val MELDUNG_NR = 4714
    const val AKTION_ERNEUT = "de.frank.jarvis.ANKER_ERNEUT"

    /** Für die Anzeige in den Einstellungen und für Jarvis. */
    enum class Zustand(val text: String) {
        AUS("Aus"), VERBUNDEN("Verbunden"), SUCHT("Sucht"), WARTET("Wartet"),
        BLUETOOTH_AUS("Bluetooth aus"), KOPFHOERER("Kopfhörer verbunden"), UNTERWEGS("Nicht zu Hause"), NICHT_GEKOPPELT("Nicht gekoppelt"), KEINE_ERLAUBNIS("Bluetooth-Erlaubnis fehlt"),
    }

    private val _zustand = MutableStateFlow(Zustand.AUS)
    val zustand: StateFlow<Zustand> = _zustand.asStateFlow()

    val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var lauf: Job? = null
    @Volatile private var gestartet = false
    @Volatile private var a2dp: BluetoothProfile? = null
    @Volatile private var headset: BluetoothProfile? = null
    /** Seit wann Jarvis zu Hause ohne Verbindung sucht (elapsedRealtime); 0 = verbunden oder nicht zu Hause. */
    @Volatile private var ohneVerbindungSeit = 0L
    @Volatile private var meldungGezeigt = false
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

    /**
     * Ist neben dem Anker ein anderes Bluetooth-Audiogerät verbunden (Sony-Kopfhörer, Auto)? Dann läuft alles darüber: Jarvis
     * verbindet den Anker nicht, trennt ihn und hört weder Weckwort noch Gespräch über ihn. Name des Geräts oder null.
     */
    @SuppressLint("MissingPermission")
    fun fremdesAudioGeraet(c: Context): String? {
        if (!erlaubt(c)) return null
        for (p in listOfNotNull(a2dp, headset)) {
            // Nur echte Audiogeräte (Kopfhörer, Headset, Auto); Uhren mit Freisprech-Profil (etwa die T-Rex) zählen nicht.
            val d = runCatching { p.connectedDevices }.getOrNull()?.firstOrNull { g ->
                runCatching { g.name?.contains(KonferenzMikrofon.GERAET, ignoreCase = true) != true && g.bluetoothClass?.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO }.getOrDefault(false)
            }
            if (d != null) return runCatching { d.name }.getOrNull() ?: "Kopfhörer"
        }
        return null
    }

    /** Anker trennen (Medien- und Telefon-Profil), damit Ton und Mikrofon sicher bei den Kopfhörern bleiben. */
    @SuppressLint("MissingPermission")
    private fun trennen(c: Context, grund: String) {
        val d = geraet(c) ?: return
        if (!verbunden(c)) return
        Logbuch.system("Anker getrennt", grund)
        for ((name, proxy) in listOf("Medien" to a2dp, "Telefon" to headset)) {
            if (proxy == null) continue
            runCatching { proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java).invoke(proxy, d) }
                .onSuccess { melden("$name-disconnect", "Ergebnis $it") }
                .onFailure { melden("$name-disconnect", ursache(it)) }
        }
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

    /** Angefragte, noch nicht verbundene Profile: nie doppelt anfragen (sonst häufen sich Binder-Verbindungen). */
    private val angefragt = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    /** Ein einziger Zuhörer für beide Profile. */
    private var appKontext: Context? = null
    private val hoerer = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            angefragt.remove(profile)
            if (profile == BluetoothProfile.A2DP) a2dp = proxy else if (profile == BluetoothProfile.HEADSET) headset = proxy
            appKontext?.let { zustandAktualisieren(it) }
        }
        override fun onServiceDisconnected(profile: Int) {
            angefragt.remove(profile)
            if (profile == BluetoothProfile.A2DP) a2dp = null else if (profile == BluetoothProfile.HEADSET) headset = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun proxysHolen(c: Context) {
        if (!erlaubt(c)) return
        val a = adapter(c)?.takeIf { it.isEnabled } ?: return
        appKontext = c
        if (a2dp == null && angefragt.add(BluetoothProfile.A2DP) && !runCatching { a.getProfileProxy(c, hoerer, BluetoothProfile.A2DP) }.getOrDefault(false)) angefragt.remove(BluetoothProfile.A2DP)
        if (headset == null && angefragt.add(BluetoothProfile.HEADSET) && !runCatching { a.getProfileProxy(c, hoerer, BluetoothProfile.HEADSET) }.getOrDefault(false)) angefragt.remove(BluetoothProfile.HEADSET)
    }

    /** Bluetooth aus: Proxys sauber schließen. */
    private fun proxysSchliessen(c: Context) {
        val a = adapter(c)
        a2dp?.let { p -> runCatching { a?.closeProfileProxy(BluetoothProfile.A2DP, p) } }
        headset?.let { p -> runCatching { a?.closeProfileProxy(BluetoothProfile.HEADSET, p) } }
        a2dp = null; headset = null; angefragt.clear()
    }

    /** Warum gerade nicht gesucht wird; null = suchen. */
    private fun hindernis(c: Context): Zustand? = when {
        !Einstellungen.get(c).ankerAutoVerbinden -> Zustand.AUS
        !erlaubt(c) -> Zustand.KEINE_ERLAUBNIS
        adapter(c)?.isEnabled != true -> Zustand.BLUETOOTH_AUS
        geraet(c) == null -> Zustand.NICHT_GEKOPPELT
        fremdesAudioGeraet(c) != null -> Zustand.KOPFHOERER
        !zuHause(c) -> Zustand.UNTERWEGS
        else -> null
    }

    /** Liest Bluetooth (Binder-Aufrufe) im Hintergrund, nie auf dem Hauptfaden. */
    fun zustandAktualisieren(c: Context) { bereich.launch { runCatching { zustandJetzt(c) } } }

    private fun zustandJetzt(c: Context) {
        val h = hindernis(c)
        meldungPruefen(c, h)
        _zustand.value = when {
            h == Zustand.AUS || h == Zustand.KEINE_ERLAUBNIS || h == Zustand.BLUETOOTH_AUS || h == Zustand.NICHT_GEKOPPELT || h == Zustand.KOPFHOERER -> h
            verbunden(c) -> Zustand.VERBUNDEN
            h != null -> h
            lauf?.isActive == true -> Zustand.SUCHT
            else -> Zustand.WARTET
        }
    }

    /**
     * Zu Hause (Umkreis eingerichtet und drinnen) seit 15 Minuten ohne Verbindung: Meldung mit „Erneut versuchen“, einmal je Suche.
     * Verbunden, Schalter aus, Bluetooth aus oder weg: Meldung weg und Uhr zurück.
     */
    private fun meldungPruefen(c: Context, h: Zustand?) {
        val jetzt = android.os.SystemClock.elapsedRealtime()
        val sicherZuHause = eingerichtet.isNotEmpty() && !p(c).getBoolean("weg", false)
        if (h != null || !sicherZuHause || verbunden(c)) {
            ohneVerbindungSeit = 0L
            if (meldungGezeigt) { meldungGezeigt = false; runCatching { c.getSystemService(android.app.NotificationManager::class.java).cancel(MELDUNG_NR) } }
            return
        }
        if (ohneVerbindungSeit == 0L) { ohneVerbindungSeit = jetzt; return }
        if (meldungGezeigt || jetzt - ohneVerbindungSeit < MELDEN_NACH_MS) return
        meldungGezeigt = true
        val minuten = (jetzt - ohneVerbindungSeit) / 60_000L
        Logbuch.system("Anker: Meldung", "seit $minuten Minuten zu Hause ohne Verbindung")
        val erneut = PendingIntent.getBroadcast(c, MELDUNG_NR, Intent(c, AnkerAktionEmpfaenger::class.java).setAction(AKTION_ERNEUT),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "Jarvis versucht seit $minuten Minuten, den Anker PowerConf zu verbinden. Ist er eingeschaltet? Tippe auf „Erneut versuchen“, dann sucht Jarvis wieder im Sekundentakt."
        val b = androidx.core.app.NotificationCompat.Builder(c, de.frank.jarvis.meldung.Meldungen.normalerKanal(c, de.frank.jarvis.meldung.Meldungsarten.ANKER))
            .setSmallIcon(de.frank.jarvis.R.drawable.ic_stat_jarvis)
            .setContentTitle("Anker lässt sich nicht verbinden").setContentText(text)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(c, MELDUNG_NR, Intent(c, de.frank.jarvis.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(de.frank.jarvis.R.drawable.ic_stat_jarvis, "Erneut versuchen", erneut)
        de.frank.jarvis.meldung.Meldungen.zeige(c, de.frank.jarvis.meldung.Meldungsarten.ANKER, MELDUNG_NR, b, "Anker lässt sich nicht verbinden", null)
    }

    /** Aus der Meldung: Meldung weg, Uhr zurück, schnell neu suchen. */
    internal fun erneutVersuchen(c: Context) {
        runCatching { c.getSystemService(android.app.NotificationManager::class.java).cancel(MELDUNG_NR) }
        meldungGezeigt = false
        ohneVerbindungSeit = 0L
        anstossen(c, "Erneut versuchen (Meldung)")
    }

    /** Radius geändert: Umkreis neu einrichten. */
    fun umkreisNeu(context: Context) {
        val c = context.applicationContext
        eingerichtet = ""
        bereich.launch { runCatching { umkreisAbgleichen(c) } }
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
        var anzahl = 0
        var protokolliert = false
        while (true) {
            proxysHolen(c)
            val h = hindernis(c)
            if (h != null || verbunden(c)) { zustandJetzt(c); return }
            _zustand.value = Zustand.SUCHT
            meldungPruefen(c, null)
            if (!protokolliert) { Logbuch.system("Anker: suche Verbindung", grund); protokolliert = true }
            geraet(c)?.let { verbindenVersuchen(c, it) }
            // Kurz warten und prüfen, ob es gegriffen hat (die Profile brauchen ein, zwei Sekunden).
            anzahl++
            delay(abstand(versuch++))
            if (verbunden(c)) {
                Logbuch.system("Anker verbunden", "nach $anzahl Versuch(en), Anlass: $grund")
                zustandJetzt(c)
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
                    BluetoothAdapter.STATE_OFF -> { lauf?.cancel(); proxysSchliessen(c); zustandAktualisieren(c) }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> if (istAnker && Einstellungen.get(c).ankerAutoVerbinden) {
                    Logbuch.system("Anker getrennt")
                    anstossen(c, "Anker getrennt")
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED, BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> if (!istAnker && d != null &&
                    runCatching { d.bluetoothClass?.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO }.getOrDefault(false)) {
                    // Kopfhörer (oder Auto) kommt oder geht: Mit ihnen läuft alles über sie, ohne sie holt Jarvis den Anker zurück.
                    when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                        BluetoothProfile.STATE_CONNECTED -> bereich.launch {
                            lauf?.cancel()
                            if (Einstellungen.get(c).ankerAutoVerbinden) trennen(c, "${fremdesAudioGeraet(c) ?: "Kopfhörer"} verbunden")
                            zustandAktualisieren(c)
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> bereich.launch {
                            delay(2_000)
                            if (fremdesAudioGeraet(c) == null) anstossen(c, "Kopfhörer getrennt") else zustandAktualisieren(c)
                        }
                    }
                } else if (istAnker) {
                    zustandAktualisieren(c)
                    // Der Anker verbindet sich selbst, während Kopfhörer verbunden sind: wieder trennen, der Ton bleibt bei ihnen.
                    if (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_CONNECTED && Einstellungen.get(c).ankerAutoVerbinden)
                        bereich.launch { fremdesAudioGeraet(c)?.let { trennen(c, "$it ist verbunden") } }
                    // Ein Profil fällt weg, ohne dass die Funkstrecke reißt: Steht danach keins mehr, sofort neu verbinden.
                    if (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1) == BluetoothProfile.STATE_DISCONNECTED) bereich.launch {
                        delay(2_000)
                        if (!verbunden(c) && lauf?.isActive != true) { Logbuch.system("Anker getrennt", "Profil weg"); anstossen(c, "Profil getrennt") }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Zuhause (Umkreis wie beim Licht)

    private fun p(c: Context) = c.getSharedPreferences("anker_verbindung", Context.MODE_PRIVATE)

    /**
     * Ohne Standort „Immer“ gilt Zuhause; sonst der zuletzt von Android gemeldete Stand (gespeichert, gilt also auch direkt nach
     * einem Neustart, bevor der Umkreis wieder eingerichtet ist).
     */
    fun zuHause(c: Context): Boolean = !de.frank.jarvis.fahrt.Standort.immerErlaubt(c) || !p(c).getBoolean("weg", false)

    private fun absicht(c: Context): PendingIntent = PendingIntent.getBroadcast(
        c, 4713, Intent(c, AnkerUmkreisEmpfaenger::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    @Volatile private var eingerichtet = ""
    @Volatile private var entferntInDiesemLauf = false
    @Volatile private var zuhauseFehlerBis = 0L

    @SuppressLint("MissingPermission")
    private suspend fun umkreisAbgleichen(c: Context) {
        val e = Einstellungen.get(c)
        val gewuenscht = e.ankerAutoVerbinden && de.frank.jarvis.fahrt.Standort.immerErlaubt(c)
        if (!gewuenscht) {
            if (eingerichtet.isNotEmpty() || !entferntInDiesemLauf) {
                runCatching { LocationServices.getGeofencingClient(c).removeGeofences(absicht(c)) }
                // Ohne Umkreis kein altes „weg“ stehen lassen, sonst sperrt es nach dem Wiedereinschalten.
                p(c).edit().putBoolean("weg", false).apply()
                eingerichtet = ""; entferntInDiesemLauf = true
            }
            return
        }
        entferntInDiesemLauf = false
        if (System.currentTimeMillis() < zuhauseFehlerBis) return
        val heim = de.frank.jarvis.kaffee.Kaffeemaschine.zuhause(c) ?: run { zuhauseFehlerBis = System.currentTimeMillis() + 3_600_000L; return }
        val radius = e.ankerRadius.toFloat()
        val kennung = "${heim.first},${heim.second},$radius"
        if (eingerichtet == kennung) return
        eingerichtet = kennung
        val anfrage = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(
                Geofence.Builder().setRequestId(UMKREIS_ID).setCircularRegion(heim.first, heim.second, radius)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(60_000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT).build(),
            ).build()
        runCatching {
            LocationServices.getGeofencingClient(c).addGeofences(anfrage, absicht(c))
                .addOnSuccessListener { Logbuch.system("Anker Umkreis", "eingerichtet (${radius.toInt()} m)") }
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

/** Knopf „Erneut versuchen“ in der Meldung „Anker lässt sich nicht verbinden“. */
class AnkerAktionEmpfaenger : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AnkerVerbindung.AKTION_ERNEUT) AnkerVerbindung.erneutVersuchen(context.applicationContext)
    }
}

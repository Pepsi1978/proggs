package de.frank.jarvis.live

/**
 * Echosperre im Medienweg: entscheidet je Mikrofonblock, ob der Dienst Stille statt des Mikrofons bekommt, weil Jarvis' Ton
 * noch zu hören ist. Reine Logik ohne Android, damit sie ohne Gerät prüfbar ist; [LiveGespraech] liefert die Messwerte.
 *
 * Befund 20.10.2026 (Gemini 3.8 Live im Auto, Bluetooth-Medienton, Echosperre an): Die Sperre richtete sich nur nach der
 * Kopfposition des AudioTrack plus 400 ms. Der Kopf zählt aber, was der Mischer abgeholt hat, nicht was aus dem
 * Autolautsprecher kommt; über Bluetooth liegt dazwischen die Funkstrecke samt Puffer des Autoradios. Die Geräte-AEC hat für
 * Ton, der das Handy über Bluetooth verlässt, keine Referenz. So kamen die letzten 0,6–0,9 s jeder Antwort ins offene
 * Mikrofon und als Franks Worte zurück („bin für dich da“, „noch etwas?“, „direkt nach“, „Garnelen hinzu“, „weiterleite“).
 *
 * Deshalb hängt bei Bluetooth hinter dem Kopf noch die Ausgabelatenz: gemessen über `AudioTrack.getTimestamp()`, mindestens
 * aber [BLUETOOTH_MIN_LATENZ_MS], plus ein Mikrofonblock, weil ein gelesener Block Ton der letzten [BLOCK_MS] enthält. Am
 * Handy-Lautsprecher und an Kabelkopfhörern bleibt alles wie gemessen (§14.4 der Live-Best-Practices).
 */
class EchoSperre {
    /** Gemessene Ausgabelatenz hinter dem Kopf in ms, geglättet; -1 = noch keine brauchbare Messung. */
    @Volatile var latenzMs = -1L
        private set
    /** Mikrofon frühestens ab hier wieder offen. */
    @Volatile var freiAb = 0L
        private set

    /**
     * Eine Messung zum selben Zeitpunkt: [kopf] = Kopfposition, [ausgegeben] = laut Zeitstempel gerade ausgegebener Frame
     * (beide in Frames der Wiedergabe, dieselbe Zählung). Unplausible Werte zählen nicht.
     */
    fun latenzMessen(kopf: Long, ausgegeben: Long, rate: Int) {
        if (rate <= 0) return
        val ms = (kopf - ausgegeben) * 1000 / rate
        if (ms < 0 || ms > LATENZ_MAX_MS) return
        latenzMs = if (latenzMs < 0) ms else (latenzMs * 3 + ms) / 4
    }

    /** Ausgabelatenz, die die Sperre bei Bluetooth hinter dem Kopf abwartet. */
    fun bluetoothLatenzMs(): Long = maxOf(latenzMs, BLUETOOTH_MIN_LATENZ_MS).coerceAtMost(LATENZ_MAX_MS)

    /** So lange bleibt das Mikrofon zu, nachdem kein echter Ton mehr offen ist. */
    fun nachlaufMs(bluetooth: Boolean): Long = if (bluetooth) NACHLAUF_MS + BLOCK_MS + bluetoothLatenzMs() else NACHLAUF_MS

    /**
     * Je Mikrofonblock. [tonOffen] = echter Ton wartet oder der Kopf hat ihn noch nicht abgespielt. Spätestens [SPERRE_MAX_MS]
     * (bei Bluetooth plus Latenz) nach dem letzten hörbaren Ton ist das Mikrofon wieder offen, egal was die Wiedergabe meldet.
     */
    fun zu(jetzt: Long, tonOffen: Boolean, letzteHoerbarMs: Long, bluetooth: Boolean): Boolean {
        if (tonOffen) freiAb = jetzt + nachlaufMs(bluetooth)
        val deckel = SPERRE_MAX_MS + if (bluetooth) bluetoothLatenzMs() else 0L
        return jetzt < freiAb && jetzt - letzteHoerbarMs < deckel
    }

    companion object {
        /** Nachlauf hinter dem Kopf (Puffer, Hall); am Handy-Lautsprecher gemessen: Sperre öffnete 591–1225 ms nach dem letzten Schreiben. */
        const val NACHLAUF_MS = 400L
        /** Ein Mikrofonblock: Sein Inhalt ist bis zu so viel älter als der Zeitpunkt, an dem über ihn entschieden wird. */
        const val BLOCK_MS = 100L
        /**
         * Mindestlatenz hinter dem Kopf bei Bluetooth, auch wenn der Zeitstempel weniger meldet (Google: er kennt keine Verzögerung,
         * die der Implementierung unbekannt ist, etwa im Autoradio). Abgeleitet, nicht gemessen: Mit 400 ms Nachlauf und einem
         * Block Versatz rutschten 0,6–0,9 s Antwortende durch, das hörbare Ende lag also rund 0,9–1,2 s hinter dem Kopf.
         */
        const val BLUETOOTH_MIN_LATENZ_MS = 1_000L
        /** Größere Messwerte gelten als Fehlmessung (etwa direkt nach dem Leeren). */
        const val LATENZ_MAX_MS = 2_000L
        /** Höchstens so lange nach dem letzten hörbaren Ton bleibt die Sperre zu (ohne Bluetooth-Latenz). */
        const val SPERRE_MAX_MS = 3_000L

        /** Antwortwörter, die Frank oft allein sagt: nie Echoverdacht, auch wenn Jarvis sie eben selbst gesagt hat. */
        private val ANTWORTWORTE = setOf("ja", "nein", "ok", "okay", "genau", "stopp", "danke", "gut", "klar", "richtig", "falsch", "weiter")
        /** So viele letzte Wörter der eigenen Ausgabe zählen für den Echoverdacht. */
        private const val ECHO_FENSTER_WORTE = 15

        private fun worte(text: String) = text.lowercase(java.util.Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }

        /**
         * Nur zur Diagnose, filtert nichts: Besteht [mitschrift] aus Worten, die in derselben Reihenfolge am Stück unter den letzten
         * Worten der eigenen Ausgabe ([eigene]) stehen? Mindestens zwei Worte oder ein Wort ab sechs Buchstaben; ein bloßes
         * Antwortwort („ja“, „nein“ …) nie.
         */
        fun echoVerdacht(mitschrift: String, eigene: String): Boolean {
            val m = worte(mitschrift)
            if (m.isEmpty() || (m.size == 1 && (m[0].length < 6 || m[0] in ANTWORTWORTE))) return false
            val e = worte(eigene).takeLast(ECHO_FENSTER_WORTE)
            if (m.size > e.size) return false
            return (0..e.size - m.size).any { i -> e.subList(i, i + m.size) == m }
        }
    }
}

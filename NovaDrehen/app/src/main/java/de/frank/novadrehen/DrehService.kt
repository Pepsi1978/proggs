package de.frank.novadrehen

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

// Sperrt die Drehung, solange Nova vorne ist und das Fold zugeklappt ist, und solange der
// ChatGPT-Sprachmodus (Kugel nach langem Druck auf die Seitentaste) offen ist, sonst ist sie an.
// Setzt bei jedem Anlass den Soll-Zustand neu durch, statt sich einen eigenen Zustand zu merken.
//
// Welche App vorne ist, kommt aus der Fensterliste (fokussiertes App-Fenster), nicht aus dem
// Absender eines Ereignisses: Einblendungen fremder Pakete (Benachrichtigungen, Edge-Panel,
// Systemdienste) senden beim Drehen eigene Ereignisse und haben die Sperre sonst im Wechsel
// gelöst und wieder gesetzt, sodass Nova zugeklappt hoch und runter pendelte.
class DrehService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var vorne: String? = null
    private var chatGptSprache = false
    private var freigabeGewuenschtSeit = 0L
    private var zuletztGeschrieben = -1

    // Die Fensterliste kann dem Ereignis kurz hinterherhinken: kurz danach noch einmal lesen.
    private val fensterNachpruefen = Runnable {
        vorneAktualisieren(null, "Nachprüfung")
        pruefen()
    }
    private val freigabePruefen = Runnable { pruefen() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) = pruefen()
    }

    private val drehObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            // Fremde Schreiber (Routinen, Modi, andere Dreh-Apps) sichtbar machen: Sie würden mit
            // diesem Dienst Pingpong spielen, das kein Code hier lösen kann.
            val wert = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
            if (zuletztGeschrieben != -1 && wert != zuletztGeschrieben) {
                Protokoll.neu("Auto-Drehen von außen auf $wert gestellt")
            }
            pruefen()
        }
    }

    override fun onServiceConnected() {
        Protokoll.neu("Dienst gestartet")
        getSystemService(DisplayManager::class.java).registerDisplayListener(displayListener, handler)
        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, drehObserver
        )
        vorneAktualisieren(null, "Start")
        pruefen()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        var activityVonEvent: String? = null
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Kugel verschwunden, ohne dass eine andere App nach vorne kam: Sperre trotzdem lösen.
                if (chatGptSprache && !chatGptFensterOffen()) {
                    chatGptSprache = false
                    Protokoll.neu("ChatGPT-Kugel zu")
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: return
                if (pkg in IGNORIEREN || pkg == tastaturPaket()) return
                val klasse = event.className?.toString().orEmpty()
                when {
                    pkg == CHATGPT -> {
                        when {
                            istSprachmodus(klasse) -> chatGptSprache = true
                            istActivity(pkg, klasse) -> chatGptSprache = false
                            // Dialoge und Teilfenster innerhalb von ChatGPT ändern den Modus nicht.
                            vorne != CHATGPT -> chatGptSprache = false
                        }
                        activityVonEvent = CHATGPT
                    }
                    // Nur ein echter App-Wechsel (Activity) zählt. Einblendungen, Popups und
                    // Systemfenster fremder Pakete ändern weder „vorne“ noch den Sprachmodus.
                    istActivity(pkg, klasse) -> {
                        chatGptSprache = false
                        activityVonEvent = pkg
                    }
                    else -> Protokoll.neu("Einblendung ignoriert: $pkg / ${klasse.substringAfterLast('.')}")
                }
            }
            else -> return
        }
        vorneAktualisieren(activityVonEvent, "Ereignis")
        pruefen()
        handler.removeCallbacks(fensterNachpruefen)
        handler.postDelayed(fensterNachpruefen, NACHPRUEFEN_MS)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        pruefen()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
        contentResolver.unregisterContentObserver(drehObserver)
        if (Settings.System.canWrite(this)) {
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        }
        super.onDestroy()
    }

    // Ein frischer App-Wechsel aus dem Ereignis gilt sofort (die Fensterliste hinkt evtl. nach),
    // sonst entscheidet die Fensterliste. Lässt sich nichts sicher bestimmen, bleibt „vorne“ wie es
    // ist: Ein leerer Wert würde die Sperre lösen, genau das Hin und Her, das hier verhindert wird.
    private fun vorneAktualisieren(activityVonEvent: String?, anlass: String) {
        val neu = activityVonEvent ?: vordergrundAusFenstern() ?: return
        if (neu != vorne) {
            Protokoll.neu("vorne: $vorne → $neu ($anlass)")
            vorne = neu
        }
    }

    // Paket des fokussierten App-Fensters, sonst des aktiven, sonst des obersten App-Fensters.
    // Einblendungen anderer Apps (TYPE_APPLICATION_OVERLAY), Toasts, Systemleiste und Tastatur haben
    // andere Fenstertypen und fallen damit heraus.
    private fun vordergrundAusFenstern(): String? {
        val liste = try {
            windows
        } catch (e: RuntimeException) {
            Protokoll.neu("Fensterliste nicht lesbar: ${e.javaClass.simpleName}")
            return null
        }
        val apps = liste.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val fenster = apps.firstOrNull { it.isFocused } ?: apps.firstOrNull { it.isActive }
            ?: apps.maxByOrNull { it.layer } ?: return null
        val paket = fenster.root?.packageName?.toString() ?: return null
        if (paket in IGNORIEREN || paket == tastaturPaket()) return null
        return paket
    }

    private fun pruefen() {
        if (!Settings.System.canWrite(this)) return
        val cr = contentResolver
        val zu = zugeklappt()
        val novaSperre = vorne in NOVA && zu
        val sperren = novaSperre || chatGptSprache
        val ist = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1)
        if (sperren) {
            freigabeGewuenschtSeit = 0L
            handler.removeCallbacks(freigabePruefen)
            if (ist != 0) {
                // Nova: immer Hochformat. ChatGPT-Sprachmodus: die gerade gezeigte Lage einfrieren,
                // damit beim Sperren selbst kein Drehen passiert, das die Sitzung abbrechen würde.
                val lage = if (novaSperre) 0 else aktuelleLage()
                Settings.System.putInt(cr, Settings.System.USER_ROTATION, lage)
                zuletztGeschrieben = 0
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0)
                Protokoll.neu("SPERRE an (vorne=$vorne zu=$zu sprache=$chatGptSprache lage=$lage)")
            } else if (novaSperre && Settings.System.getInt(cr, Settings.System.USER_ROTATION, 0) != 0) {
                // Grund wechselt bei schon gesperrter Drehung zu Nova (z. B. Sprachmodus aufgeklappt quer
                // eingefroren, dann zugeklappt): Nova muss trotzdem ins Hochformat.
                Settings.System.putInt(cr, Settings.System.USER_ROTATION, 0)
                Protokoll.neu("Nova-Hochformat nachgezogen")
            }
        } else if (ist != 1) {
            // Lösen erst, wenn der Wunsch eine Weile stabil bleibt. Ein kurzes Flackern (Ereignis eines
            // fremden Fensters, Zwischenstand beim Drehen oder Klappen) löst die Sperre so nicht mehr.
            // Die Frist zählt ab dem ersten Wunsch, damit häufige Aufrufe sie nicht endlos verschieben.
            val jetzt = SystemClock.uptimeMillis()
            if (freigabeGewuenschtSeit == 0L) freigabeGewuenschtSeit = jetzt
            val rest = ENTPRELLEN_MS - (jetzt - freigabeGewuenschtSeit)
            if (rest > 0) {
                handler.removeCallbacks(freigabePruefen)
                handler.postDelayed(freigabePruefen, rest)
            } else {
                freigabeGewuenschtSeit = 0L
                zuletztGeschrieben = 1
                Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1)
                Protokoll.neu("Sperre aus (vorne=$vorne zu=$zu)")
            }
        } else {
            freigabeGewuenschtSeit = 0L
        }
    }

    private fun aktuelleLage(): Int =
        getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0

    // Liest nur den Paketnamen der offenen App-Fenster, keine Inhalte. Ist ein Fenster gerade
    // nicht lesbar, gilt es vorsichtshalber als ChatGPT, damit die Sperre mitten im Gespräch hält.
    private fun chatGptFensterOffen(): Boolean = windows.any {
        if (it.type != AccessibilityWindowInfo.TYPE_APPLICATION) return@any false
        val paket = it.root?.packageName ?: return@any true
        paket == CHATGPT
    }

    private fun istSprachmodus(klasse: String): Boolean =
        klasse.startsWith("com.openai.voice.") || klasse.startsWith("com.openai.feature.assistant.")

    // Braucht Paket-Sichtbarkeit (<queries> im Manifest). Unsichtbare Pakete gelten als „keine
    // Activity“: Ihr Ereignis wird ignoriert und die Fensterliste entscheidet.
    private fun istActivity(pkg: String, klasse: String): Boolean = try {
        packageManager.getActivityInfo(ComponentName(pkg, klasse), 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun zugeklappt(): Boolean {
        val b = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val kurz = minOf(b.width(), b.height()) / resources.displayMetrics.density
        return kurz < 500f
    }

    private fun tastaturPaket(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')

    companion object {
        private val NOVA = setOf("com.teslacoilsw.launcher")
        private const val CHATGPT = "com.openai.chatgpt"
        private val IGNORIEREN = setOf(
            "com.android.systemui", "de.frank.novadrehen", "com.sec.android.app.launcher"
        )
        private const val NACHPRUEFEN_MS = 200L
        // Muss länger sein als NACHPRUEFEN_MS, damit die Nachprüfung ein Flackern abfängt,
        // bevor die Sperre gelöst wird.
        private const val ENTPRELLEN_MS = 500L
    }
}

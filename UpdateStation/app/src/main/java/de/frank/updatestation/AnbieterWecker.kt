package de.frank.updatestation

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Der Dokumentanbieter des Update-Ordners (Google Drive) ist auf dem Handy abgeschaltet und ließ sich nicht wecken. */
class AnbieterAbgeschaltet(app: String) :
    java.io.IOException("$app ist auf dem Handy abgeschaltet. Bitte UpdateStation oder $app einmal öffnen, dann geht die Prüfung wieder.")

/**
 * Samsung schaltet ungenutzte Apps selbst ab ("auto_disabler", Zustand DISABLED_UNTIL_USED), auch Google Drive.
 * Dann verschwindet der Dokumentanbieter des Update-Ordners, jede Abfrage liefert null, obwohl die
 * Ordnerfreigabe noch gilt. Die Shell darf den Zustand nicht zurücksetzen; das Öffnen der App schon.
 * Deshalb startet [WeckActivity] kurz die Start-Activity des Anbieters und holt UpdateStation danach wieder nach vorn.
 * Nur solange UpdateStation selbst im Vordergrund ist: im Hintergrund nie eine fremde App über Franks aktuelle legen.
 */
object AnbieterWecker {
    private const val FLAGS = PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS
    private const val DRIVE = "com.google.android.apps.docs"
    private val sperre = Mutex()

    fun istDrive(authority: String?): Boolean = authority?.startsWith(DRIVE) == true

    private fun erreichbar(context: Context, authority: String): Boolean =
        context.packageManager.resolveContentProvider(authority, 0) != null

    private fun paket(context: Context, authority: String): String? =
        context.packageManager.resolveContentProvider(authority, FLAGS)?.packageName ?: if (istDrive(authority)) DRIVE else null

    private fun startKomponente(context: Context, paket: String): ComponentName? {
        val suche = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(paket)
        val info = context.packageManager.queryIntentActivities(suche, FLAGS).firstOrNull()?.activityInfo ?: return null
        return ComponentName(info.packageName, info.name)
    }

    private fun appName(context: Context, paket: String?, authority: String): String = paket?.let {
        runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, FLAGS)).toString() }.getOrNull()
    } ?: if (istDrive(authority)) "Google Drive" else "Die Ordner-App"

    private fun imVordergrund(): Boolean {
        val info = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(info)
        return info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    /**
     * Stellt sicher, dass der Anbieter erreichbar ist; weckt ihn bei Bedarf (nur im Vordergrund). Wirft
     * [AnbieterAbgeschaltet], wenn das nicht gelingt. Parallele Aufrufe (ein Ordner je Projekt) wecken nur einmal.
     */
    suspend fun sicherstellen(context: Context, authority: String) {
        if (erreichbar(context, authority)) return
        sperre.withLock {
            if (erreichbar(context, authority)) return
            val paket = paket(context, authority)
            val zustand = paket?.let { runCatching { context.packageManager.getApplicationEnabledSetting(it) }.getOrNull() }
            Log.w(TAG, "Dokumentanbieter $authority fehlt (Paket $paket, Zustand $zustand)")
            Diagnose.ereignis(context, Phase.SCAN, "ANBIETER_AUS", "paket" to paket, "zustand" to zustand)
            val komponente = paket?.takeIf { zustand == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED }
                ?.let { startKomponente(context, it) }
            if (komponente != null && imVordergrund()) {
                val gestartet = withContext(Dispatchers.Main) {
                    runCatching {
                        context.startActivity(
                            Intent(context, WeckActivity::class.java)
                                .putExtra(WeckActivity.ZIEL, komponente)
                                .putExtra(WeckActivity.PAKET, paket)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                        )
                    }.onFailure { Log.w(TAG, "Wecken nicht startbar", it) }.isSuccess
                }
                if (gestartet) for (i in 0 until 20) {
                    delay(500)
                    if (erreichbar(context, authority)) {
                        Diagnose.ereignis(context, Phase.SCAN, "ANBIETER_GEWECKT", "paket" to paket)
                        delay(1_500) // Anbieter kurz hochfahren lassen
                        return
                    }
                }
            }
            Diagnose.ereignis(context, Phase.SCAN, "ANBIETER_WECKEN_FEHLT", "paket" to paket)
            throw AnbieterAbgeschaltet(appName(context, paket, authority))
        }
    }
}

/**
 * Unsichtbare Activity im eigenen Task: startet die abgeschaltete App (das schaltet sie wieder ein) und holt
 * danach UpdateStation zurück nach vorn – auch wenn die App ihre Oberfläche in einem eigenen Task öffnet.
 */
class WeckActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var runden = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ziel = if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(ZIEL, ComponentName::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(ZIEL)
        if (ziel == null) { finish(); return }
        // Nach einer Neuerstellung (Falten, Drehen) nicht erneut starten, nur weiter warten.
        if (savedInstanceState == null) {
            runCatching {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ziel)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
            }.onFailure { Log.w(TAG, "Anbieter-App nicht startbar", it); finish(); return }
        }
        handler.postDelayed(::pruefe, 300)
    }

    private fun pruefe() {
        val paket = intent.getStringExtra(PAKET)
        val zustand = paket?.let { runCatching { packageManager.getApplicationEnabledSetting(it) }.getOrNull() }
        val an = zustand == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || zustand == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        if (an || ++runden >= 10) {
            Log.i(TAG, "Anbieter-App $paket ${if (an) "wieder eingeschaltet" else "nicht eingeschaltet"}, UpdateStation wieder nach vorn")
            moveTaskToBack(true)
            runCatching {
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION))
            }
            finish()
        } else handler.postDelayed(::pruefe, 300)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        const val ZIEL = "ziel"
        const val PAKET = "paket"
    }
}

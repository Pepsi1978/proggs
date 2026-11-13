package de.frank.updatestation

import android.app.Activity
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
import kotlinx.coroutines.withContext

/** Der Dokumentanbieter des Update-Ordners (Google Drive) ist auf dem Handy abgeschaltet und ließ sich nicht wecken. */
class AnbieterAbgeschaltet(app: String) :
    java.io.IOException("$app ist auf dem Handy abgeschaltet. Bitte $app einmal öffnen, dann prüft UpdateStation wieder.")

/**
 * Samsung schaltet ungenutzte Apps selbst ab ("auto_disabler", Zustand DISABLED_UNTIL_USED), auch Google Drive.
 * Dann verschwindet der Dokumentanbieter des Update-Ordners, jede Abfrage liefert null, obwohl die
 * Ordnerfreigabe noch gilt. Die Shell darf den Zustand nicht zurücksetzen; das Öffnen der App schon.
 * Deshalb startet [WeckActivity] kurz die Start-Activity des Anbieters und schickt sie sofort wieder weg.
 */
object AnbieterWecker {
    private const val FLAGS = PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS

    fun erreichbar(context: Context, authority: String): Boolean =
        context.packageManager.resolveContentProvider(authority, 0) != null

    private fun paket(context: Context, authority: String): String? =
        context.packageManager.resolveContentProvider(authority, FLAGS)?.packageName
            ?: if (authority.startsWith("com.google.android.apps.docs")) "com.google.android.apps.docs" else null

    private fun startKomponente(context: Context, paket: String): ComponentName? {
        val suche = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(paket)
        val info = context.packageManager.queryIntentActivities(suche, FLAGS).firstOrNull()?.activityInfo ?: return null
        return ComponentName(info.packageName, info.name)
    }

    private fun appName(context: Context, paket: String?): String = paket?.let {
        runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, FLAGS)).toString() }.getOrNull()
    } ?: "Google Drive"

    /**
     * Stellt sicher, dass der Anbieter erreichbar ist; weckt ihn bei Bedarf. Wirft [AnbieterAbgeschaltet],
     * wenn das nicht gelingt (etwa ohne Erlaubnis "Über anderen Apps anzeigen" im Hintergrund).
     */
    suspend fun sicherstellen(context: Context, authority: String) {
        if (erreichbar(context, authority)) return
        val paket = paket(context, authority)
        val zustand = paket?.let { runCatching { context.packageManager.getApplicationEnabledSetting(it) }.getOrNull() }
        Log.w(TAG, "Dokumentanbieter $authority fehlt (Paket $paket, Zustand $zustand) – wecke ihn")
        Diagnose.ereignis(context, Phase.SCAN, "ANBIETER_AUS", "paket" to paket, "zustand" to zustand)
        val komponente = paket?.let { startKomponente(context, it) }
        if (komponente != null) {
            withContext(Dispatchers.Main) {
                runCatching {
                    context.startActivity(
                        Intent(context, WeckActivity::class.java)
                            .putExtra(WeckActivity.ZIEL, komponente)
                            .putExtra(WeckActivity.PAKET, paket)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                    )
                }.onFailure { Log.w(TAG, "Wecken nicht startbar", it) }
            }
            for (i in 0 until 20) {
                delay(500)
                if (erreichbar(context, authority)) {
                    Diagnose.ereignis(context, Phase.SCAN, "ANBIETER_GEWECKT", "paket" to paket)
                    delay(1_500) // Anbieter kurz hochfahren lassen
                    return
                }
            }
        }
        Diagnose.ereignis(context, Phase.SCAN, "ANBIETER_WECKEN_FEHLT", "paket" to paket)
        throw AnbieterAbgeschaltet(appName(context, paket))
    }
}

/**
 * Unsichtbare Activity im eigenen Task: startet die abgeschaltete App (das schaltet sie wieder ein)
 * im selben Task und schiebt den ganzen Task sofort nach hinten, sodass die vorherige Ansicht bleibt.
 */
class WeckActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var runden = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ziel = if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(ZIEL, ComponentName::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(ZIEL)
        if (ziel == null) { finish(); return }
        runCatching {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ziel)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }.onFailure { Log.w(TAG, "Anbieter-App nicht startbar", it); finish(); return }
        handler.postDelayed(::pruefe, 300)
    }

    private fun pruefe() {
        val paket = intent.getStringExtra(PAKET)
        val an = paket != null && runCatching { packageManager.getApplicationEnabledSetting(paket) }.getOrNull() !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        if (an || ++runden >= 10) {
            Log.i(TAG, "Anbieter-App $paket ${if (an) "wieder eingeschaltet" else "nicht eingeschaltet"}, Task nach hinten")
            moveTaskToBack(true)
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

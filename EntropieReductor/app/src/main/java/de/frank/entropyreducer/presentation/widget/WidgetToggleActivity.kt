package de.frank.entropyreducer.presentation.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import de.frank.entropyreducer.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Unsichtbare Toggle-Activity (Frank-Wunsch 2026-05-11, vierte Iteration — Bugfix).
 *
 * Glance's ActionCallback mit Hilt-EntryPoint hat in der Praxis nicht
 * zuverlaessig gefeuert — der Klick auf den Häkchen-Toggle kam nicht durch.
 * Diese Activity ist `Theme.NoDisplay` + `noHistory=true` + `excludeFromRecents=true`:
 * der User sieht keinen Activity-Wechsel, sie laeuft im Hintergrund und
 * finished sich sofort.
 *
 * Vorteil: actionStartActivity ist die robusteste Glance-Action — geht IMMER
 * durch. Nachteil: minimaler Activity-Lifecycle-Overhead (vernachlaessigbar).
 *
 * Bugfix 2026-05-11: Zwei kritische Aenderungen gegenueber Iteration 3:
 *  1) ATOMARER TOGGLE statt read-then-write. Vorher las die Activity den
 *     aktuellen Wert UND schrieb dann den invertierten — bei zwei schnellen
 *     Klicks lasen beide den GLEICHEN Wert (DataStore commited asynchron) und
 *     beide setzten denselben Zielwert. Resultat: jeder zweite Klick "tat
 *     nichts". Jetzt: ein atomarer toggleWidgetOnlyToday() im DataStore-edit
 *     der Mutual Exclusion garantiert.
 *  2) applicationContext statt Activity-Context fuer updateAll. Die Activity
 *     ruft finish() sofort nach launch — updateAll lief mit einem sterbenden
 *     Activity-Context, was bei Glance manchmal stille Update-Verluste
 *     verursachte.
 */
@AndroidEntryPoint
class WidgetToggleActivity : ComponentActivity() {

    @Inject
    lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ApplicationContext einmal greifen — bleibt gueltig auch nach finish().
        val appCtx = applicationContext
        // CoroutineScope ueber Application-Lifecycle damit die Activity sofort
        // finishen kann ohne den Toggle abzubrechen.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newValue = settings.toggleWidgetOnlyToday()
                EntropyReducerWidget().updateAll(appCtx)
                android.util.Log.i("WidgetToggle", "Atomic toggle → $newValue, updateAll fired")
            } catch (t: Throwable) {
                android.util.Log.e("WidgetToggle", "Atomic toggle FAILED", t)
            }
        }
        finish()
    }
}

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
 * Unsichtbare Toggle-Activity (Frank-Wunsch 2026-05-11, dritte Iteration).
 *
 * Glance's ActionCallback mit Hilt-EntryPoint hat in der Praxis nicht
 * zuverlaessig gefeuert — der Klick auf den Häkchen-Toggle kam nicht durch.
 * Diese Activity ist `Theme.NoDisplay` + `noHistory=true` + `excludeFromRecents=true`:
 * der User sieht keinen Activity-Wechsel, sie laeuft im Hintergrund und
 * finished sich sofort.
 *
 * Vorteil: actionStartActivity ist die robusteste Glance-Action — geht IMMER
 * durch. Nachteil: minimaler Activity-Lifecycle-Overhead (vernachlaessigbar).
 */
@AndroidEntryPoint
class WidgetToggleActivity : ComponentActivity() {

    @Inject
    lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CoroutineScope ueber Application-Lifecycle damit die Activity sofort
        // finishen kann ohne den Toggle abzubrechen.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val current = settings.readWidgetOnlyTodayOnce()
                settings.setWidgetOnlyToday(!current)
                EntropyReducerWidget().updateAll(this@WidgetToggleActivity)
                android.util.Log.i("WidgetToggle", "Activity toggled $current → ${!current}")
            } catch (t: Throwable) {
                android.util.Log.e("WidgetToggle", "Activity FAILED", t)
            }
        }
        finish()
    }
}

package de.frank.entropyreducer.presentation.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import de.frank.entropyreducer.presentation.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Widget Deep-Link Infrastruktur (Frank-Wunsch 2026-05-11).
 *
 * Klassisches Widget (kein Glance mehr). Konstanten + Bus sind weiterhin
 * geteilte Resource zwischen Widget-Provider/Service und App-Seite
 * (MainActivity, TasksScreen).
 */
object WidgetIntents {

    const val ACTION_FOCUS = "focus"
    const val ACTION_RESCHEDULE = "reschedule"

    /**
     * Tap auf die Prio-Perle im Widget: App oeffnen, zur Aufgabe scrollen und
     * direkt deren Prioritaets-Schieberegler aufklappen (Verknuepfung zur
     * manuellen Prioritaet — analog zu ACTION_RESCHEDULE beim Bucket-Picker).
     */
    const val ACTION_SET_PRIORITY = "set_priority"

    /**
     * Frank-Wunsch 2026-05-31: Tap auf die Prio-/Tag-Perle einer LOOP-Vorlage im Widget.
     * Oeffnet die App, klappt den Loop-Block auf und oeffnet direkt den Schieberegler
     * bzw. die Tag-Auswahl GENAU dieser Vorlage. EXTRA_TASK_ID traegt die Template-ID.
     */
    const val ACTION_SET_LOOP_PRIORITY = "set_loop_priority"
    const val ACTION_SET_LOOP_BUCKET = "set_loop_bucket"
    const val ACTION_SETTINGS = "settings"
    const val ACTION_OPEN = "open"

    /** Haekchen-Tap im Widget: Aufgabe direkt als erledigt markieren (ohne App). */
    const val ACTION_COMPLETE = "complete"

    /** Tap auf einen Bucket-Header: Sektion auf-/zuklappen (Akkordeon, ohne App). */
    const val ACTION_TOGGLE_BUCKET = "toggle_bucket"

    /** Bundle-Schluessel im Intent. */
    const val EXTRA_TASK_ID = "widget_task_id"
    const val EXTRA_ACTION = "widget_action"

    /** Bucket-Name (TimeBucket.name) fuer ACTION_TOGGLE_BUCKET. */
    const val EXTRA_BUCKET = "widget_bucket"

    /**
     * Klassik-Intent-Erzeugung fuer Tap auf Widget-Items. Wird vom
     * EntropyReducerWidgetProvider via setOnClickFillInIntent verwendet.
     */
    fun buildExplicitIntent(context: Context, taskId: String, action: String): Intent =
        Intent().apply {
            component = ComponentName(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_ACTION, action)
        }
}

/**
 * Datentraeger fuer einen Widget-Tap, der in der App verarbeitet werden soll.
 * Wird vom WidgetDeepLinkBus als StateFlow durchgeschoben.
 */
data class WidgetDeepLink(
    val taskId: String,
    val action: String,
)

/**
 * Process-scoped Bus fuer Widget-Deep-Links. MainActivity emit'et bei
 * Widget-Intent, TasksScreen collect'et und reagiert.
 */
object WidgetDeepLinkBus {
    private val _events = MutableStateFlow<WidgetDeepLink?>(null)
    val events: kotlinx.coroutines.flow.StateFlow<WidgetDeepLink?> = _events

    fun emit(link: WidgetDeepLink) {
        _events.value = link
    }

    fun clear() {
        _events.value = null
    }
}

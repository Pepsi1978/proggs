package de.frank.wecker

import android.content.SharedPreferences

/**
 * SharedPreferences.commit() übernimmt eine Änderung zuerst in den RAM-Stand und schreibt erst danach auf die Platte.
 * Liefert es false (oder wirft), bleibt der abgewiesene Wert im RAM und würde mit dem nächsten erfolgreichen Commit
 * eines ANDEREN Schlüssels dauerhaft gespeichert. Dieser Helfer setzt dann genau die betroffenen Schlüssel zurück.
 */
internal object PrefsCommit {
    /**
     * Führt [change] aus und committet. Bei false oder Ausnahme werden nur [keys] auf ihren vorherigen Stand zurückgesetzt
     * (fehlende Schlüssel werden wieder entfernt). Eine Ausnahme wird unverändert weitergeworfen; ein Fehler beim
     * Zurücksetzen geht nur an [onRestoreFailure].
     */
    fun commitOrRestore(
        prefs: SharedPreferences,
        keys: Collection<String>,
        onRestoreFailure: (Throwable) -> Unit,
        change: (SharedPreferences.Editor) -> Unit,
    ): Boolean {
        val all = prefs.all
        val before = keys.associateWith { all[it] }
        val committed = try {
            val editor = prefs.edit()
            change(editor)
            editor.commit()
        } catch (e: Throwable) {
            restore(prefs, before, onRestoreFailure)
            throw e
        }
        if (!committed) restore(prefs, before, onRestoreFailure)
        return committed
    }

    private fun restore(prefs: SharedPreferences, before: Map<String, Any?>, onRestoreFailure: (Throwable) -> Unit) {
        try {
            val editor = prefs.edit()
            before.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                when (value) {
                    null -> editor.remove(key)
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value as Set<String>)
                    else -> editor.remove(key)
                }
            }
            // Even if the disk write fails again, commit() has already restored the in-memory state.
            if (!editor.commit()) onRestoreFailure(IllegalStateException("Zurücksetzen nicht auf die Platte geschrieben"))
        } catch (e: Throwable) {
            onRestoreFailure(e)
        }
    }
}

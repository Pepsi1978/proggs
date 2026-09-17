package de.frank.wecker

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsCommitTest {
    /** Mimics SharedPreferencesImpl: commit() applies to RAM first, then writes the whole map to "disk" (or fails). */
    private class FakePrefs : SharedPreferences {
        val memory = mutableMapOf<String, Any?>()
        var disk = mapOf<String, Any?>()
        var failNextCommits = 0
        var throwOnNextCommit: RuntimeException? = null

        override fun getAll(): Map<String, *> = HashMap(memory)
        override fun getString(key: String, defValue: String?) = memory[key] as String? ?: defValue
        @Suppress("UNCHECKED_CAST") override fun getStringSet(key: String, defValues: MutableSet<String>?) = memory[key] as MutableSet<String>? ?: defValues
        override fun getInt(key: String, defValue: Int) = memory[key] as Int? ?: defValue
        override fun getLong(key: String, defValue: Long) = memory[key] as Long? ?: defValue
        override fun getFloat(key: String, defValue: Float) = memory[key] as Float? ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = memory[key] as Boolean? ?: defValue
        override fun contains(key: String) = memory.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            val ops = mutableListOf<(MutableMap<String, Any?>) -> Unit>()
            override fun putString(key: String, value: String?) = apply { ops += { it[key] = value } }
            override fun putStringSet(key: String, values: MutableSet<String>?) = apply { ops += { it[key] = values } }
            override fun putInt(key: String, value: Int) = apply { ops += { it[key] = value } }
            override fun putLong(key: String, value: Long) = apply { ops += { it[key] = value } }
            override fun putFloat(key: String, value: Float) = apply { ops += { it[key] = value } }
            override fun putBoolean(key: String, value: Boolean) = apply { ops += { it[key] = value } }
            override fun remove(key: String) = apply { ops += { it.remove(key) } }
            override fun clear() = apply { ops += { it.clear() } }
            override fun commit(): Boolean {
                throwOnNextCommit?.let { throwOnNextCommit = null; ops.forEach { op -> op(memory) }; throw it }
                ops.forEach { it(memory) }
                if (failNextCommits > 0) { failNextCommits--; return false }
                disk = HashMap(memory)
                return true
            }
            override fun apply() { commit() }
        }
    }

    private val restoreFailures = mutableListOf<Throwable>()

    @Test fun rejectedWriteWithFailedRollbackIsNotPersistedByALaterForeignCommit() {
        val prefs = FakePrefs().apply { memory["alarms"] = "old"; disk = mapOf("alarms" to "old") }
        prefs.failNextCommits = 2 // the change AND the rollback fail on disk
        assertFalse(PrefsCommit.commitOrRestore(prefs, listOf("alarms"), { restoreFailures += it }) { it.putString("alarms", "rejected") })
        assertEquals("old", prefs.getString("alarms", null))
        assertEquals(1, restoreFailures.size)
        // A later successful commit of another key writes the whole map: it must contain the old alarm.
        assertTrue(prefs.edit().putString("ringing", "x").commit())
        assertEquals("old", prefs.disk["alarms"])
    }

    @Test fun successIsKept() {
        val prefs = FakePrefs().apply { memory["alarms"] = "old" }
        assertTrue(PrefsCommit.commitOrRestore(prefs, listOf("alarms"), { restoreFailures += it }) { it.putString("alarms", "new") })
        assertEquals("new", prefs.disk["alarms"])
        assertTrue(restoreFailures.isEmpty())
    }

    @Test fun previouslyMissingKeyIsRemovedAgainAndForeignKeysStayUntouched() {
        val prefs = FakePrefs().apply { memory["draft"] = "keep" }
        prefs.failNextCommits = 1
        assertFalse(PrefsCommit.commitOrRestore(prefs, listOf("alarmIssues")) { it.putString("alarmIssues", "{}") }.let { it })
        assertFalse(prefs.contains("alarmIssues"))
        assertEquals("keep", prefs.getString("draft", null))
        assertTrue(prefs.edit().putString("x", "y").commit())
        assertFalse(prefs.disk.containsKey("alarmIssues"))
    }

    @Test fun thrownCommitIsRolledBackAndTheOriginalErrorIsRethrown() {
        val prefs = FakePrefs().apply { memory["ringing"] = "a"; memory["ringingMeta"] = "{}" }
        val original = IllegalStateException("Plattenfehler")
        prefs.throwOnNextCommit = original
        val thrown = assertThrows(IllegalStateException::class.java) {
            PrefsCommit.commitOrRestore(prefs, listOf("ringing", "ringingMeta"), { restoreFailures += it }) {
                it.putString("ringing", "a,b").putString("ringingMeta", "{\"b\":1}")
            }
        }
        assertSame(original, thrown)
        assertEquals("a", prefs.getString("ringing", null))
        assertEquals("{}", prefs.getString("ringingMeta", null))
    }

    private fun PrefsCommit.commitOrRestore(prefs: SharedPreferences, keys: Collection<String>, change: (SharedPreferences.Editor) -> Unit) =
        commitOrRestore(prefs, keys, { restoreFailures += it }, change)
}

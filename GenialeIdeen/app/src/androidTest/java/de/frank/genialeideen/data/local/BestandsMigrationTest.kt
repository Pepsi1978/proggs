package de.frank.genialeideen.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Öffnet die echte Bestandsdatenbank nur lesend und prüft das vollständig migrierte Schema. */
@RunWith(AndroidJUnit4::class)
class BestandsMigrationTest {
    @Test
    fun datenbankIstAufVersionDreiMitKategoriearten() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val datenbank = GenialeIdeenDatabase.getInstance(context)
        val sqlite = datenbank.openHelper.writableDatabase

        assertEquals(3, sqlite.version)

        val spalten = mutableMapOf<String, String>()
        sqlite.query("PRAGMA table_info(kategorien)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")
            while (cursor.moveToNext()) {
                spalten[cursor.getString(nameIndex)] = cursor.getString(defaultIndex).orEmpty()
            }
        }
        assertEquals("'MENTAL'", spalten["art"])

        val indizes = mutableSetOf<String>()
        sqlite.query("PRAGMA index_list(kategorien)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) indizes += cursor.getString(nameIndex)
        }
        assertTrue("index_kategorien_name_art" in indizes)

        sqlite.query("SELECT DISTINCT art FROM kategorien").use { cursor ->
            while (cursor.moveToNext()) {
                assertTrue(cursor.getString(0) in Kategorieart.entries.map(Kategorieart::name))
            }
        }
    }
}

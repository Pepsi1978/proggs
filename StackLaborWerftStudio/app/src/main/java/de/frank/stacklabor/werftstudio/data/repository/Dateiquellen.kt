package de.frank.stacklabor.werftstudio.data.repository

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface StartbestandQuelle {
    suspend fun lesen(): String
}

class AssetStartbestandQuelle(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StartbestandQuelle {
    override suspend fun lesen(): String = withContext(ioDispatcher) {
        context.assets.open(DATEINAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    companion object {
        const val DATEINAME: String = "startbestand.json"
    }
}

interface RueckfallsicherungStore {
    suspend fun schreiben(json: String)
    suspend fun lesen(): String?
}

class DateiRueckfallsicherungStore(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RueckfallsicherungStore {
    private val datei = File(context.applicationContext.filesDir, DATEINAME)

    override suspend fun schreiben(json: String): Unit = withContext(ioDispatcher) {
        val temporaer = File(datei.parentFile, "$DATEINAME.tmp")
        temporaer.writeText(json, Charsets.UTF_8)
        check(temporaer.renameTo(datei) || temporaer.copyTo(datei, overwrite = true).let { temporaer.delete() }) {
            "Rückfallsicherung konnte nicht atomar geschrieben werden."
        }
    }

    override suspend fun lesen(): String? = withContext(ioDispatcher) {
        datei.takeIf(File::isFile)?.readText(Charsets.UTF_8)
    }

    companion object {
        const val DATEINAME: String = "letzte-rueckfallsicherung.json"
    }
}

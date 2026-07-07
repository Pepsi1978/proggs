package de.frank.entropyreducer.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaRecorder-Wrapper für M4A/AAC mono 16 kHz, 64 kbit/s.
 * Liefert die geschriebene Datei zurück. Spec §8.2.
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    /** Startet eine Aufnahme und gibt die Ziel-Datei zurück. */
    fun start(): File {
        if (recorder != null) discard()
        val dir = File(context.cacheDir, "audio").apply { if (!exists()) mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.m4a")

        val rec = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (t: Throwable) {
            rec.release()
            file.delete()
            currentFile = null
            throw t
        }
        recorder = rec
        currentFile = file
        return file
    }

    /** Beendet die Aufnahme und gibt die geschriebene Datei zurück. */
    fun stop(): File? {
        return try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            currentFile
        } catch (t: Throwable) {
            recorder?.release()
            recorder = null
            currentFile?.delete()
            null
        }
    }

    fun getMaxAmplitude(): Int = recorder?.maxAmplitude ?: 0

    fun isRecording(): Boolean = recorder != null

    fun discard() {
        try { recorder?.stop() } catch (_: Throwable) {}
        recorder?.release()
        recorder = null
        currentFile?.delete()
        currentFile = null
    }
}

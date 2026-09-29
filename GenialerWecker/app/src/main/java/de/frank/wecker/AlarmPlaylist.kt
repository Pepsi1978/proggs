package de.frank.wecker

import java.io.File

data class AlarmClip(val step: String, val audio: PreparedAudio, val variation: Int = 1, val pauseAfterMillis: Long = 0)

object AlarmPlaylist {
    /** Pause nach dem eigenen Text. */
    const val TEXT_PAUSE = 2000L
    /** Pause zwischen zwei Ideen. */
    const val IDEA_PAUSE = 1500L
    /** Pause zwischen zwei Aufgaben. */
    const val TASK_PAUSE = 2000L
    /** Pause nach einem vollständig vorgelesenen Ideen- bzw. Aufgaben-Block, bevor er wiederholt wird oder der nächste Schritt folgt. */
    const val BLOCK_PAUSE = 3000L

    /**
     * Ein Durchlauf je Sprachvariante. Ideen und Aufgaben werden darin so oft am Stück vorgelesen, wie
     * [Alarm.ideasRepeats] bzw. [Alarm.tasksRepeats] sagen (0 = einmal), jede Lesung mit der nächsten Variante;
     * erst danach folgt der nächste Schritt, etwa der Song. Weil jeder Schritt seinen eigenen Variantenzähler hat
     * und es genau so viele Durchläufe wie Varianten gibt, geht auch der Rücksprung an den Anfang nahtlos weiter.
     */
    fun build(alarm: Alarm, tones: Map<String, String>, available: (String) -> Boolean = { File(it).length() > 44 }): List<AlarmClip> {
        val emergency = PreparedAudio(tones.getValue("classic"), provider = "local", fallback = true)
        val variants = alarm.voiceVariants.ifEmpty { listOf(VoiceVariant(alarm.prepared.mapValues { (_, paths) ->
            paths.map { PreparedAudio(it, alarm.preparedSpeed) }
        })) }
        val nextVariant = IntArray(Step.entries.size)
        var shown = 1
        val clips = mutableListOf<AlarmClip>()
        fun add(step: Step, audio: PreparedAudio, variation: Int, pause: Long) {
            shown = variation
            clips += AlarmClip(step.title, if (available(audio.path)) audio else emergency, variation, pause)
        }
        repeat(variants.size) {
            alarm.steps.forEach { step ->
                when (step) {
                    Step.TONE -> add(step, PreparedAudio(tones[alarm.cue] ?: tones.getValue("chime")), shown, 0)
                    Step.MUSIC -> add(step, PreparedAudio(alarm.music.ifBlank { tones[alarm.tone] ?: tones.getValue("classic") }),
                        shown, 0)
                    Step.IDEAS, Step.TEXT, Step.TASKS -> {
                        val readings = when (step) {
                            Step.IDEAS -> alarm.ideasRepeats
                            Step.TASKS -> alarm.tasksRepeats
                            else -> 1
                        }.coerceIn(1, Alarm.MAX_REPEATS)
                        repeat(readings) {
                            val index = nextVariant[step.ordinal]++ % variants.size
                            val audio = variants[index].steps[step.name].orEmpty().ifEmpty { listOf(emergency) }
                            audio.forEachIndexed { part, clip ->
                                val last = part == audio.lastIndex
                                add(step, clip, index + 1, when (step) {
                                    Step.TEXT -> if (last) TEXT_PAUSE else 0
                                    // Einmal gelesene Ideen behalten ihre bisherige kurze Pause am Blockende.
                                    Step.IDEAS -> if (last) (if (readings > 1) BLOCK_PAUSE else IDEA_PAUSE) else if (clip.endOfIdea) IDEA_PAUSE else 0
                                    else -> if (last) BLOCK_PAUSE else if (clip.endOfIdea) TASK_PAUSE else 0
                                })
                            }
                        }
                    }
                }
            }
        }
        return clips.ifEmpty { listOf(AlarmClip("Ersatzweckton", emergency)) }
    }
}

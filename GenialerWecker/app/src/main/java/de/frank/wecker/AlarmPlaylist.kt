package de.frank.wecker

import java.io.File

data class AlarmClip(val step: String, val audio: PreparedAudio, val variation: Int = 1, val pauseAfterMillis: Long = 0)

object AlarmPlaylist {
    fun build(alarm: Alarm, tones: Map<String, String>, available: (String) -> Boolean = { File(it).length() > 44 }): List<AlarmClip> {
        val emergency = PreparedAudio(tones.getValue("classic"), provider = "local", fallback = true)
        val variants = alarm.voiceVariants.ifEmpty { listOf(VoiceVariant(alarm.prepared.mapValues { (_, paths) ->
            paths.map { PreparedAudio(it, alarm.preparedSpeed) }
        })) }
        return variants.flatMapIndexed { index, variant ->
            alarm.steps.flatMap { step ->
                val audio = when (step) {
                    Step.TONE -> listOf(PreparedAudio(tones[alarm.cue] ?: tones.getValue("chime")))
                    Step.MUSIC -> listOf(PreparedAudio(alarm.music.ifBlank { tones[alarm.tone] ?: tones.getValue("classic") }))
                    Step.IDEAS, Step.TEXT -> variant.steps[step.name].orEmpty().ifEmpty { listOf(emergency) }
                }
                audio.mapIndexed { part, clip ->
                    AlarmClip(step.title, if (available(clip.path)) clip else emergency, index + 1,
                        pauseAfterMillis = when {
                            step == Step.TEXT && part == audio.lastIndex -> 2000
                            step == Step.IDEAS && (clip.endOfIdea || part == audio.lastIndex) -> 1500
                            else -> 0
                        })
                }
            }
        }.ifEmpty { listOf(AlarmClip("Ersatzweckton", emergency)) }
    }
}

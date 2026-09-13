package de.frank.wecker

import org.json.JSONArray
import org.json.JSONObject

data class PreparedAudio(val path: String, val speed: Float = 1f, val provider: String = "", val fallback: Boolean = false) {
    fun json() = JSONObject().put("path", path).put("speed", speed).put("provider", provider).put("fallback", fallback)
    companion object {
        fun from(j: JSONObject) = PreparedAudio(j.getString("path"), j.optDouble("speed", 1.0).toFloat(), j.optString("provider"), j.optBoolean("fallback"))
    }
}

data class VoiceVariant(val steps: Map<String, List<PreparedAudio>>) {
    fun json() = JSONObject().apply { steps.forEach { (name, clips) -> put(name, JSONArray(clips.map { it.json() })) } }
    companion object {
        fun from(j: JSONObject) = VoiceVariant(j.keys().asSequence().associateWith { key ->
            j.getJSONArray(key).let { a -> (0 until a.length()).map { PreparedAudio.from(a.getJSONObject(it)) } }
        })
    }
}

/** Eine Idee oder ein eigener Erinnerungstext; die Grenze wird beim Erzeugen nie überschritten. */
data class SpeechGroup(val step: String, val paragraphs: List<String>)

object VoiceVariations {
    const val COUNT = 6
    /** Neue Synthese plus behutsame Variation; Stimme, Text und Grundtempo bleiben erhalten. */
    fun rate(base: Float, variant: Int): Float {
        val factors = when {
            base >= 1.9f -> floatArrayOf(1f, .985f, .97f, .955f, .94f, .925f)
            base <= .55f -> floatArrayOf(1f, 1.015f, 1.03f, 1.045f, 1.06f, 1.075f)
            else -> floatArrayOf(1f, .98f, 1.02f, .96f, 1.04f, 1.01f)
        }
        return (base * factors[Math.floorMod(variant, COUNT)]).coerceIn(.5f, 2f)
    }
    suspend fun build(paragraphs: Map<String, List<String>>, render: suspend (String, Int) -> PreparedAudio,
        progress: suspend (Int, Int, Int) -> Unit = { _, _, _ -> }): List<VoiceVariant> {
        return buildGroups(paragraphs.map { SpeechGroup(it.key, it.value) }, render) { _, _, variation, part, total -> progress(variation, part, total) }
    }
    suspend fun buildGroups(groups: List<SpeechGroup>, render: suspend (String, Int) -> PreparedAudio,
        progress: suspend (Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _ -> }): List<VoiceVariant> {
        val variants = List(COUNT) { linkedMapOf<String, MutableList<PreparedAudio>>() }
        groups.forEachIndexed { groupIndex, group ->
            repeat(COUNT) { variation ->
                group.paragraphs.forEachIndexed { part, text ->
                    progress(groupIndex + 1, groups.size, variation + 1, part + 1, group.paragraphs.size)
                    variants[variation].getOrPut(group.step) { mutableListOf() }.add(render(text, variation))
                }
            }
        }
        return variants.map { VoiceVariant(it.mapValues { (_, clips) -> clips.toList() }) }
    }
}
